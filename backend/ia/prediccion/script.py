#!/usr/bin/env python3
# -*- coding: utf-8 -*-

import os
import random
from datetime import datetime, timedelta
import psycopg
from psycopg.rows import dict_row
from dotenv import load_dotenv

os.environ["PGCLIENTENCODING"] = "utf8"

def run():
    env_path = os.path.abspath(os.path.join(os.path.dirname(__file__), '..', '..', '.env'))
    load_dotenv(env_path)

    EMAIL_USUARIO = 'esteban@gmail.com'
    # Generamos registros para cubrir los 4000 solicitados
    FECHA_FIN = datetime.now()
    FECHA_INICIO = FECHA_FIN - timedelta(days=800) # ~2.2 años para tener volumen
    registros = []

    try:
        conn = psycopg.connect(
            conninfo="postgres://postgres.cnhlbiawrrgbvydizkjt:modulo_inteligente@aws-0-us-west-2.pooler.supabase.com:5432/postgres",
            row_factory=dict_row
        )
        print("Conexión exitosa a Supabase")
    except Exception as e:
        print(f"Error de conexión: {e}")
        return

    def clean_text(t):
        return t.lower().replace("á","a").replace("é","e").replace("í","i").replace("ó","o").replace("ú","u").replace(" ","")

    if conn:
        with conn.cursor() as cur:
            cur.execute("SELECT id FROM usuarios WHERE LOWER(email)=LOWER(%s)", (EMAIL_USUARIO,))
            usuario_id = cur.fetchone()["id"]
            cur.execute("SELECT id FROM cuentas WHERE usuario_id=%s LIMIT 1", (usuario_id,))
            cuenta_id = cur.fetchone()["id"]
            cur.execute("SELECT id, nombre FROM categorias;")
            cat_map = {clean_text(row["nombre"]): row["id"] for row in cur.fetchall()}

    # ==========================================
    # GENERADOR DE ALTA DENSIDAD Y ACCURACY
    # ==========================================
    current_date = FECHA_INICIO
    while current_date <= FECHA_FIN:
        dia_sem = current_date.weekday()
        dia_mes = current_date.day
        mes = current_date.month

        # --- PATRÓN 1: TRANSPORTE (Usa Micro en semana, Taxi en fin de semana) ---
        if dia_sem <= 4: # Lunes a Viernes
            monto_trans = 2.00 # Micro ida y vuelta = 4.00, pero pongamos viajes individuales
            registros.append((cuenta_id, 2.00, 'EGRESO', 'Pasaje Micro (Efectivo)', 
                            current_date.replace(hour=7, minute=0), 'EFECTIVO', cat_map['transporte'], 'COMPLETADO'))
            registros.append((cuenta_id, 2.00, 'EGRESO', 'Pasaje Micro (Efectivo)', 
                            current_date.replace(hour=14, minute=0), 'EFECTIVO', cat_map['transporte'], 'COMPLETADO'))
        else: # Sábado y Domingo (Fines de semana, usa Taxi)
            monto_taxi = round(random.uniform(20.0, 35.0), 2)
            registros.append((cuenta_id, monto_taxi, 'EGRESO', 'Taxi/Uber Fin de Semana', 
                            current_date.replace(hour=random.choice([19, 21, 23]), minute=random.randint(0, 59)), 'PAGO_QR', cat_map['transporte'], 'COMPLETADO'))

        # --- PATRÓN 2: ALIMENTACIÓN (Almuerzo universitario, más caro findes) ---
        base_almuerzo = 15.0 if dia_sem <= 4 else 50.0 # Entre semana pensión/comida en la U, fin de semana comida fuerte
        monto_almuerzo = round(random.uniform(base_almuerzo - 2, base_almuerzo + 15), 2)
        registros.append((cuenta_id, monto_almuerzo, 'EGRESO', 'Almuerzo/Cena', 
                        current_date.replace(hour=13, minute=0), 'PAGO_QR', cat_map['alimentacion'], 'COMPLETADO'))
        
        # Snack/Café
        if dia_sem in [1, 3]:
            monto_snack = round(random.uniform(10.0, 18.0), 2)
            registros.append((cuenta_id, monto_snack, 'EGRESO', 'Snack U', 
                            current_date.replace(hour=16, minute=0), 'PAGO_QR', cat_map['alimentacion'], 'COMPLETADO'))

        # --- PATRÓN 3: VIVIENDA Y SERVICIOS FIJOS (Inicio de mes) ---
        if dia_mes == 1:
            monto_alquiler = round(random.uniform(1400.0, 1600.0), 2) # Alquiler de cuarto/depa pequeño
            registros.append((cuenta_id, monto_alquiler, 'EGRESO', 'Alquiler Departamento', 
                            current_date.replace(hour=9, minute=0), 'TRANSFERENCIA', cat_map['vivienda'], 'COMPLETADO'))
        
        if dia_mes == 2:
            registros.append((cuenta_id, 160.00, 'EGRESO', 'Internet Tigo/Entel', 
                            current_date.replace(hour=10, minute=0), 'TRANSFERENCIA', cat_map['servicios'], 'COMPLETADO'))

        # --- PATRÓN 4: EDUCACIÓN (Materiales esporádicos) ---
        if dia_mes == 15 and mes in [2, 8]:
            monto_libros = round(random.uniform(200.0, 300.0), 2)
            registros.append((cuenta_id, monto_libros, 'EGRESO', 'Material Universitario', 
                            current_date.replace(hour=11, minute=0), 'PAGO_QR', cat_map['educacion'], 'COMPLETADO'))

        # --- PATRÓN 5: ENTRETENIMIENTO (Excesivo fin de semana) ---
        if dia_sem in [4, 5]: # Viernes o Sábado
            monto_ocio = round(random.uniform(100.0, 250.0), 2)
            registros.append((cuenta_id, monto_ocio, 'EGRESO', 'Salida/Boliche/Cine', 
                            current_date.replace(hour=22, minute=0), 'PAGO_QR', cat_map['entretenimiento'], 'COMPLETADO'))

        # --- PATRÓN 6: RUIDO ESPORÁDICO ---
        if random.random() < 0.2:
            monto_extra = round(random.uniform(5.0, 15.0), 2)
            registros.append((cuenta_id, monto_extra, 'EGRESO', 'Gasto Hormiga (Dulces/Gaseosa)', 
                            current_date.replace(hour=15, minute=0), 'EFECTIVO', cat_map['otros'], 'COMPLETADO'))

        current_date += timedelta(days=1)
        if len(registros) >= 4000: break

    # =============================
    # INSERCIÓN MASIVA
    # =============================
    if conn:
        try:
            with conn.cursor() as cur:
                print("Limpiando datos antiguos de la tabla 'movimientos'...")
                cur.execute("TRUNCATE TABLE movimientos RESTART IDENTITY;")
                print("Tabla limpiada con éxito.")
                
                query = """INSERT INTO movimientos (cuenta_id, monto, tipo, concepto, fecha, tipo_transaccion, categoria_id, estado)
                           VALUES (%s,%s,%s,%s,%s,%s,%s,%s)"""
                # Usamos lotes para no saturar el pooler de Supabase
                for i in range(0, len(registros), 500):
                    cur.executemany(query, registros[i:i+500])
                conn.commit()
                print(f"Éxito: {len(registros)} registros insertados con patrones de alta precisión.")
        except Exception as e:
            print(f"Error: {e}")
            conn.rollback()
        finally:
            conn.close()

if __name__ == "__main__":
    run()