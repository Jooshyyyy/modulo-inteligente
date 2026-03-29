# Preguntas sobre la Categorización Automática con IA

Al revisar la propuesta para escalar la categorización de movimientos usando Zero-Shot Classification con Hugging Face `transformers`, me surgen algunas dudas importantes sobre cómo vamos a estructurar y desplegar esto de cara al futuro. Aquí tienes un listado para que las definamos:

## 1. Integración con el Backend Actual
Por lo que veo, el backend principal está construido con Node.js y Express (Javascript). Python no corre de forma nativa ahí.
- **¿Cómo planeas integrar esta IA de Python con Node.js?**
  - ¿Crear un **microservicio independiente** en Python (con FastAPI o Flask) que el backend de Node.js consuma por HTTP? *(Es la opción más recomendada y escalable).*
  - ¿O ejecutar el script de Python internamente desde Node mediante `child_process`? *(Suele ser más lento y problemático a largo plazo).*

## 2. Momento de la Ejecución y Tiempos de Respuesta
Cargar modelos en memoria y hacer inferencia (procesar texto) demora, sobre todo trabajando en CPU y no en GPU.
- **¿La categorización va a ser en tiempo real o asíncrona?**
  - ¿El usuario necesita ver la categoría apenas se registra la transacción (síncrono)?
  - ¿O podemos guardar la transacción y procesar la categorización *"por debajo"*, en colas o jobs de fondo (ejemplo: cron jobs o RabbitMQ/Redis), para no bloquear la respuesta al usuario?

## 3. Costos de Servidor y Hardware
Los modelos de \`transformers\` consumen bastante memoria RAM y poder computacional.
- **¿Dónde tienes pensado desplegar la IA?** Si planeamos usar algo como un servidor en la nube sin GPU (como AWS EC2 básico o un VPS barato), podríamos enfrentar lentitud o caídas por falta de RAM. 
- ¿Deberíamos apuntar a un modelo muy "liviano" (destilado) o a APIs externas (como OpenAI u otras) si los costos de servidor se disparan?

## 4. Regionalismos y Precisión
El modelo *Zero-Shot* general está pre-entrenado con español neutro o internacional. Palabras bolivianas o cruceñas como *"Trufi"*, *"Surtidor"*, *"Boliche"*, *"Expensas"* o *"Hipermaxi"* le dificultarán inferir adecuadamente.
- **¿Has considerado un "plan B" en caso de que Zero-Shot confunda mucho estas palabras?** Más adelante, podríamos necesitar hacer **Fine-Tuning** (entrenar el modelo con nuestros propios datos) si el nivel de precisión inicial no es suficiente.

## 5. Umbral de Confianza (Threshold) y "Otros"
La IA dará un porcentaje de "confianza" en su predicción. Por ejemplo: `[Alimentación: 95%]` o `[Compras: 30%, Entretenimiento: 28%]`.
- Cuando la IA tenga una confianza muy baja en la categoría elegida, **¿establecemos un puntaje mínimo (ejemplo: >40%) para asignarle una categoría principal?** 
- Si no supera la nota de corte, ¿la mandamos a `"Otros"` temporalmente y dejamos una opción en la App Android (Kotlin) para que el propio usuario *"corrija/enseñe"* a la aplicación, y nosotros retroalimentamos nuestros datos?

## 6. Sincronización en la Base de Datos
He notado en el SQL y en la alteración de la tabla `movimientos` que añadimos `categoria_id`.
- Cuando la IA clasifica el movimiento, **¿el script de Python modificará la base de datos de PostgreSQL directamente haciendo el `UPDATE`?** ¿O el script solo le devolverá el resultado JSON a nuestra API Node, para que el ORM/Controlador actual se encargue de guardar el ID de la categoría?
