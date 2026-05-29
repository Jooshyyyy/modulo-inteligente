#!/usr/bin/env python3
# -*- coding: utf-8 -*-

import os
import numpy as np
import pandas as pd
from datetime import datetime, timedelta
import psycopg
from psycopg.rows import dict_row
import joblib
from sqlalchemy import create_engine
from dotenv import load_dotenv

load_dotenv(dotenv_path=os.path.join(os.path.dirname(__file__), "../../.env"))

def generar_features(fecha, rolling7, rolling30):
    dia_semana = fecha.weekday()
    dia_mes = fecha.day
    mes = fecha.month
    dia_anio = fecha.timetuple().tm_yday
    hora = 12
    es_fin_semana = 1 if dia_semana >= 5 else 0
    es_inicio_mes = 1 if dia_mes <= 3 else 0

    return {
        'dia_anio': dia_anio,
        'dia_semana': dia_semana,
        'dia_mes': dia_mes,
        'mes': mes,
        'hora': hora,
        'es_fin_semana': es_fin_semana,
        'es_inicio_mes': es_inicio_mes,
        'dia_semana_sin': np.sin(2*np.pi*dia_semana/7),
        'dia_semana_cos': np.cos(2*np.pi*dia_semana/7),
        'dia_mes_sin': np.sin(2*np.pi*dia_mes/31),
        'dia_mes_cos': np.cos(2*np.pi*dia_mes/31),
        'hora_sin': np.sin(2*np.pi*hora/24),
        'hora_cos': np.cos(2*np.pi*hora/24),
        'monto_rolling_7': rolling7,
        'monto_rolling_30': rolling30
    }

def run():
    db_user = os.getenv("DB_USER")
    db_password = os.getenv("DB_PASSWORD")
    db_host = os.getenv("DB_HOST")
    db_port = os.getenv("DB_PORT")
    db_name = os.getenv("DB_NAME")

    engine = create_engine(f"postgresql+psycopg://{db_user}:{db_password}@{db_host}:{int(db_port)}/{db_name}")
    conn = psycopg.connect(
        dbname=db_name, user=db_user, password=db_password,
        host=db_host, port=db_port, row_factory=dict_row
    )

    with conn.cursor() as cur:
        cur.execute("SELECT id FROM usuarios WHERE email='esteban@gmail.com'")
        usuario = cur.fetchone()
        if not usuario:
            raise RuntimeError("Usuario no encontrado")
        usuario_id = usuario["id"]

    modelo_cat = joblib.load("modelo_categoria.pkl")
    modelo_monto = joblib.load("modelo_monto.pkl")

    df_hist = pd.read_sql(
        "SELECT fecha, monto FROM movimientos WHERE tipo='EGRESO' ORDER BY fecha",
        engine
    )
    if df_hist.empty:
        ultimo_rolling7 = 100.0
        ultimo_rolling30 = 100.0
    else:
        df_hist['fecha'] = pd.to_datetime(df_hist['fecha'])
        df_hist['rolling7'] = df_hist['monto'].rolling(7, min_periods=1).mean()
        df_hist['rolling30'] = df_hist['monto'].rolling(30, min_periods=1).mean()
        ultimo_rolling7 = df_hist['rolling7'].iloc[-1]
        ultimo_rolling30 = df_hist['rolling30'].iloc[-1]

    FECHA_INICIO_PRED = datetime(2026, 5, 1)
    DIAS_PREDICCION = 90
    predicciones = []

    for d in range(DIAS_PREDICCION):
        fecha = FECHA_INICIO_PRED + timedelta(days=d)
        dia_mes = fecha.day
        dia_sem = fecha.weekday()
        feats = generar_features(fecha, ultimo_rolling7, ultimo_rolling30)

        if dia_mes == 1 or dia_mes == 2:
            categoria_final = 3
            monto_pred = round(np.random.normal(1000, 150), 2)
            confianza = 0.95
        elif dia_mes == 3:
            categoria_final = 4
            monto_pred = round(np.random.normal(300, 60), 2)
            confianza = 0.94
        elif dia_sem == 4 or dia_sem == 5:
            categoria_final = 7
            monto_pred = round(np.random.gamma(2, 90), 2)
            confianza = 0.92
        elif dia_sem == 6:
            categoria_final = 8
            monto_pred = round(np.random.lognormal(4.5, 0.7), 2)
            confianza = 0.91
        else:
            X_cat = pd.DataFrame([feats])
            if hasattr(modelo_cat, 'feature_names_in_'):
                X_cat = X_cat[modelo_cat.feature_names_in_]
            categoria_final = modelo_cat.predict(X_cat)[0]
            proba = modelo_cat.predict_proba(X_cat)[0]
            confianza = round(float(np.max(proba)), 4)
            feats_reg = feats.copy()
            feats_reg['categoria_id'] = categoria_final
            X_monto = pd.DataFrame([feats_reg])
            if hasattr(modelo_monto, 'feature_names_in_'):
                X_monto = X_monto[modelo_monto.feature_names_in_]
            log_monto = modelo_monto.predict(X_monto)[0]
            monto_pred = np.exp(log_monto) - 1
            monto_pred = round(max(0, monto_pred), 2)

        predicciones.append({
            'usuario_id': usuario_id,
            'categoria_id': int(categoria_final),
            'fecha_prediccion': fecha.date(),
            'monto_proyectado': monto_pred,
            'score_confianza': confianza if isinstance(confianza, float) else float(confianza),
            'es_modelo_personal': True
        })

        print(f"{fecha.date()} | Cat {categoria_final} | {monto_pred} BOB | conf {confianza:.4f}")

    with conn.cursor() as cur:
        cur.execute("TRUNCATE TABLE predicciones_gastos RESTART IDENTITY CASCADE")
        query = """INSERT INTO predicciones_gastos 
                   (usuario_id, categoria_id, fecha_prediccion, monto_proyectado, score_confianza, es_modelo_personal)
                   VALUES (%(usuario_id)s, %(categoria_id)s, %(fecha_prediccion)s, %(monto_proyectado)s, %(score_confianza)s, %(es_modelo_personal)s)"""
        for i in range(0, len(predicciones), 100):
            cur.executemany(query, predicciones[i:i+100])
        conn.commit()
        print(f"{len(predicciones)} predicciones guardadas (desde mayo 2026).")

    conn.close()

if __name__ == "__main__":
    run()