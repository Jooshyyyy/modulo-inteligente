import joblib
import pandas as pd
import random
import time
from datetime import datetime, timedelta
import os
import psycopg
from dotenv import load_dotenv
import numpy as np

env_path = os.path.abspath(os.path.join(os.path.dirname(__file__), '..', '..', '.env'))
load_dotenv(env_path)

# ==============================
# CARGAR MODELOS
# ==============================
def cargar_modelos():
    try:
        modelo_categoria = joblib.load("modelo_categoria.pkl")
        modelo_monto = joblib.load("modelo_monto.pkl")
        return modelo_categoria, modelo_monto
    except Exception as e:
        print(f"Error cargando modelos: {e}")
        exit()

# ==============================
# MAPA DE CATEGORÍAS
# ==============================
categorias = {
    1: 'Alimentacion',
    2: 'Transporte',
    4: 'Servicios',
    5: 'Educacion',
    7: 'Entretenimiento'
}

# ==============================
# PREDICCIÓN COMPLETA
# ==============================
def realizar_inferencia(modelo_categoria, modelo_monto, fecha_test):

    dia_semana = fecha_test.weekday()
    dia_mes = fecha_test.day
    mes = fecha_test.month
    dia_anio = fecha_test.timetuple().tm_yday
    hora = fecha_test.hour
    
    dia_semana_sin = np.sin(2 * np.pi * dia_semana / 7)
    dia_semana_cos = np.cos(2 * np.pi * dia_semana / 7)
    dia_mes_sin = np.sin(2 * np.pi * dia_mes / 31)
    dia_mes_cos = np.cos(2 * np.pi * dia_mes / 31)
    hora_sin = np.sin(2 * np.pi * hora / 24)
    hora_cos = np.cos(2 * np.pi * hora / 24)

    # ==========================
    # INPUT PARA CATEGORÍA
    # ==========================
    features_dict = {
        'dia_anio': dia_anio,
        'dia_semana': dia_semana,
        'dia_mes': dia_mes,
        'mes': mes,
        'hora': hora,
        'dia_semana_sin': dia_semana_sin,
        'dia_semana_cos': dia_semana_cos,
        'dia_mes_sin': dia_mes_sin,
        'dia_mes_cos': dia_mes_cos,
        'hora_sin': hora_sin,
        'hora_cos': hora_cos
    }
    X_cat = pd.DataFrame([features_dict])

    # ==========================
    # PROBABILIDADES DE CATEGORÍA
    # ==========================
    probas = modelo_categoria.predict_proba(X_cat)[0]
    clases = modelo_categoria.classes_

    # Top 3 categorías probables
    top_indices = probas.argsort()[-3:]
    top_categorias = [clases[i] for i in top_indices]

    # ==========================
    # AJUSTE TEMPORAL DEMO
    # ==========================
    if dia_semana in [4, 5] and 7 in top_categorias:
        categoria_pred = 7  # viernes/sábado -> entretenimiento
    else:
        categoria_pred = random.choice(top_categorias)

    # ==========================
    # INPUT PARA MONTO
    # ==========================
    monto_dict = {'categoria_id': categoria_pred}
    monto_dict.update(features_dict)
    X_monto = pd.DataFrame([monto_dict])

    monto_pred = modelo_monto.predict(X_monto)[0]

    # ==========================
    # NOMBRE DEL DÍA
    # ==========================
    dias_nombre = [
        "Lunes", "Martes", "Miercoles",
        "Jueves", "Viernes", "Sabado", "Domingo"
    ]

    nombre_dia = dias_nombre[dia_semana]

    # ==========================
    # JSON OUTPUT
    # ==========================
    json_output = {
        "status": "success",
        "data": {
            "usuario_id": 1,
            "contexto": {
                "fecha_analizada": fecha_test.strftime("%Y-%m-%d"),
                "dia_nombre": nombre_dia,
                "es_fin_de_semana": dia_semana in [5, 6]
            },
            "prediccion": {
                "categoria": categorias.get(categoria_pred, 'Otra'),
                "monto_estimado": round(float(monto_pred), 2),
                "moneda": "BOB",
                "confianza": round(max(probas), 2),
                "insight": f"Patrón temporal detectado para {nombre_dia}."
            }
        }
    }

    # ==========================
    # SQL OUTPUT DATA
    # ==========================
    score_confianza = max(probas)
    sql_data = (
        1,  # usuario_id
        int(categoria_pred),  # categoria_id
        fecha_test.date(),  # fecha_prediccion
        round(monto_pred, 2),  # monto_proyectado
        float(score_confianza),  # score_confianza
        True  # es_modelo_personal
    )

    return json_output, sql_data

# ==============================
# MAIN LOOP
# ==============================
if __name__ == "__main__":

    try:
        conn = psycopg.connect(
            dbname=os.getenv("DB_NAME", "postgres"),
            user=os.getenv("DB_USER", "postgres"),
            password=os.getenv("DB_PASSWORD", ""),
            host=os.getenv("DB_HOST", "localhost"),
            port=os.getenv("DB_PORT", "5432")
        )
        print("Conexión a la BD exitosa para inserción de predicciones.")
    except Exception as e:
        print(f"Error conectando a BD: {e}")
        conn = None

    modelo_categoria, modelo_monto = cargar_modelos()

    fecha_iterativa = datetime(2026, 3, 30)

    print("MONITOR DE INFERENCIA TEMPORAL INTELIGENTE")
    print("Simulación basada en patrones aprendidos")
    print("-" * 60)

    try:
        while True:

            res_json, sql_data = realizar_inferencia(
                modelo_categoria,
                modelo_monto,
                fecha_iterativa
            )

            ctx = res_json["data"]["contexto"]
            pred = res_json["data"]["prediccion"]

            print(f"\n[FECHA: {ctx['fecha_analizada']} | {ctx['dia_nombre']}]")
            print(f"Categoria: {pred['categoria']}")
            print(f"Prediccion IA: {pred['monto_estimado']} BOB")
            print(f"Confianza: {pred['confianza']}")
            
            if conn:
                try:
                    with conn.cursor() as cur:
                        query = """
                            INSERT INTO predicciones_gastos
                            (usuario_id, categoria_id, fecha_prediccion, monto_proyectado, score_confianza, es_modelo_personal)
                            VALUES (%s, %s, %s, %s, %s, %s)
                        """
                        cur.execute(query, sql_data)
                        conn.commit()
                        print("Insertado en tabla predicciones_gastos exitosamente.")
                except Exception as e:
                    print(f"Error insertando: {e}")
                    conn.rollback()

            print("-" * 50)

            fecha_iterativa += timedelta(days=1)

            if (fecha_iterativa - datetime(2026, 3, 30)).days > 30:
                fecha_iterativa = datetime(2026, 3, 30)

            time.sleep(5)

    except KeyboardInterrupt:
        print("\nSimulación detenida.")
        if 'conn' in locals() and conn:
            conn.close()