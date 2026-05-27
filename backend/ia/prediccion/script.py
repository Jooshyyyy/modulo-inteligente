#!/usr/bin/env python3
# -*- coding: utf-8 -*-

import os
import random
import numpy as np
from datetime import datetime, timedelta
import psycopg
from psycopg.rows import dict_row
from dotenv import load_dotenv

# Cargar variables desde backend/.env
load_dotenv(dotenv_path=os.path.join(os.path.dirname(__file__), "../../.env"))

def run():
    EMAIL_USUARIO = 'esteban@gmail.com'
    FECHA_FIN = datetime.now()
    FECHA_INICIO = FECHA_FIN - timedelta(days=730)

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

    with conn.cursor() as cur:
        cur.execute("SELECT id FROM usuarios WHERE LOWER(email)=LOWER(%s)", (EMAIL_USUARIO,))
        row = cur.fetchone()
        if not row:
            raise RuntimeError("Usuario no encontrado")
        usuario_id = row["id"]

        cur.execute("SELECT id FROM cuentas WHERE usuario_id=%s LIMIT 1", (usuario_id,))
        row = cur.fetchone()
        if not row:
            raise RuntimeError("Cuenta no encontrada")
        cuenta_id = row["id"]

        cur.execute("SELECT id, nombre FROM categorias;")
        cats = {row["nombre"].lower(): row["id"] for row in cur.fetchall()}

    registros = []
    current_date = FECHA_INICIO

    while current_date <= FECHA_FIN:
        dia_sem = current_date.weekday()
        dia_mes = current_date.day
        mes = current_date.month

        if dia_sem < 5 and random.random() < 0.9:
            monto_trans = round(np.random.normal(25, 5), 2)
            registros.append((cuenta_id, monto_trans, 'EGRESO', 'Taxi',
                             current_date.replace(hour=random.choice([7,8,9])), 'QR', cats['transporte'], 'COMPLETADO'))
            if random.random() < 0.8:
                registros.append((cuenta_id, round(np.random.normal(25, 5), 2), 'EGRESO', 'Taxi regreso',
                                 current_date.replace(hour=random.choice([17,18,19])), 'QR', cats['transporte'], 'COMPLETADO'))

        monto_almuerzo = round(np.random.lognormal(mean=3.2, sigma=0.3), 2)
        registros.append((cuenta_id, monto_almuerzo, 'EGRESO', 'Almuerzo',
                         current_date.replace(hour=random.randint(12,14)), 'QR', cats['alimentación'], 'COMPLETADO'))

        if dia_sem >= 5:
            monto_comida = round(np.random.gamma(2, 50), 2)
            registros.append((cuenta_id, monto_comida, 'EGRESO', 'Comida fin de semana',
                             current_date.replace(hour=random.randint(13,15)), 'QR', cats['alimentación'], 'COMPLETADO'))

        if dia_mes in [1,2]:
            monto_alquiler = round(np.random.normal(2500, 300), 2)
            registros.append((cuenta_id, monto_alquiler, 'EGRESO', 'Alquiler mensual',
                             current_date.replace(hour=random.randint(8,11)), 'TRANSFERENCIA', cats['vivienda'], 'COMPLETADO'))
            monto_servicios = round(np.random.normal(200, 50), 2)
            registros.append((cuenta_id, monto_servicios, 'EGRESO', 'Servicios',
                             current_date.replace(hour=random.randint(9,12)), 'TRANSFERENCIA', cats['servicios'], 'COMPLETADO'))

        if dia_mes in [14,15,16] and mes in [2,8]:
            monto_edu = round(np.random.normal(2500, 400), 2)
            registros.append((cuenta_id, monto_edu, 'EGRESO', 'Material universitario',
                             current_date.replace(hour=random.randint(10,12)), 'QR', cats['educación'], 'COMPLETADO'))

        if random.random() < (0.2 if dia_sem < 5 else 0.5):
            monto_ocio = round(np.random.gamma(2, 80), 2)
            registros.append((cuenta_id, monto_ocio, 'EGRESO', 'Entretenimiento',
                             current_date.replace(hour=random.randint(18,23)), 'QR', cats['entretenimiento'], 'COMPLETADO'))

        if random.random() < 0.3:
            monto_extra = round(np.random.normal(15, 5), 2)
            registros.append((cuenta_id, monto_extra, 'EGRESO', 'Gasto hormiga',
                             current_date.replace(hour=random.randint(9,20)), 'EFECTIVO', cats['otros'], 'COMPLETADO'))

        current_date += timedelta(days=1)

    with conn.cursor() as cur:
        cur.execute("TRUNCATE TABLE movimientos RESTART IDENTITY;")
        query = """INSERT INTO movimientos 
                   (cuenta_id, monto, tipo, concepto, fecha, tipo_transaccion, categoria_id, estado)
                   VALUES (%s,%s,%s,%s,%s,%s,%s,%s)"""
        for i in range(0, len(registros), 500):
            cur.executemany(query, registros[i:i+500])
        conn.commit()
        print(f"{len(registros)} registros insertados.")
    conn.close()

if __name__ == "__main__":
    run()
