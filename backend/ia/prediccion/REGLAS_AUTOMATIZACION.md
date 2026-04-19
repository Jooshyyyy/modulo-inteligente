# Reglas de ejecucion IA

## Reglas activas en backend

- Lunes 00:00: se ejecuta reentrenamiento (`entrenamiento.py`) y luego se generan predicciones semanales.
- Diario 03:00: se valida si se debe mostrar prediccion del dia segun confianza minima.
- Usuario objetivo por defecto: `10` (`PREDICTION_USER_ID`).
- Umbral minimo para mostrar en dashboard: `0.35` (`PREDICTION_MIN_CONFIDENCE`).

## Comandos manuales

- Reentrenar: `npm run ia:train`
- Generar semana (usuario 10): `npm run ia:predict:week`
- Validar visibilidad de hoy: `npm run ia:validate:today`

## Variables opcionales

- `PREDICTION_USER_ID`
- `PREDICTION_MIN_CONFIDENCE`
- `PYTHON_BIN`
