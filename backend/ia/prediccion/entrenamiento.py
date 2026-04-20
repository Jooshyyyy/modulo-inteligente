import os
import pandas as pd
from sqlalchemy import create_engine
from sklearn.ensemble import HistGradientBoostingClassifier, HistGradientBoostingRegressor
from sklearn.model_selection import TimeSeriesSplit
from sklearn.metrics import f1_score, r2_score, mean_squared_error
import joblib
import numpy as np
from dotenv import load_dotenv

from dotenv import load_dotenv

load_dotenv(dotenv_path=os.path.join(os.path.dirname(__file__), "../../.env"))

def train():
    db_user = os.getenv("DB_USER")
    db_password = os.getenv("DB_PASSWORD")
    db_host = os.getenv("DB_HOST")
    db_port = os.getenv("DB_PORT")
    db_name = os.getenv("DB_NAME")

    if not all([db_user, db_password, db_host, db_port, db_name]):
        raise RuntimeError("Variables de entorno no cargadas correctamente")

    engine = create_engine(
        f"postgresql+psycopg://{db_user}:{db_password}@{db_host}:{int(db_port)}/{db_name}"
    )
    df = pd.read_sql("SELECT fecha, monto, categoria_id FROM movimientos WHERE tipo='EGRESO'", engine)

    df['fecha'] = pd.to_datetime(df['fecha'])
    df = df.sort_values("fecha")

    df['dia_semana'] = df['fecha'].dt.dayofweek
    df['mes'] = df['fecha'].dt.month
    df['dia_mes'] = df['fecha'].dt.day
    df['dia_anio'] = df['fecha'].dt.dayofyear
    df['hora'] = df['fecha'].dt.hour

    df['dia_semana_sin'] = np.sin(2*np.pi*df['dia_semana']/7)
    df['dia_semana_cos'] = np.cos(2*np.pi*df['dia_semana']/7)
    df['dia_mes_sin'] = np.sin(2*np.pi*df['dia_mes']/31)
    df['dia_mes_cos'] = np.cos(2*np.pi*df['dia_mes']/31)
    df['hora_sin'] = np.sin(2*np.pi*df['hora']/24)
    df['hora_cos'] = np.cos(2*np.pi*df['hora']/24)

    df['monto_rolling_7'] = df['monto'].rolling(7, min_periods=1).mean()
    df['monto_rolling_30'] = df['monto'].rolling(30, min_periods=1).mean()

    features = [
        'dia_anio','dia_semana','dia_mes','mes','hora',
        'dia_semana_sin','dia_semana_cos',
        'dia_mes_sin','dia_mes_cos',
        'hora_sin','hora_cos',
        'monto_rolling_7','monto_rolling_30'
    ]

    tscv = TimeSeriesSplit(n_splits=3)

    model_cat = HistGradientBoostingClassifier(
        max_depth=6,
        learning_rate=0.05,
        max_iter=300,
        l2_regularization=1.0,
        random_state=42
    )

    f1_scores = []
    for train_idx, test_idx in tscv.split(df):
        train_df, test_df = df.iloc[train_idx], df.iloc[test_idx]
        model_cat.fit(train_df[features], train_df['categoria_id'])
        pred_cat = model_cat.predict(test_df[features])
        f1_scores.append(f1_score(test_df['categoria_id'], pred_cat, average='weighted'))

    print("F1 promedio:", np.mean(f1_scores))
    joblib.dump(model_cat, "modelo_categoria.pkl")

    df['log_monto'] = np.log(df['monto'] + 1)

    model_monto = HistGradientBoostingRegressor(
        max_depth=6,
        learning_rate=0.05,
        max_iter=300,
        l2_regularization=1.0,
        random_state=42
    )

    r2_scores, rmse_scores = [], []
    for train_idx, test_idx in tscv.split(df):
        train_df, test_df = df.iloc[train_idx], df.iloc[test_idx]
        model_monto.fit(train_df[features+['categoria_id']], train_df['log_monto'])
        pred_log = model_monto.predict(test_df[features+['categoria_id']])
        pred_monto = np.exp(pred_log) - 1
        r2_scores.append(r2_score(test_df['monto'], pred_monto))
        rmse_scores.append(np.sqrt(mean_squared_error(test_df['monto'], pred_monto)))

    print("R2 promedio:", np.mean(r2_scores))
    print("RMSE promedio:", np.mean(rmse_scores))
    joblib.dump(model_monto, "modelo_monto.pkl")

if __name__ == "__main__":
    train()
