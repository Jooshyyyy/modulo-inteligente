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
    hora = 12  # hora fija (podría ser la media del usuario)
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

    # Obtener rolling means del historial real
    df_hist = pd.read_sql(
        "SELECT fecha, monto FROM movimientos WHERE tipo='EGRESO' ORDER BY fecha",
        engine
    )
    if df_hist.empty:
        # si no hay datos, valores por defecto
        ultimo_rolling7 = 100.0
        ultimo_rolling30 = 100.0
    else:
        df_hist['fecha'] = pd.to_datetime(df_hist['fecha'])
        df_hist['rolling7'] = df_hist['monto'].rolling(7, min_periods=1).mean()
        df_hist['rolling30'] = df_hist['monto'].rolling(30, min_periods=1).mean()
        ultimo_rolling7 = df_hist['rolling7'].iloc[-1]
        ultimo_rolling30 = df_hist['rolling30'].iloc[-1]

    DIAS_PREDICCION = 90
    HOY = datetime.now()
    predicciones = []

    for d in range(1, DIAS_PREDICCION + 1):
        fecha = HOY + timedelta(days=d)
        feats = generar_features(fecha, ultimo_rolling7, ultimo_rolling30)

        X_cat = pd.DataFrame([feats])
        if hasattr(modelo_cat, 'feature_names_in_'):
            X_cat = X_cat[modelo_cat.feature_names_in_]
        categoria_pred = modelo_cat.predict(X_cat)[0]
        proba = modelo_cat.predict_proba(X_cat)[0]
        confianza = round(float(np.max(proba)), 4)

        # Predecir monto
        feats_reg = feats.copy()
        feats_reg['categoria_id'] = categoria_pred
        X_monto = pd.DataFrame([feats_reg])
        if hasattr(modelo_monto, 'feature_names_in_'):
            X_monto = X_monto[modelo_monto.feature_names_in_]
        log_monto = modelo_monto.predict(X_monto)[0]
        monto_pred = np.exp(log_monto) - 1
        monto_pred = round(max(0, monto_pred), 2)

        predicciones.append({
            'usuario_id': usuario_id,
            'categoria_id': int(categoria_pred),
            'fecha_prediccion': fecha.date(),
            'monto_proyectado': monto_pred,
            'score_confianza': confianza,
            'es_modelo_personal': True
        })

        print(f"{fecha.date()} | Cat {categoria_pred} | {monto_pred} BOB | conf {confianza}")

    with conn.cursor() as cur:
        cur.execute("TRUNCATE TABLE predicciones_gastos RESTART IDENTITY CASCADE")
        query = """INSERT INTO predicciones_gastos 
                   (usuario_id, categoria_id, fecha_prediccion, monto_proyectado, score_confianza, es_modelo_personal)
                   VALUES (%(usuario_id)s, %(categoria_id)s, %(fecha_prediccion)s, %(monto_proyectado)s, %(score_confianza)s, %(es_modelo_personal)s)"""
        for i in range(0, len(predicciones), 100):
            cur.executemany(query, predicciones[i:i+100])
        conn.commit()
        print(f"{len(predicciones)} predicciones guardadas.")

    conn.close()

if __name__ == "__main__":
    run()