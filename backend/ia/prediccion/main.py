import os
from datetime import datetime, timedelta
import psycopg
from psycopg.rows import dict_row
import joblib
import numpy as np
import random
import pandas as pd
from dotenv import load_dotenv

# Cargar variables desde backend/.env
load_dotenv(dotenv_path=os.path.join(os.path.dirname(__file__), "../../.env"))

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
    print("Conexión exitosa a PostgreSQL")
except Exception as e:
    raise RuntimeError(f"Error de conexión: {e}")

modelo_cat = joblib.load("modelo_categoria.pkl")
modelo_monto = joblib.load("modelo_monto.pkl")

DIAS_PREDICCION = 90
HOY = datetime.now()

def generar_features(fecha, rolling7, rolling30):
    dia_semana = fecha.weekday()
    dia_mes = fecha.day
    mes = fecha.month
    dia_anio = fecha.timetuple().tm_yday
    hora = random.choice([9, 12, 18])

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

with conn.cursor() as cur:
    cur.execute("SELECT id FROM usuarios WHERE email=%s", ('esteban@gmail.com',))
    row = cur.fetchone()
    if not row:
        raise RuntimeError("Usuario no encontrado")
    usuario_id = row['id']

registros_pred = []
hist_montos = [100.0]*30

rangos = {
    1: (10, 200),     
    2: (5, 100),      
    3: (1000, 4000),  
    4: (100, 500),    
    5: (500, 3000),   
    6: (50, 500),     
    7: (100, 1000),   
    8: (50, 1000),    
    9: (50, 2000),    
    10: (10, 500)     
}

for d in range(DIAS_PREDICCION):
    fecha_pred = HOY + timedelta(days=d)
    rolling7 = np.mean(hist_montos[-7:])
    rolling30 = np.mean(hist_montos[-30:])
    feats = generar_features(fecha_pred, rolling7, rolling30)

    categorias_dia = set()

    X_cat = pd.DataFrame([feats])
    proba = modelo_cat.predict_proba(X_cat)[0]
    clases = modelo_cat.classes_

    dia_sem = fecha_pred.weekday()  # 0=lunes, 6=domingo
    dia_mes = fecha_pred.day

    for pred_num in range(2):

        # ==============================
        # REGLAS FUERTES POR CONTEXTO
        # ==============================

        if dia_mes in [1, 2, 3]:
            # INICIO DE MES (MUY FUERTE)
            opciones = [3, 4, 5]  # vivienda, servicios, educación
            categoria_pred = opciones[pred_num % len(opciones)]

        elif dia_sem >= 5:
            # FIN DE SEMANA (FUERTE)
            opciones = [7, 2]  # entretenimiento, transporte
            categoria_pred = opciones[pred_num % 2]

        else:
            # ENTRE SEMANA → usar modelo PERO con sesgo a alimentación
            if pred_num == 0:
                categoria_pred = clases[np.argmax(proba)]
            else:
                top_idx = np.argsort(proba)[-3:]
                top_cats = clases[top_idx]
                top_probs = proba[top_idx]

                # sesgo a alimentación
                for i, c in enumerate(top_cats):
                    if c == 1:
                        top_probs[i] *= 1.5

                top_probs = top_probs / top_probs.sum()
                categoria_pred = np.random.choice(top_cats, p=top_probs)

        # evitar duplicados en el mismo día
        if categoria_pred in categorias_dia:
            alternativas = [c for c in clases if c not in categorias_dia]
            if alternativas:
                categoria_pred = random.choice(alternativas)

        categorias_dia.add(categoria_pred)

        # confianza
        if categoria_pred in [1,2]:
            confianza = round(random.uniform(0.6,0.8),4)
        elif categoria_pred in [3,4,5,7]:
            confianza = round(random.uniform(0.4,0.6),4)
        else:
            confianza = round(random.uniform(0.5,0.7),4)

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
            'monto_proyectado': round(float(monto_pred),2),
            'score_confianza': confianza,
            'es_modelo_personal': True
        })

        print(f"usuario={usuario_id} fecha={fecha_pred.date()} categoria={categoria_pred} monto={round(monto_pred,2)} confianza={confianza}")

with conn.cursor() as cur:
    query = """INSERT INTO predicciones_gastos 
               (usuario_id, categoria_id, fecha_prediccion, monto_proyectado, score_confianza, es_modelo_personal)
               VALUES (%(usuario_id)s, %(categoria_id)s, %(fecha_prediccion)s, %(monto_proyectado)s, %(score_confianza)s, %(es_modelo_personal)s)"""

    for i in range(0, len(registros_pred), 100):
        cur.executemany(query, registros_pred[i:i+100])

    conn.commit()
    print(f"{len(registros_pred)} predicciones insertadas en la tabla predicciones_gastos")

conn.close()