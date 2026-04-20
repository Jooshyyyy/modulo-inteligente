import os
import random
import numpy as np
import joblib
from datetime import datetime, timedelta
import psycopg
from psycopg.rows import dict_row
from dotenv import load_dotenv

from entrenamiento import train

load_dotenv(dotenv_path=os.path.join(os.path.dirname(__file__), "../../.env"))

DIAS_PREDICCION = 30
HOY = datetime.now()

def generar_features(fecha, rolling7, rolling30):
    dia_semana = fecha.weekday()
    dia_mes = fecha.day
    mes = fecha.month
    dia_anio = fecha.timetuple().tm_yday
    hora = 12
    return {
        'dia_anio': dia_anio,
        'dia_semana': dia_semana,
        'dia_mes': dia_mes,
        'mes': mes,
        'hora': hora,
        'dia_semana_sin': np.sin(2*np.pi*dia_semana/7),
        'dia_semana_cos': np.cos(2*np.pi*dia_semana/7),
        'dia_mes_sin': np.sin(2*np.pi*dia_mes/31),
        'dia_mes_cos': np.cos(2*np.pi*dia_mes/31),
        'hora_sin': np.sin(2*np.pi*hora/24),
        'hora_cos': np.cos(2*np.pi*hora/24),
        'monto_rolling_7': rolling7,
        'monto_rolling_30': rolling30
    }

def run_predictions():
    db_user = os.getenv("DB_USER")
    db_password = os.getenv("DB_PASSWORD")
    db_host = os.getenv("DB_HOST")
    db_port = os.getenv("DB_PORT")
    db_name = os.getenv("DB_NAME")

    try:
        conn = psycopg.connect(
            dbname=db_name,
            user=db_user,
            password=db_password,
            host=db_host,
            port=db_port,
            row_factory=dict_row
        )
    except Exception as e:
        raise RuntimeError(f"Connection error: {e}")

    modelo_cat = joblib.load("modelo_categoria.pkl")
    modelo_monto = joblib.load("modelo_monto.pkl")

    with conn.cursor() as cur:
        cur.execute("SELECT id FROM usuarios WHERE email=%s", ('esteban@gmail.com',))
        row = cur.fetchone()
        if not row:
            raise RuntimeError("User not found")
        usuario_id = row['id']

    registros_pred = []
    hist_montos = [100.0] * 30

    rangos = {
        10: (10, 200),
        2: (1000, 4000),
        3: (100, 1000),
        1: (5, 100),
        4: (500, 3000),
    }

    for d in range(DIAS_PREDICCION):
        fecha_pred = HOY + timedelta(days=d)
        rolling7 = np.mean(hist_montos[-7:])
        rolling30 = np.mean(hist_montos[-30:])
        feats = generar_features(fecha_pred, rolling7, rolling30)

        X_cat = np.array([list(feats.values())])
        categoria_pred = modelo_cat.predict(X_cat)[0]

        if categoria_pred in [10, 1]:
            confianza = round(random.uniform(0.6, 0.8), 4)
        elif categoria_pred in [3, 4]:
            confianza = round(random.uniform(0.4, 0.6), 4)
        else:
            confianza = round(random.uniform(0.5, 0.7), 4)

        feats_monto = list(feats.values()) + [categoria_pred]
        X_monto = np.array([feats_monto])
        pred_log = modelo_monto.predict(X_monto)[0]
        monto_pred = np.exp(pred_log) - 1

        min_val, max_val = rangos.get(int(categoria_pred), (0, 5000))
        monto_pred = np.clip(monto_pred, min_val, max_val)

        hist_montos.append(float(monto_pred))

        registros_pred.append({
            'usuario_id': usuario_id,
            'categoria_id': int(categoria_pred),
            'fecha_prediccion': fecha_pred.date(),
            'monto_proyectado': round(float(monto_pred), 2),
            'score_confianza': confianza,
            'es_modelo_personal': True
        })

    with conn.cursor() as cur:
        query = """INSERT INTO predicciones_gastos 
                   (usuario_id, categoria_id, fecha_prediccion, monto_proyectado, score_confianza, es_modelo_personal)
                   VALUES (%(usuario_id)s, %(categoria_id)s, %(fecha_prediccion)s, %(monto_proyectado)s, %(score_confianza)s, %(es_modelo_personal)s)"""
        for i in range(0, len(registros_pred), 100):
            cur.executemany(query, registros_pred[i:i+100])
        conn.commit()

    conn.close()

if __name__ == "__main__":
    train()
    run_predictions()
