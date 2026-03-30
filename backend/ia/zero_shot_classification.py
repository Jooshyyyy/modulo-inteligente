import json
from transformers import pipeline

# NOTA PARA EL DESARROLLADOR: 
# Para correr este script necesitas instalar las dependencias:
# pip install transformers torch sentencepiece

class CategorizadorGastos:
    def __init__(self):
        # Usamos un modelo multilingüe eficiente (Zero-Shot) basado en DeBERTa o XLM-RoBERTa
        # "MoritzLaurer/mDeBERTa-v3-base-mnli-xnli" es excelente para español
        self.modelo_nombre = "MoritzLaurer/mDeBERTa-v3-base-mnli-xnli"
        print(f"Cargando modelo de Machine Learning ({self.modelo_nombre})... esto podría tardar unos segundos. \n")
        self.classifier = pipeline("zero-shot-classification", model=self.modelo_nombre)
        
        # Estas categorías deben hacer match exacto con el campo "nombre" de tu base de datos
        self.categorias = [
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

    def categorizar_transaccion(self, descripcion_transaccion, umbral_confianza=0.40):
        """
        Analiza una descripción de transacción y le asigna la categoría correspondiente.
        """
        # Hipótesis que el modelo intentará resolver en base a cada categoría
        hypothesis_template = "Este tipo de gasto es de {}."
        
        resultado = self.classifier(
            descripcion_transaccion, 
            self.categorias, 
            hypothesis_template=hypothesis_template
        )
        
        categoria_predicha = resultado['labels'][0]
        confianza_predicha = resultado['scores'][0]
        
        # Lógica de seguridad: si el modelo no está muy seguro, mandarlo a 'Otros'
        if confianza_predicha < umbral_confianza:
            categoria_final = 'Otros'
        else:
            categoria_final = categoria_predicha
            
        return {
            "descripcion": descripcion_transaccion,
            "categoria_asignada": categoria_final,
            "confianza": confianza_predicha,
            "todas_las_probabilidades": {resultado['labels'][i]: resultado['scores'][i] for i in range(len(resultado['labels']))}
        }

if __name__ == "__main__":
    # Inicializar la clase (carga la IA a memoria)
    ia_categorizacion = CategorizadorGastos()
    
    # ------------------ PRUEBA DEL ALGORITMO ------------------
    # Lista simulando las 'descripciones' de los movimientos de la Base de Datos
    transacciones_prueba = [
        "Surtidor El Cristo 100 bs",                 # Debería ser Transporte
        "Pollos Chuy Menu personal",                  # Debería ser Alimentación
        "Mensualidad UAGRM",                          # Debería ser Educación
        "Pago Alquiler Marzo Condominio",             # Debería ser Vivienda
        "Recibo CRE Factura luz",                     # Debería ser Servicios
        "Transferencia QR a Maria Perez",             # Debería ser Transferencias
        "Farmacorp 2 paracetamol",                    # Debería ser Salud
        "Cine Center entradas y pipocas",             # Debería ser Entretenimiento
        "Multicenter ropa nueva",                     # Debería ser Compras
        "Compra rara y mal descrita 123",             # Debería irse a Otros por confianza baja
    ]
    
    print("--------------------------------------------------")
    print("INICIANDO CATEGORIZACIÓN AUTOMÁTICA (Zero-Shot)")
    print("--------------------------------------------------")
    
    for descripcion in transacciones_prueba:
        res = ia_categorizacion.categorizar_transaccion(descripcion)
        # Formateando salida para propósitos de demostración
        categoria = res['categoria_asignada']
        confianza = res['confianza']
        
        print(f"📝 Transacción : '{descripcion}'")
        print(f"🤖 IA Predice  : {categoria.upper()} (Certeza: {confianza*100:.2f}%)")
        print("-" * 50)
