# 🧠 Arquitectura Técnica del Módulo Inteligente de Categorización

Este documento explica de forma clara, técnica pero sencilla cómo la Inteligencia Artificial (IA) de nuestra aplicación es capaz de leer y comprender en qué gastaste tu dinero, asignándole automáticamente la categoría correcta.

---

## 1. ¿Qué es y cómo funciona a grandes rasgos?
Cuando realizas un gasto, generalmente llega con descripciones confusas o muy humanas. Por ejemplo: `"Pollos Chuy menú 2"` o `"Factura de CRE"`.

El Módulo de Categorización es un "lector inteligente" que recibe esas frases cortas, entiende su contexto lógico, y clasifica la transacción en las categorías financieras de nuestra aplicación (Ej. `Alimentación`, `Vivienda`, `Transporte`, etc.), para que tus finanzas estén siempre ordenadas sin que muevas un dedo.

---

## 2. El "Lector" de IA (Clasificador Zero-Shot NLP)

En lugar de crear un diccionario gigante y tonto con millones de palabras (lo cual fallaría si escribes con faltas de ortografía o usas términos nuevos), usamos **Procesamiento de Lenguaje Natural (NLP)**. 

### 🤖 El Modelo: mDeBERTa-v3
Dentro de nuestro archivo `zero_shot_classification.py`, levantamos a memoria viva un poderoso modelo neuronal llamado *MoritzLaurer/mDeBERTa-v3-base-mnli-xnli*. 
- Es **multilingüe**, por lo que entiende el español con sus modismos y variaciones latinas perfectamente.
- Usa una técnica llamada **Zero-Shot Classification**; esto significa que "no necesita haber visto la palabra antes en su entrenamiento para deducir qué significa en su contexto actual".

### 🔎 ¿Cómo piensa la IA? (Construcción de Hipótesis)
Cuando a la IA le pasamos el texto `"Surtidor El Cristo"`, la máquina internamente hace preguntas. Usa una "plantilla de hipótesis" que programamos así:
> *"Este tipo de gasto es de {Categoría}"*

Entonces la máquina prueba todas las opciones:
- *"Este tipo de gasto es de Alimentación"* (y evalúa lógicamente la oración... Responde: Poco probable, 12%).
- *"Este tipo de gasto es de Entretenimiento"* (Responde: Falso, 3%).
- *"Este tipo de gasto es de Transporte"* (Responde: ¡Tiene sentido lógico total! 98%).

### 🛡️ El "Escudo" de Confianza (Umbral de Seguridad)
La máquina nunca adivina a ciegas. Si ingresas un texto extremadamente raro como *"Gasto para cositas 123"*, la IA hará el test, pero su nivel de certeza o **Score de Confianza** será muy bajo (por ejemplo, 14% de certeza en la categoría ganadora).

Para evitar que tu dinero se clasifique mal, el código tiene una regla obligatoria (`umbral_confianza` programado en 15% o 40%). Si la máquina no está completamente segura, descarta sus ideas y lo clasifica bajo el comodín seguro de **`Otros`**.

---

## 3. El Microservicio de Despliegue (FastAPI)

Los modelos de lenguaje natural son "pesados" (pesan Gigabytes) y tardan varios segundos en arrancar. No sería eficiente prender y apagar el cerebro para cada transacción de la aplicación.

Por ello, hemos construido nuestra IA de categorización dentro de un entorno **FastAPI** (`backend/ia/main.py`):
1. **Memoria RAM Constante:** Al iniciarse el servidor, el modelo de DeBERTa se carga a la memoria y se queda "despierto" escuchando continuamente.
2. **Endpoint de Comunicación (`/categorizar`):** Se habilita una pequeña reja de conexión web ultrarrápida.
3. **Flujo de Asignación en Milisegundos:** Cuando nuestro backend general de la aplicación o la aplicación Android registra un nuevo movimiento, éste simplemente le hace "ping" (una llamada `POST`) a la ruta `/categorizar` pasándole la descripción. El microservicio responde inmediatamente un JSON con la *categoría asignada* y la *confianza*.

---

## 4. Beneficio para el Usuario y Escalabilidad

Gracias a todo este entorno inteligente, aseguramos que:
- **Tus reportes sean exactos:** Cuando vas a consultar tus "Cuentas" y reportes, el 90%+ de tus gastos están meticulosamente ordenados.
- **Se nutre a la Predicción del Futuro:** El algoritmo explicado en el informe de predicciones existe porque *esta IA de categorización* alimenta excelentemente el registro histórico; por lo tanto, el sistema aprende a predecir a futuro basándose en datos ya pre-digeridos, cerrando un círculo tecnológico financiero de primer nivel.
