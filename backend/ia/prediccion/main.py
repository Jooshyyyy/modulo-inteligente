import argparse
import os
from datetime import datetime, timedelta

import joblib
import numpy as np
import pandas as pd
import psycopg
from dotenv import load_dotenv

env_path = os.path.abspath(os.path.join(os.path.dirname(__file__), "..", "..", ".env"))
load_dotenv(env_path)


def cargar_modelos():
    base_dir = os.path.dirname(__file__)
    modelo_categoria = joblib.load(os.path.join(base_dir, "modelo_categoria.pkl"))
    modelo_monto = joblib.load(os.path.join(base_dir, "modelo_monto.pkl"))
    return modelo_categoria, modelo_monto


def construir_features(fecha_test):
    dia_semana = fecha_test.weekday()
    dia_mes = fecha_test.day
    mes = fecha_test.month
    dia_anio = fecha_test.timetuple().tm_yday
    hora = fecha_test.hour

    return {
        "dia_anio": dia_anio,
        "dia_semana": dia_semana,
        "dia_mes": dia_mes,
        "mes": mes,
        "hora": hora,
        "dia_semana_sin": np.sin(2 * np.pi * dia_semana / 7),
        "dia_semana_cos": np.cos(2 * np.pi * dia_semana / 7),
        "dia_mes_sin": np.sin(2 * np.pi * dia_mes / 31),
        "dia_mes_cos": np.cos(2 * np.pi * dia_mes / 31),
        "hora_sin": np.sin(2 * np.pi * hora / 24),
        "hora_cos": np.cos(2 * np.pi * hora / 24),
    }


def inferir_para_dia(modelo_categoria, modelo_monto, fecha_test):
    features = construir_features(fecha_test)

    x_cat = pd.DataFrame([features])
    probas = modelo_categoria.predict_proba(x_cat)[0]
    clases = modelo_categoria.classes_

    # Si el modelo está totalmente seguro, introducir variación
    if np.max(probas) >= 0.999:
        ruido = np.random.uniform(0.05, 0.15, size=len(probas))
        probas = probas + ruido
        probas = probas / probas.sum()

    # Aplicar temperatura para suavizar distribución
    temperature = 1.5
    probas = np.power(probas, 1 / temperature)
    probas = probas / probas.sum()

    # Selección probabilística
    categoria_pred = int(np.random.choice(clases, p=probas))
    score_confianza = float(probas[np.where(clases == categoria_pred)][0])

    # Predicción de monto
    x_monto = pd.DataFrame([{**features, "categoria_id": categoria_pred}])
    monto_pred = max(0.0, float(modelo_monto.predict(x_monto)[0]))

    return categoria_pred, round(monto_pred, 2), score_confianza


def rango_fechas(fecha_inicio, fecha_fin):
    fecha = fecha_inicio
    while fecha <= fecha_fin:
        yield fecha
        fecha += timedelta(days=1)


def fecha_inicio_default():
    now = datetime.now()
    return now.replace(day=20).strftime("%Y-%m-%d")


def fecha_fin_mes(fecha_inicio):
    if fecha_inicio.month == 12:
        siguiente_mes = fecha_inicio.replace(year=fecha_inicio.year + 1, month=1, day=1)
    else:
        siguiente_mes = fecha_inicio.replace(month=fecha_inicio.month + 1, day=1)
    return siguiente_mes - timedelta(days=1)


def upsert_prediccion(cur, usuario_id, categoria_id, fecha_prediccion, monto, confianza):
    delete_query = """
        DELETE FROM predicciones_gastos
        WHERE usuario_id = %s
          AND categoria_id = %s
          AND fecha_prediccion = %s
    """

    insert_query = """
        INSERT INTO predicciones_gastos (
            usuario_id, categoria_id, fecha_prediccion,
            monto_proyectado, score_confianza, es_modelo_personal
        )
        VALUES (%s, %s, %s, %s, %s, %s)
    """

    cur.execute(delete_query, (usuario_id, categoria_id, fecha_prediccion))
    cur.execute(insert_query, (usuario_id, categoria_id, fecha_prediccion, monto, confianza, True))


def main():
    parser = argparse.ArgumentParser(description="Generador de predicciones de gastos")
    parser.add_argument("--usuario-id", type=int, default=10)
    parser.add_argument("--fecha-inicio", type=str, default=fecha_inicio_default())
    parser.add_argument("--fecha-fin", type=str, default=None)
    parser.add_argument("--dias", type=int, default=None)
    args = parser.parse_args()

    fecha_inicio = datetime.strptime(args.fecha_inicio, "%Y-%m-%d")
    if args.dias is not None and args.dias > 0:
        fecha_fin = fecha_inicio + timedelta(days=args.dias - 1)
    elif args.fecha_fin:
        fecha_fin = datetime.strptime(args.fecha_fin, "%Y-%m-%d")
    else:
        # Generar 3 meses desde fecha_inicio
        month = fecha_inicio.month - 1 + 3
        year = fecha_inicio.year + month // 12
        month = month % 12 + 1
        day = min(fecha_inicio.day, 28)  # evitar problemas con febrero

        fecha_fin = fecha_inicio.replace(year=year, month=month, day=day) - timedelta(days=1)

    modelo_categoria, modelo_monto = cargar_modelos()

    conn = psycopg.connect(
        dbname=os.getenv("DB_NAME", "postgres"),
        user=os.getenv("DB_USER", "postgres"),
        password=os.getenv("DB_PASSWORD", ""),
        host=os.getenv("DB_HOST", "localhost"),
        port=os.getenv("DB_PORT", "5432"),
    )

    try:
        with conn.cursor() as cur:
            for fecha in rango_fechas(fecha_inicio, fecha_fin):
                categoria_id, monto, confianza = inferir_para_dia(
                    modelo_categoria, modelo_monto, fecha
                )

                upsert_prediccion(
                    cur,
                    args.usuario_id,
                    categoria_id,
                    fecha.date(),
                    monto,
                    confianza,
                )

                print(
                    f"[OK] usuario={args.usuario_id} fecha={fecha.date()} "
                    f"categoria={categoria_id} monto={monto} "
                    f"confianza={round(confianza, 4)}"
                )

            conn.commit()
    finally:
        conn.close()


if __name__ == "__main__":
    main()