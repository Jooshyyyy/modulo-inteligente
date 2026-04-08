from fastapi import FastAPI
from pydantic import BaseModel
from transformers import pipeline

app = FastAPI(title="Categorizador de Gastos IA", description="Microservicio Zero-Shot para categorización asíncrona de movimientos bancarios")

# Cargamos el modelo a la memoria al arrancar para evitar re-carga de Gb en cada petición.
print("Cargando modelo multilingüe MoritzLaurer/mDeBERTa-v3-base-mnli-xnli en memoria RAM... \n(Esto tardará unos segundos en arrancar)")
try:
    clasificador = pipeline("zero-shot-classification", model="MoritzLaurer/mDeBERTa-v3-base-mnli-xnli")
    print("Modelo cargado con éxito. FastAPI escuchando en puerto.")
except Exception as e:
    print(f"Error al cargar el modelo: {e}")

CATEGORIAS_DISPONIBLES = [
    'Alimentación', 
    'Transporte', 
    'Vivienda', 
    'Servicios', 
    'Educación', 
    'Salud', 
    'Entretenimiento', 
    'Compras', 
    'Transferencias', 
    'Otros'
]

# Umbral bajo tal como solicitado para forzar categorización frente a "Otros"
UMBRAL_CONFIANZA = 0.15 

class TransaccionInput(BaseModel):
    descripcion: str

@app.post("/categorizar")
def categorizar_movimiento(transaccion: TransaccionInput):
    hypothesis_template = "Este tipo de gasto es de {}."
    
    # Procesar la clasificación localmente
    resultado = clasificador(
        transaccion.descripcion, 
        CATEGORIAS_DISPONIBLES, 
        hypothesis_template=hypothesis_template
    )
    
    categoria_predicha = resultado['labels'][0]
    confianza_predicha = resultado['scores'][0]
    
    if confianza_predicha < UMBRAL_CONFIANZA:
        categoria_final = 'Otros'
    else:
        categoria_final = categoria_predicha
        
    return {
        "descripcion": transaccion.descripcion,
        "categoria": categoria_final,
        "confianza": confianza_predicha
    }
