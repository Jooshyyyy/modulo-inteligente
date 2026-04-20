# 🚀 Arquitectura Técnica del Módulo Inteligente de Predicción

Este documento explica de forma clara, general y técnica cómo funciona todo el flujo tecnológico para la predicción de gastos de nuestra aplicación; detallando el recorrido de los datos desde el núcleo de la Inteligencia Artificial hasta la interfaz final del teléfono móvil.

---

## 1. ¿Qué es y cómo funciona a grandes rasgos?
El Módulo Inteligente es una característica tecnológica avanzada cuyo único propósito es **predecir el futuro financiero inmediato del usuario**. Analiza su registro y deduce **qué** va a gastar hoy, a qué **hora** es probable que lo haga, de qué **categoría** será (Transporte, Comida, etc.) y **cuánto monto** le va a costar. 

El teléfono del usuario por sí solo no tiene toda la potencia requerida para analizar miles de datos en milisegundos, por ello el sistema se divide cuidadosamente en 3 bloques principales: **El Cerebro (IA), El Puente (API Node.js) y La Cara (Frontend Android).**

---

## 2. El "Cerebro" de la IA (Backend Analítico - Python)

La lógica predictiva es sumamente profunda y se ejecuta silenciosamente en un entorno de servidor aislado que utiliza Python. Hacemos uso de Modelos de Machine Learning (Aprendizaje Automático) pre-entrenados y representados en archivos computacionales (`modelo_categoria.pkl` y `modelo_monto.pkl`).

### 🕰️ Análisis de Tiempo Cíclico
La IA no lee "hoy es Lunes" o "son las 10 de la mañana". Utiliza transformaciones matemáticas trigonométricas (Senos y Cosenos) sobre los días de la semana, del año y horas del mes. Esto le permite a la máquina entender que un *Domingo en la noche* está lógicamente pegado a un *Lunes por la madrugada*, estableciendo verdaderas cercanías de tiempo en el comportamiento de los gastos.

### 📊 Doble Predicción Encadenada
Cuando la IA despierta para un día determinado, realiza dos pasos vitales:
1. **Predicción de Categoría Probable:** Evalúa las variables matemáticas cíclicas mencionadas para deducir qué categoría encaja perfecto hoy. (¿Día laborable? Quizás Transporte. ¿Fin de semana noche? Quizás Entretenimiento).
2. **Predicción de Monto Proyectado:** Sabiendo cuál podría ser el gasto, el segundo cerebro artificial predice directamente cuántos Bolivianos (Bs.) invertirá el usuario basándose puramente en su comportamiento previo.
3. Se genera un grado de acierto llamado **"Score de Confianza"** (por ejemplo, "estoy 92% seguro de que esto pasará hoy"). Todo esto termina insertado continuamente de manera automática en la base de datos robusta en Postgres (Tabla `predicciones_gastos`).

---

## 3. El "Puente" de Datos (API Intermediaria - Node.js)

Una vez que la IA guardó su informe para el día, éste necesita llegar finalmente a la app de manera eficiente, optimizada y sobre todo legal. Aquí entra nuestra **API en Node.js**.

- **Rutas Propias y Seguras:** Se creó en el backend la ruta especializada  `/api/predicciones/dia`. Antes de devolver cualquier número a quien lo pide por internet, el interceptor o middleware `verificarToken` se abalanza y constata de que seas el usuario autorizado a leer esa información usando tokens de cifrado.
- **Acceso y Unión de Tablas Directas:** A través del código `prediccion.model.js`, la API entra a la base de datos SQL e intercepta de forma optimizada la tabla de *predicciones* y le engancha (fusiona) la tabla estándar de *categorías* (usando un cruce tolerante a errores llamado `LEFT JOIN`). Así no sólo extrae un simple ID numérico de la fila, sino que saca a superficie exactamente la palabra "Alimentación" o "Vivienda", su monto exacto y el porcentaje de precisión.
- Posteriormente, `prediccion.controller.js` limpia la data, le da formato redondeado, establece si el nivel de gasto es "Alto" o "Moderado" según reglas programadas de cantidad y lo prepara todo en un paquete de datos liviano (`JSON`) para el viaje.

---

## 4. La Cara del Usuario (Frontend - App Android en Kotlin)

Finalmente, todo este pesado cálculo intelectual le es entregado a nuestro software de Android en fracciones minúsculas de segundo al momento de abrir la aplicación. 

- **Consumo Remoto (RetrofitClient / Models):** Construimos una especie de molde de información (Tanto para la petición de red como para las clases de Kotlin) ubicados en nuestros archivos `Network.kt` y `Models.kt`. A través de la librería **Retrofit** y procesos en segundo plano (Corrutinas `lifecycleScope.launch`), el teléfono jala el paquete de datos del backend discretamente.
- **Integración Visual Completa (`DashboardActivity`):** El código principal de la pantalla de "Mi Banca" obtiene los bolivianos y probabilidades devueltas por Node.js y la inyecta interactivamente a los elementos de nuestra interfaz construidos en `layout_prediccion_card.xml`.
- **Elegancia de Diseño e Inyección Inflada:** Utilizamos patrones bancarios nativos; diseños suavizados (Esquinas redondeadas o `Elevation` en CardViews), paletas de crema con etiquetas amigables indicando intervenciones de ⚡ IA. Si la IA pronosticó dos o tres tendencias distintas, la aplicación se encarga de autogenerar listados inflados de los sub-items ("Starbucks", "Uber") encajándolos unos por debajo de otros automáticamente a través de `item_probabilidad_ia.xml`.
- **Confiabilidad Absoluta sin Crashes:** Si por algún motivo el usuario llega a una fecha del futuro o del pasado donde nuestra tabla de bases de datos aún no generó cálculos de Inteligencias Artificial, el Backend levanta una bandera de advertencia 404, y nuestra aplicación Android está programada para absorber este vacío elegantemente, pintando la interfaz en *Bs. 0.00* y comunicando que se encuentra *"Sin predicciones para este día"*. 

De esta manera, un intrincado cálculo y modelo entrenado con miles de líneas de datos, recae en la experiencia cómoda, dinámica, hermosa y rápida dentro del bolsillo de nuestros usuarios.
