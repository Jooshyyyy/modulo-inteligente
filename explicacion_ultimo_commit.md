# 📦 Explicación del Último Commit
**Nombre:** `feat/frontend: Diseño interactivo y predictivo`  
**Fecha:** Domingo 19 de abril de 2026 — 23:22 hs  
**Archivos modificados:** 21 · **Líneas agregadas:** +1,403 · **Líneas eliminadas:** -496

---

## 🗂️ Resumen General

Este commit tuvo como objetivo transformar la forma en que la aplicación Android muestra las **predicciones de IA**. En lugar de listas construidas manualmente (inflando vistas una por una dentro de `LinearLayout`), se pasó a un sistema moderno basado en **RecyclerView + Adapters**. Además se enriqueció la interfaz con **semáforos de riesgo**, **filtros por categoría** y **animaciones de entrada**.

Los cambios se concentraron en **dos interfaces** del frontend y sus archivos de soporte en el backend.

---

## 🖥️ INTERFAZ 1 — `IAPrediccionActivity` (Pantalla de IA)

Esta fue la interfaz más modificada del commit. Es la pantalla dedicada a mostrar las predicciones diarias, semanales y mensuales generadas por el modelo de IA.

### 🔁 ¿Qué se hacía antes?

Antes, cada fila de predicción se creaba así:
```kotlin
val row = LayoutInflater.from(this)
    .inflate(R.layout.item_prediction_day, dayList, false)
row.findViewById<TextView>(R.id.tvDayLabel).text = "..."
dayList.addView(row)
```
Esto significa que se inflaban las vistas una a una y se agregaban directamente a un `LinearLayout` (`dayList`, `categoryList`). Es funcional pero ineficiente y difícil de mantener.

---

### ✅ ¿Qué se hizo ahora?

#### 1. Se reemplazaron los `LinearLayout` dinámicos por `RecyclerView`

Se eliminaron las referencias directas a `llMonthlyCategoryList` y `llMonthlyDayList` (que eran `LinearLayout`) y se conectaron **RecyclerView** con sus respectivos adapters:

```kotlin
// ANTES
val categoryList = findViewById<LinearLayout>(R.id.llMonthlyCategoryList)
val dayList = findViewById<LinearLayout>(R.id.llMonthlyDayList)

// AHORA — con adapters dedicados
categoryAdapter.submit(categorias, data.total, categoriaFiltro)
dayAdapter.submit(dias)
```

#### 2. Se creó un sistema de **filtro por categoría** en la vista mensual

Se implementó una función `renderDiasDelMes()` que filtra los días mostrados según la categoría que el usuario toque:

```kotlin
private fun renderDiasDelMes() {
    val dias = if (categoriaFiltro == null) diasMesCache
               else diasMesCache.filter { it.categoria == categoriaFiltro }
    dayAdapter.submit(dias)
    categoryAdapter.submit(categoriasMesCache, totalMesCache, categoriaFiltro)
}
```

👆 **Efecto:** El usuario puede tocar una categoría en la lista (ej. "Alimentación") y automáticamente los días se filtran para mostrar solo los de esa categoría. Al tocarla de nuevo, se limpia el filtro.

#### 3. Se agregó un **semáforo de riesgo** para la predicción diaria (Coach IA)

Se agregó lógica para el campo `tvCoachSemaforo` que compara el gasto proyectado contra la meta definida por el usuario:

```kotlin
val ratio = totalGastoProyectado / meta
tvSemaforo.text = when {
    ratio < 0.7  -> "Riesgo: Verde ✅ (dentro del presupuesto)"
    ratio < 1.0  -> "Riesgo: Amarillo ⚠️ (cerca del límite)"
    else         -> "Riesgo: Rojo 🔴 (superando la meta)"
}
```

Si no hay meta definida, muestra un mensaje neutral: `"Riesgo: — (definí una meta para comparar)"`.

#### 4. Se mejoró el texto del insight mensual

El texto que aparece debajo del gráfico de barras pasó de ser genérico a indicar el **día pico del mes**:

```
Antes:  "Día más alto: lun 7 (Alimentación) Bs. 120.00"
Ahora:  "Pico del mes: lun 7 · Alimentación · Bs. 120.00 (confianza 87%). Tocá un día para comparar."
```

#### 5. Se protegió contra valores `NaN`

Se añadieron validaciones para evitar que aparezca `NaN` en montos:
```kotlin
monthTotal.text = if (data.total.isNaN()) "Bs. 0.00" else "Bs. ${decimalFormat.format(data.total)}"
```

---

## 📊 INTERFAZ 2 — `DashboardActivity` (Panel Principal)

La pantalla de inicio también fue actualizada para adoptar el mismo patrón de adapters y añadir el semáforo de riesgo semanal.

### ✅ Cambios realizados

#### 1. Se eliminaron los `LinearLayout` dinámicos de la sección semanal

Igual que en `IAPrediccionActivity`, se reemplazaron `llWeeklyCategoryList` y `llWeeklyDayList` por adapters:

```kotlin
// ANTES — crear vistas manualmente
val itemView = LayoutInflater.from(this).inflate(R.layout.item_weekly_category, categoryList, false)
itemView.findViewById<TextView>(R.id.tvCategoryName).text = categoria.categoria
categoryList.addView(itemView)

// AHORA — con adapter
weeklyCategoryAdapter.submit(categorias, totalSemana, null)
renderWeeklyDays()
```

#### 2. Se añadió un **semáforo de riesgo semanal** (`tvWeeklyRisk`)

Se calculó qué porcentaje del gasto total está concentrado en **una sola categoría**:

| Semáforo | Condición | Color |
|---|---|---|
| 🟢 Verde | La categoría dominante tiene < 45% del gasto | `#7CFFB2` |
| 🟡 Amarillo | Entre 45% y 70% | `#FFD166` |
| 🔴 Rojo | Más del 70% concentrado en una categoría | `#FF6B6B` |

```kotlin
val topPct = categorias.maxOfOrNull { (it.monto / totalSemana) * 100 } ?: 0.0
when {
    topPct < 45 -> riskText.text = "Riesgo semanal: Verde (gasto diversificado)"
    topPct < 70 -> riskText.text = "Riesgo semanal: Amarillo (dependencia moderada)"
    else        -> riskText.text = "Riesgo semanal: Rojo (alta concentración en una categoría)"
}
```

#### 3. Se creó `renderWeeklyDays()` para filtrado semanal

Similar al mensual, permite filtrar los días de la semana por categoría:

```kotlin
private fun renderWeeklyDays() {
    val days = if (weeklyFiltroCategoria == null) weeklyDiasCache
               else weeklyDiasCache.filter { it.categoria == weeklyFiltroCategoria }
    weeklyDayAdapter.submit(days)
    weeklyCategoryAdapter.submit(weeklyCategoriasCache, weeklyTotalCache, weeklyFiltroCategoria)
}
```

#### 4. Se mejoró el semáforo de predicción diaria en el Dashboard

El campo `tvPrediccionSemaforo` también se actualizó para comparar con la meta del usuario, con los mismos colores que en `IAPrediccionActivity`.

---

## 🆕 NUEVOS ADAPTERS CREADOS (5 archivos)

Se crearon **5 adapters nuevos** en el paquete `ui/prediccion/`. Todos siguen el mismo patrón moderno de `RecyclerView.Adapter` con animaciones de entrada.

### 📌 `ProbabilidadAdapter`
**Propósito:** Muestra la lista de probabilidades por categoría en la predicción diaria.

Cada ítem muestra:
- Nombre de la categoría
- Hora estimada (`"Durante el día"`)
- Porcentaje de probabilidad
- Monto en Bolivianos

Con animación de entrada (fade + slide desde abajo):
```kotlin
holder.itemView.alpha = 0f
holder.itemView.translationY = 10f
holder.itemView.animate().alpha(1f).translationY(0f).setDuration(180)...
```

### 📌 `MonthlyCategoryAdapter`
**Propósito:** Lista de categorías del mes con soporte para **selección/filtrado**.

Cuando una categoría está seleccionada, su fondo cambia a un color púrpura oscuro para indicar que está activa como filtro:
```kotlin
vRoot.setBackgroundColor(
    if (isSelected) Color.parseColor("#332A55") else Color.TRANSPARENT
)
```

### 📌 `MonthlyDayAdapter`
**Propósito:** Lista de días del mes con soporte para **click en cada fila** para mostrar el detalle en el insight.

Cada ítem permite al usuario tocar un día específico para que aparezca información detallada debajo del gráfico.

### 📌 `CoachSuggestionAdapter`
**Propósito:** Muestra las sugerencias del Coach IA (tips personalizados basados en los patrones de gasto del usuario).

### 📌 `CoachIndicatorAdapter`
**Propósito:** Muestra indicadores numéricos del Coach IA (KPIs como gasto promedio, días activos, etc.) en formato de tarjetas.

---

## ⚙️ BACKEND — Cambios en el Servidor

### `prediccion.model.js` — Modelo de datos

**Problema anterior:** La consulta a la base de datos solo devolvía **1 fila** por día (la categoría con mayor confianza).

**Solución:** Ahora devuelve **hasta 12 filas** por día, una por cada categoría predicha:

```javascript
// ANTES
LIMIT 1
return result.rows[0];  // Un solo objeto

// AHORA
LIMIT 12
return result.rows;  // Array de categorías
```

También se cambió el criterio de ordenamiento: antes se priorizaba por `score_confianza`, ahora por **`monto_proyectado`** (el monto es más relevante visualmente).

Para las vistas **semanal y mensual**, se pasó de mostrar 1 categoría por día a hasta **8 categorías**:
```sql
-- ANTES
WHERE rn = 1

-- AHORA
WHERE rn <= 8
ORDER BY fecha_prediccion ASC, monto_proyectado DESC
```

---

### `prediccion.controller.js` — Controlador

El controlador se actualizó para manejar el array de filas que ahora retorna el modelo:

```javascript
// ANTES — una sola fila
const total = parseFloat(predDb.monto_proyectado);
const porcentajeConfianza = Math.floor(parseFloat(predDb.score_confianza) * 100);

// AHORA — suma de todas las filas
const total = filas.reduce((acc, r) => acc + Number(r.monto_proyectado || 0), 0);
```

También se genera dinámicamente la lista de probabilidades para cada categoría:
```javascript
const probabilidades = filas.map((r) => {
    const m = Number(r.monto_proyectado || 0);
    const pctDelDia = total > 0 ? Math.round((m / total) * 100) : 0;
    return {
        nombre: r.categoria_nombre || "Otros",
        hora: "Durante el día",
        porcentaje: pctDelDia,
        monto: m.toFixed(2)
    };
});
```

El mensaje descriptivo también cambió:
```
Antes: "IA personal · confianza ~87% · categoría dominante inferida"
Ahora: "IA personal · 3 rubro(s) · mayor peso: Alimentación"
```

---

### `main.py` — Script de IA (Python)

**Problema anterior:** El script generaba **una sola predicción por día** (la categoría con mayor probabilidad).

**Solución:** Ahora genera **una predicción por cada categoría** en ese día, distribuyendo el monto total entre todas ellas:

```python
# ANTES — una sola inferencia
categoria_id, monto, confianza = inferir_para_dia(modelo_categoria, modelo_monto, fecha)
upsert_prediccion(cur, args.usuario_id, categoria_id, fecha.date(), monto, confianza)

# AHORA — distribución múltiple
filas = inferir_distribucion_dia(modelo_categoria, modelo_monto, fecha)
borrar_predicciones_del_dia(cur, args.usuario_id, fecha.date())
for categoria_id, monto, confianza in filas:
    insertar_prediccion(cur, args.usuario_id, categoria_id, fecha.date(), monto, confianza)
```

El log también mejoró para mostrar el resumen de todas las categorías generadas:
```
[OK] usuario=5 fecha=2026-04-20 filas=3 | cat=1 m=120.5 conf=0.87, cat=3 m=45.0 conf=0.72, cat=2 m=30.0 conf=0.65
```

---

## 🔄 Flujo Completo Actualizado

```
Script Python (main.py)
    ↓  Genera N predicciones por día (una por categoría)
    ↓
Base de Datos PostgreSQL (predicciones_gastos)
    ↓
Backend Node.js (prediccion.model.js)
    ↓  Retorna array de hasta 12 filas por día
    ↓
Backend Node.js (prediccion.controller.js)
    ↓  Suma totales y construye lista de probabilidades
    ↓
Android — DashboardActivity
    ↓  Muestra predicción diaria + semáforo + riesgo semanal
    ↓
Android — IAPrediccionActivity
    ↓  Muestra predicciones diarias, semanales, mensuales
    ↓  Filtro por categoría + semáforo coach IA
```

---

## 📋 Tabla de Archivos Modificados

| Archivo | Tipo | Cambio Principal |
|---|---|---|
| `IAPrediccionActivity.kt` | 🟡 Modificado | Adapters + filtro mensual + semáforo coach |
| `DashboardActivity.kt` | 🟡 Modificado | Adapters + semáforo semanal + filtro semanal |
| `activity_ia_prediccion.xml` | 🟡 Modificado | Rediseño completo del layout |
| `layout_prediccion_card.xml` | 🟡 Modificado | Card de predicción diaria rediseñada |
| `layout_weekly_expense_card.xml` | 🟡 Modificado | Card semanal rediseñada |
| `CoachIndicatorAdapter.kt` | 🟢 Nuevo | Adapter para KPIs del coach |
| `CoachSuggestionAdapter.kt` | 🟢 Nuevo | Adapter para sugerencias del coach |
| `MonthlyCategoryAdapter.kt` | 🟢 Nuevo | Adapter con filtrado por categoría |
| `MonthlyDayAdapter.kt` | 🟢 Nuevo | Adapter de días con click |
| `ProbabilidadAdapter.kt` | 🟢 Nuevo | Adapter de probabilidades diarias |
| `item_ia_coach_indicator.xml` | 🟢 Nuevo | Layout del ítem indicador |
| `item_ia_coach_suggestion.xml` | 🟡 Modificado | Layout del ítem sugerencia |
| `item_prediction_day.xml` | 🟡 Modificado | Layout del ítem de día |
| `item_probabilidad_ia.xml` | 🟡 Modificado | Layout del ítem de probabilidad |
| `item_weekly_category.xml` | 🟡 Modificado | Layout del ítem de categoría |
| `prediccion.controller.js` | 🟡 Modificado | Manejo de múltiples filas |
| `prediccion.model.js` | 🟡 Modificado | Query devuelve hasta 12 filas |
| `main.py` | 🟡 Modificado | Genera N predicciones por día |
| `meta.controller.js` | 🟡 Modificado | Ajustes en controlador de metas |
| `Models.kt` | 🟡 Modificado | Modelos de datos actualizados |

---

> **Conclusión:** Este commit representa un salto de calidad significativo en la arquitectura del frontend Android. Se pasó de un sistema de vistas manuales e ineficientes a un patrón moderno con `RecyclerView`, y el backend ahora proporciona datos mucho más ricos (múltiples categorías por día) en lugar de una sola predicción plana.
