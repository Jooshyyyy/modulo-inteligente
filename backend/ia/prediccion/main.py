import os
from datetime import datetime, timedelta
import psycopg
from psycopg.rows import dict_row
import joblib
import numpy as np
import random
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


def _probas_categoria(modelo_categoria, features):
    x_cat = pd.DataFrame([features])
    probas = modelo_categoria.predict_proba(x_cat)[0].astype(float)
    clases = modelo_categoria.classes_

    if np.max(probas) >= 0.999:
        ruido = np.random.uniform(0.05, 0.15, size=len(probas))
        probas = probas + ruido
        probas = probas / probas.sum()

    temperature = 1.5
    probas = np.power(probas, 1 / temperature)
    probas = probas / probas.sum()
    return probas, clases


def inferir_para_dia(modelo_categoria, modelo_monto, fecha_test):
    """Una fila por día: categoría muestreada (compatibilidad / pruebas)."""
    features = construir_features(fecha_test)
    probas, clases = _probas_categoria(modelo_categoria, features)
    categoria_pred = int(np.random.choice(clases, p=probas))
    idx = int(np.where(clases == categoria_pred)[0][0])
    score_confianza = float(probas[idx])
    x_monto = pd.DataFrame([{**features, "categoria_id": categoria_pred}])
    monto_pred = max(0.0, float(modelo_monto.predict(x_monto)[0]))
    return categoria_pred, round(monto_pred, 2), score_confianza


def inferir_distribucion_dia(modelo_categoria, modelo_monto, fecha_test, top_k=6):
    """
    Varias filas por día: siempre las top_k probabilidades (>0), re-normalizadas,
    para que exista reparto visible aunque el umbral 5%% dejara una sola clase.
    """
    features = construir_features(fecha_test)
    probas, clases = _probas_categoria(modelo_categoria, features)

    winner_idx = int(np.argmax(probas))
    winner_cat = int(clases[winner_idx])
    x_monto_w = pd.DataFrame([{**features, "categoria_id": winner_cat}])
    monto_total = max(0.0, float(modelo_monto.predict(x_monto_w)[0]))

    order = np.argsort(-probas)
    idxs = []
    for i in order[:top_k]:
        ii = int(i)
        if float(probas[ii]) < 1e-12:
            continue
        idxs.append(ii)

    if not idxs:
        idxs = [winner_idx]

    sub_p = probas[idxs].astype(float)
    sub_p = sub_p / sub_p.sum()

    filas = []
    for j, fi in enumerate(idxs):
        cat_id = int(clases[fi])
        m_part = round(float(sub_p[j]) * monto_total, 2)
        conf = float(probas[fi])
        if m_part > 0:
            filas.append((cat_id, m_part, conf))

    if not filas:
        filas.append((winner_cat, round(monto_total, 2), float(probas[winner_idx])))

    return filas


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


def borrar_predicciones_del_dia(cur, usuario_id, fecha_prediccion):
    cur.execute(
        """
        DELETE FROM predicciones_gastos
        WHERE usuario_id = %s
          AND fecha_prediccion::date = %s::date
        """,
        (usuario_id, fecha_prediccion),
    )


def insertar_prediccion(cur, usuario_id, categoria_id, fecha_prediccion, monto, confianza):
    cur.execute(
        """
        INSERT INTO predicciones_gastos (
            usuario_id, categoria_id, fecha_prediccion,
            monto_proyectado, score_confianza, es_modelo_personal
        )
        VALUES (%s, %s, %s, %s, %s, %s)
        """,
        (usuario_id, categoria_id, fecha_prediccion, monto, confianza, True),
    )


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
                filas = inferir_distribucion_dia(modelo_categoria, modelo_monto, fecha)
                borrar_predicciones_del_dia(cur, args.usuario_id, fecha.date())
                for categoria_id, monto, confianza in filas:
                    insertar_prediccion(
                        cur,
                        args.usuario_id,
                        categoria_id,
                        fecha.date(),
                        monto,
                        confianza,
                    )

                resumen = ", ".join(
                    f"cat={cid} m={m} conf={round(cf, 3)}"
                    for cid, m, cf in filas
                )
                print(
                    f"[OK] usuario={args.usuario_id} fecha={fecha.date()} "
                    f"filas={len(filas)} | {resumen}"
                )

            conn.commit()
    finally:
        conn.close()


if __name__ == "__main__":
    main()
