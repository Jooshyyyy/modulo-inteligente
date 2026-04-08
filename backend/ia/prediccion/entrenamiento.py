import pandas as pd
from sqlalchemy import create_engine
from sklearn.ensemble import HistGradientBoostingClassifier, HistGradientBoostingRegressor
from sklearn.model_selection import train_test_split, RandomizedSearchCV, KFold
from sklearn.metrics import mean_absolute_error, accuracy_score
import joblib
import numpy as np
import os
from dotenv import load_dotenv

def train():
    env_path = os.path.abspath(os.path.join(os.path.dirname(__file__), '..', '..', '.env'))
    load_dotenv(env_path)
    
    db_user = os.getenv("DB_USER", "postgres.cnhlbiawrrgbvydizkjt")
    db_password = os.getenv("DB_PASSWORD", "modulo_inteligente")
    db_host = os.getenv("DB_HOST", "aws-0-us-west-2.pooler.supabase.com")
    db_port = os.getenv("DB_PORT", "5432")
    db_name = os.getenv("DB_NAME", "postgres")

    engine = create_engine(f"postgresql+psycopg://{db_user}:{db_password}@{db_host}:{db_port}/{db_name}")

    query = """
        SELECT fecha, monto, tipo, categoria_id
        FROM movimientos
        WHERE tipo = 'EGRESO'
    """
    df = pd.read_sql_query(query, engine)
    print(f"Datos cargados: {len(df)}")

    df['fecha'] = pd.to_datetime(df['fecha'], errors='coerce')
    df['monto'] = pd.to_numeric(df['monto'], errors='coerce')
    df['categoria_id'] = df['categoria_id'].astype(int)
    df = df.dropna(subset=['fecha', 'monto']).sort_values('fecha')

    # --- FEATURE ENGINEERING ---
    df['dia_semana'] = df['fecha'].dt.dayofweek
    df['dia_mes'] = df['fecha'].dt.day
    df['mes'] = df['fecha'].dt.month
    df['dia_anio'] = df['fecha'].dt.dayofyear
    df['hora'] = df['fecha'].dt.hour
    
    # Transformaciones Cíclicas (Esto es excelente, mantenlo)
    df['dia_semana_sin'] = np.sin(2 * np.pi * df['dia_semana'] / 7)
    df['dia_semana_cos'] = np.cos(2 * np.pi * df['dia_semana'] / 7)
    df['dia_mes_sin'] = np.sin(2 * np.pi * df['dia_mes'] / 31)
    df['dia_mes_cos'] = np.cos(2 * np.pi * df['dia_mes'] / 31)
    df['hora_sin'] = np.sin(2 * np.pi * df['hora'] / 24)
    df['hora_cos'] = np.cos(2 * np.pi * df['hora'] / 24)

    features_base = [
        'dia_anio', 'dia_semana', 'dia_mes', 'mes', 'hora',
        'dia_semana_sin', 'dia_semana_cos',
        'dia_mes_sin', 'dia_mes_cos',
        'hora_sin', 'hora_cos'
    ]

    # ==============================
    # 1. MODELO CATEGORIA (Classifier)
    # ==============================
    X_cat = df[features_base]
    y_cat = df['categoria_id']

    X_train_cat, X_test_cat, y_train_cat, y_test_cat = train_test_split(
        X_cat, y_cat, test_size=0.15, random_state=42 # Menos test para dar más datos al entrenamiento
    )

    # Grid más agresivo
    param_grid_cat = {
        'learning_rate': [0.05, 0.1],
        'max_iter': [300, 500],
        'max_depth': [15, 20, None],
        'l2_regularization': [0, 0.1, 1.0]
    }

    print("Entrenando Categoría...")
    modelo_cat = HistGradientBoostingClassifier(random_state=42)
    
    # Aumentamos n_iter para no dejar la precisión al azar
    grid_cat = RandomizedSearchCV(modelo_cat, param_grid_cat, n_iter=20, cv=3, n_jobs=-1, random_state=42)
    grid_cat.fit(X_train_cat, y_train_cat)
    
    acc = accuracy_score(y_test_cat, grid_cat.predict(X_test_cat))
    print(f"Accuracy Final Categoría: {acc:.4f}")
    joblib.dump(grid_cat.best_estimator_, 'modelo_categoria.pkl')

    # ==============================
    # 2. MODELO MONTO (Regressor)
    # ==============================
    X_monto = df[['categoria_id'] + features_base]
    y_monto = df['monto']

    X_train_m, X_test_m, y_train_m, y_test_m = train_test_split(X_monto, y_monto, test_size=0.15, random_state=42)

    print("Entrenando Monto...")
    # IMPORTANTE: Indicamos que 'categoria_id' es categórica (índice 0 en X_monto)
    modelo_monto = HistGradientBoostingRegressor(categorical_features=[0], random_state=42)
    
    grid_monto = RandomizedSearchCV(modelo_monto, param_grid_cat, n_iter=20, cv=3, n_jobs=-1, random_state=42)
    grid_monto.fit(X_train_m, y_train_m)
    
    mae = mean_absolute_error(y_test_m, grid_monto.predict(X_test_m))
    print(f"MAE Final Monto: {mae:.2f} BOB")
    joblib.dump(grid_monto.best_estimator_, 'modelo_monto.pkl')

if __name__ == "__main__":
    train()