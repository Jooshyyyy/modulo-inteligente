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
    # Generar hasta el 30 de abril de 2026 (día anterior al inicio de predicciones)
    FECHA_FIN = datetime(2026, 4, 30)
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

    # Configuración con patrones MUY marcados
    config_cats = {
        'alimentación': {
            'prob_base': 0.7, 'dist': 'lognormal', 'params': (3.0, 0.4),
            'horas': [12,13,14,19,20,21], 'dias_semana': [0,1,2,3,4,5,6],
            'monto_min': 15, 'monto_max': 80
        },
        'transporte': {
            'prob_base': 0.6, 'dist': 'normal', 'params': (25, 8),
            'horas': [7,8,9,17,18,19], 'dias_semana': [0,1,2,3,4],
            'monto_min': 10, 'monto_max': 50
        },
        'vivienda': {
            'prob_base': 0.0, 'prob_dias_especiales': 0.95, 'dias_mes': [1,2],
            'dist': 'normal', 'params': (1000, 150),  # 1000 +- 150
            'horas': [9,10,11], 'monto_min': 800, 'monto_max': 1300
        },
        'servicios': {
            'prob_base': 0.0, 'prob_dias_especiales': 0.9, 'dias_mes': [1,2,3],
            'dist': 'normal', 'params': (300, 60),  # 300 +- 60
            'horas': [10,11,12], 'monto_min': 200, 'monto_max': 450
        },
        'educación': {
            'prob_base': 0.03, 'dist': 'normal', 'params': (2500, 500),
            'horas': [10,11,12], 'meses': [2,8], 'dias_mes': [14,15,16],
            'monto_min': 1500, 'monto_max': 3500
        },
        'salud': {
            'prob_base': 0.1, 'dist': 'gamma', 'params': (2, 120),
            'horas': [8,9,10,15,16], 'dias_semana': [0,1,2,3,4],
            'monto_min': 30, 'monto_max': 500
        },
        'entretenimiento': {
            'prob_base': 0.0, 'prob_fin_semana': 0.85, 'dias_semana': [4,5],  # viernes y sábado
            'dist': 'gamma', 'params': (2, 90), 'horas': [18,19,20,21,22],
            'monto_min': 80, 'monto_max': 350
        },
        'compras': {
            'prob_base': 0.0, 'prob_fin_semana': 0.7, 'dias_semana': [4,5,6],
            'dist': 'lognormal', 'params': (4.5, 0.7), 'horas': [11,12,16,17,18],
            'monto_min': 50, 'monto_max': 800
        },
        'transferencias': {
            'prob_base': 0.1, 'dist': 'normal', 'params': (300, 200),
            'horas': [9,10,11,14,15], 'dias_semana': [0,1,2,3,4],
            'monto_min': 50, 'monto_max': 800
        },
        'otros': {
            'prob_base': 0.02,  # muy baja para que no predomine
            'dist': 'exponential', 'params': (35,),
            'horas': [10,11,12,13,14,15,16,17,18],
            'monto_min': 5, 'monto_max': 150
        }
    }

    registros = []
    current_date = FECHA_INICIO

    while current_date <= FECHA_FIN:
        dia_sem = current_date.weekday()
        dia_mes = current_date.day
        mes = current_date.month
        n_trans = np.random.poisson(4.0)  # 4 transacciones promedio por día
        n_trans = min(n_trans, 10)
        if random.random() < 0.05:  # solo 5% días sin gastos
            n_trans = 0

        for _ in range(n_trans):
            candidatas = []
            probs = []
            for nombre, cfg in config_cats.items():
                # Verificar restricciones de día de semana
                if 'dias_semana' in cfg and dia_sem not in cfg['dias_semana']:
                    continue
                # Verificar restricciones de día del mes (para vivienda, servicios)
                if 'dias_mes' in cfg and dia_mes not in cfg['dias_mes']:
                    continue
                # Verificar meses específicos (educación)
                if 'meses' in cfg and mes not in cfg['meses']:
                    continue
                # Probabilidad según el tipo de día
                if 'prob_dias_especiales' in cfg and dia_mes in cfg['dias_mes']:
                    prob = cfg['prob_dias_especiales']
                elif 'prob_fin_semana' in cfg and dia_sem in cfg['dias_semana']:
                    prob = cfg['prob_fin_semana']
                else:
                    prob = cfg.get('prob_base', 0.0)
                if prob <= 0:
                    continue
                candidatas.append(nombre)
                probs.append(prob)
            if not candidatas:
                continue
            total = sum(probs)
            probs = [p/total for p in probs]
            cat_nombre = np.random.choice(candidatas, p=probs)
            cat_id = cats[cat_nombre]
            cfg = config_cats[cat_nombre]

            # Generar monto según distribución
            if cfg['dist'] == 'normal':
                mean, std = cfg['params']
                monto = max(cfg['monto_min'], np.random.normal(mean, std))
            elif cfg['dist'] == 'lognormal':
                mean_log, sigma = cfg['params']
                monto = np.random.lognormal(mean_log, sigma)
            elif cfg['dist'] == 'gamma':
                shape, scale = cfg['params']
                monto = np.random.gamma(shape, scale)
            else:  # exponential
                scale = cfg['params'][0]
                monto = np.random.exponential(scale)
            monto = min(cfg['monto_max'], max(cfg['monto_min'], round(monto, 2)))

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
        print(f"{len(registros)} movimientos insertados (hasta abril 2026).")

    conn.close()

if __name__ == "__main__":
    run()