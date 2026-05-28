#!/usr/bin/env python3
# -*- coding: utf-8 -*-

import os
import random
import numpy as np
from datetime import datetime, timedelta
import psycopg
from psycopg.rows import dict_row
from dotenv import load_dotenv

load_dotenv(dotenv_path=os.path.join(os.path.dirname(__file__), "../../.env"))

def run():
    EMAIL_USUARIO = 'esteban@gmail.com'
    FECHA_FIN = datetime.now()
    FECHA_INICIO = FECHA_FIN - timedelta(days=1095)  # 3 años

    db_user = os.getenv("DB_USER")
    db_password = os.getenv("DB_PASSWORD")
    db_host = os.getenv("DB_HOST")
    db_port = os.getenv("DB_PORT")
    db_name = os.getenv("DB_NAME")

    conn = psycopg.connect(
        dbname=db_name, user=db_user, password=db_password,
        host=db_host, port=db_port, row_factory=dict_row
    )

    with conn.cursor() as cur:
        cur.execute("SELECT id FROM usuarios WHERE LOWER(email)=LOWER(%s)", (EMAIL_USUARIO,))
        usuario = cur.fetchone()
        if not usuario:
            raise RuntimeError("Usuario no encontrado")
        cur.execute("SELECT id FROM cuentas WHERE usuario_id=%s LIMIT 1", (usuario["id"],))
        cuenta = cur.fetchone()
        if not cuenta:
            raise RuntimeError("Cuenta no encontrada")
        cur.execute("SELECT id, nombre FROM categorias")
        cats = {row["nombre"].lower(): row["id"] for row in cur.fetchall()}

    # Configuración de categorías con patrones realistas
    # Las probabilidades base son altas para que haya muchos registros
    cat_config = {
        'alimentación': {
            'dist': 'lognormal', 'params': (3.0, 0.4), 'base_prob': 0.8,
            'horas': [12,13,14,19,20], 'dias': [0,1,2,3,4,5,6],
            'monto_min': 20, 'monto_max': 80
        },
        'transporte': {
            'dist': 'normal', 'params': (25, 8), 'base_prob': 0.7,
            'horas': [7,8,9,17,18,19], 'dias': [0,1,2,3,4],
            'monto_min': 15, 'monto_max': 45
        },
        'vivienda': {
            'dist': 'normal', 'params': (1000, 200), 'base_prob': 0.08,  # solo días específicos
            'horas': [9,10,11], 'dias': [0,1,2,3,4],  # se usará días_mes
            'dias_mes': [1,2], 'monto_min': 800, 'monto_max': 1200
        },
        'servicios': {
            'dist': 'normal', 'params': (300, 80), 'base_prob': 0.08,
            'horas': [10,11,12], 'dias_mes': [1,2],
            'monto_min': 220, 'monto_max': 380
        },
        'educación': {
            'dist': 'normal', 'params': (1500, 400), 'base_prob': 0.03,
            'horas': [10,11,12], 'dias_mes': [15,16], 'meses': [2,8],
            'monto_min': 1000, 'monto_max': 2000
        },
        'salud': {
            'dist': 'gamma', 'params': (2, 60), 'base_prob': 0.15,
            'horas': [8,9,10,15,16], 'dias': [0,1,2,3,4],
            'monto_min': 30, 'monto_max': 250
        },
        'entretenimiento': {
            'dist': 'gamma', 'params': (2, 70), 'base_prob': 0.5,
            'horas': [18,19,20,21,22], 'dias': [4,5,6],  # viernes, sábado, domingo
            'monto_min': 80, 'monto_max': 280
        },
        'compras': {
            'dist': 'lognormal', 'params': (4.2, 0.6), 'base_prob': 0.4,
            'horas': [11,12,16,17,18], 'dias': [4,5,6],  # fines de semana
            'monto_min': 80, 'monto_max': 300
        },
        'transferencias': {
            'dist': 'normal', 'params': (200, 150), 'base_prob': 0.2,
            'horas': [9,10,11,14,15], 'dias': [0,1,2,3,4],
            'monto_min': 50, 'monto_max': 600
        },
        'otros': {
            'dist': 'exponential', 'params': (40,), 'base_prob': 0.3,
            'horas': [10,11,12,13,14,15,16,17,18], 'dias': [0,1,2,3,4,5,6],
            'monto_min': 10, 'monto_max': 150
        }
    }

    registros = []
    current_date = FECHA_INICIO

    while current_date <= FECHA_FIN:
        dia_sem = current_date.weekday()  # 0 lunes, 6 domingo
        dia_mes = current_date.day
        mes = current_date.month

        # Número de transacciones por día (media 3.5, máx 8)
        n_trans = np.random.poisson(3.5)
        n_trans = min(n_trans, 8)
        if random.random() < 0.1:
            n_trans = 0  # 10% días sin gastos

        for _ in range(n_trans):
            # Seleccionar categoría según restricciones
            candidatas = []
            probs = []
            for nombre, cfg in cat_config.items():
                # Verificar restricciones de día de semana
                if 'dias' in cfg and dia_sem not in cfg['dias']:
                    continue
                # Verificar restricciones de día de mes
                if 'dias_mes' in cfg and dia_mes not in cfg['dias_mes']:
                    continue
                # Verificar meses específicos
                if 'meses' in cfg and mes not in cfg['meses']:
                    continue
                candidatas.append(nombre)
                prob = cfg['base_prob']
                # Ajuste: aumentar probabilidad de vivienda/servicios los días 1-2
                if nombre in ['vivienda', 'servicios'] and dia_mes in [1,2]:
                    prob = 0.9  # muy probable
                # Aumentar entretenimiento/compras en fines de semana
                if nombre in ['entretenimiento', 'compras'] and dia_sem in [4,5,6]:
                    prob *= 2.0
                probs.append(prob)

            if not candidatas:
                continue
            total = sum(probs)
            probs = [p/total for p in probs]
            cat_nombre = np.random.choice(candidatas, p=probs)
            cat_id = cats[cat_nombre]
            cfg = cat_config[cat_nombre]

            # Generar monto según distribución
            if cfg['dist'] == 'normal':
                mean, std = cfg['params']
                monto = max(0, np.random.normal(mean, std))
            elif cfg['dist'] == 'lognormal':
                mean_log, sigma = cfg['params']
                monto = np.random.lognormal(mean_log, sigma)
            elif cfg['dist'] == 'gamma':
                shape, scale = cfg['params']
                monto = np.random.gamma(shape, scale)
            else:
                scale = cfg['params'][0]
                monto = np.random.exponential(scale)
            # Aplicar límites
            if 'monto_min' in cfg:
                monto = max(monto, cfg['monto_min'])
            if 'monto_max' in cfg:
                monto = min(monto, cfg['monto_max'])
            monto = round(monto, 2)

            # Elegir hora dentro de las permitidas
            hora = random.choice(cfg['horas'])
            minuto = random.randint(0, 59)
            fecha_hora = current_date.replace(hour=hora, minute=minuto)

            registros.append((
                cuenta["id"], monto, 'EGRESO', cat_nombre.capitalize(),
                fecha_hora, 'QR', cat_id, 'COMPLETADO'
            ))

        current_date += timedelta(days=1)

    with conn.cursor() as cur:
        cur.execute("TRUNCATE TABLE movimientos RESTART IDENTITY CASCADE")
        query = """INSERT INTO movimientos 
                   (cuenta_id, monto, tipo, concepto, fecha, tipo_transaccion, categoria_id, estado)
                   VALUES (%s,%s,%s,%s,%s,%s,%s,%s)"""
        for i in range(0, len(registros), 500):
            cur.executemany(query, registros[i:i+500])
        conn.commit()
        print(f"{len(registros)} movimientos generados (3 años con patrones realistas).")

    conn.close()

if __name__ == "__main__":
    run()