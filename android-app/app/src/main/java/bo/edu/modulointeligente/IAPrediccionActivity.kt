package bo.edu.modulointeligente

import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.drawerlayout.widget.DrawerLayout
import androidx.lifecycle.lifecycleScope
import com.google.android.material.button.MaterialButton
import com.google.android.material.chip.ChipGroup
import com.google.android.material.navigation.NavigationView
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import kotlinx.coroutines.launch
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

class IAPrediccionActivity : BaseActivity() {

    private val monthCalendar = Calendar.getInstance()
    private val monthDisplayFormat = SimpleDateFormat("MMMM yyyy", Locale("es", "ES"))
    private val monthApiFormat = SimpleDateFormat("yyyy-MM", Locale.getDefault())
    private val dayLabelFormat = SimpleDateFormat("EEE dd/MM", Locale("es", "ES"))
    private val decimalFormat = DecimalFormat("#,##0.00", DecimalFormatSymbols(Locale("es", "ES")))
    private val apiDateFormats = listOf(
        "yyyy-MM-dd",
        "yyyy-MM-dd'T'HH:mm:ss.SSSX",
        "yyyy-MM-dd'T'HH:mm:ssX",
        "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
        "yyyy-MM-dd'T'HH:mm:ss'Z'"
    ).map { SimpleDateFormat(it, Locale.getDefault()) }

    private var metaIdActual: Int? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_ia_prediccion)

        RetrofitClient.authToken = SessionManager(this).fetchAuthToken()

        val drawerLayout = findViewById<DrawerLayout>(R.id.drawer_layout)
        val navView = findViewById<NavigationView>(R.id.nav_view)
        setupDrawer(drawerLayout, navView)

        findViewById<ChipGroup>(R.id.chipGroupPlantilla).check(R.id.chipVacaciones)
        aplicarPlantillaChipDesdeChip()

        findViewById<TextInputEditText>(R.id.etMetaFecha).setText(fechaDefaultLimite())

        findViewById<ChipGroup>(R.id.chipGroupPlantilla).setOnCheckedStateChangeListener { _, checkedIds ->
            if (checkedIds.isEmpty()) {
                findViewById<ChipGroup>(R.id.chipGroupPlantilla).check(R.id.chipVacaciones)
                return@setOnCheckedStateChangeListener
            }
            aplicarPlantillaChipDesdeChip()
        }

        findViewById<MaterialButton>(R.id.btnGuardarMeta).setOnClickListener { guardarMeta() }
        findViewById<MaterialButton>(R.id.btnPausarMeta).setOnClickListener { pausarMetaActual() }
        findViewById<MaterialButton>(R.id.btnActualizarProgreso).setOnClickListener { actualizarProgresoMeta() }

        findViewById<Button>(R.id.btnPrevMonth).setOnClickListener {
            monthCalendar.add(Calendar.MONTH, -1)
            refrescarPantalla()
        }
        findViewById<Button>(R.id.btnNextMonth).setOnClickListener {
            monthCalendar.add(Calendar.MONTH, 1)
            refrescarPantalla()
        }

        refrescarPantalla()
    }

    private fun refrescarPantalla() {
        cargarPrediccionMensual()
        cargarIaCoach()
    }

    private fun fechaDefaultLimite(): String {
        val c = Calendar.getInstance()
        c.add(Calendar.MONTH, 6)
        return SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(c.time)
    }

    private fun plantillaDesdeChip(): String? = when (findViewById<ChipGroup>(R.id.chipGroupPlantilla).checkedChipId) {
        R.id.chipVacaciones -> "vacaciones"
        R.id.chipEquipo -> "equipo"
        R.id.chipViaje -> "viaje"
        R.id.chipEmergencia -> "emergencia"
        R.id.chipOtro -> "otro"
        else -> null
    }

    private fun aplicarPlantillaChipDesdeChip() {
        val chipId = findViewById<ChipGroup>(R.id.chipGroupPlantilla).checkedChipId
        val titulo = when (chipId) {
            R.id.chipVacaciones -> "Vacaciones"
            R.id.chipEquipo -> "Equipo o tecnología"
            R.id.chipViaje -> "Viaje"
            R.id.chipEmergencia -> "Fondo de emergencia"
            else -> return
        }
        if (chipId != R.id.chipOtro) {
            findViewById<TextInputEditText>(R.id.etMetaTitulo).setText(titulo)
        }
    }

    private fun guardarMeta() {
        val titulo = findViewById<TextInputEditText>(R.id.etMetaTitulo).text?.toString()?.trim().orEmpty()
        val montoStr = findViewById<TextInputEditText>(R.id.etMetaMonto).text?.toString()?.trim().orEmpty()
        val fecha = findViewById<TextInputEditText>(R.id.etMetaFecha).text?.toString()?.trim().orEmpty()
        val monto = montoStr.replace(",", ".").toDoubleOrNull()

        if (titulo.isEmpty() || monto == null || fecha.length < 8) {
            Toast.makeText(this, "Completa nombre, monto y fecha de la meta.", Toast.LENGTH_SHORT).show()
            return
        }

        lifecycleScope.launch {
            try {
                val req = CrearMetaRequest(
                    titulo = titulo,
                    montoObjetivo = monto,
                    fechaLimite = fecha,
                    plantilla = plantillaDesdeChip(),
                    descripcion = null
                )
                val res = RetrofitClient.instance.crearMeta(req)
                if (res.isSuccessful && res.body() != null) {
                    Toast.makeText(this@IAPrediccionActivity, res.body()!!.mensaje, Toast.LENGTH_SHORT).show()
                    cargarIaCoach()
                } else {
                    Toast.makeText(this@IAPrediccionActivity, "No se pudo guardar la meta.", Toast.LENGTH_SHORT).show()
                }
            } catch (_: Exception) {
                Toast.makeText(this@IAPrediccionActivity, "Error de conexión al guardar meta.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun pausarMetaActual() {
        val id = metaIdActual ?: return
        lifecycleScope.launch {
            try {
                val res = RetrofitClient.instance.pausarMeta(id)
                if (res.isSuccessful) {
                    Toast.makeText(this@IAPrediccionActivity, "Meta pausada.", Toast.LENGTH_SHORT).show()
                    metaIdActual = null
                    cargarIaCoach()
                } else {
                    Toast.makeText(this@IAPrediccionActivity, "No se pudo pausar.", Toast.LENGTH_SHORT).show()
                }
            } catch (_: Exception) {
                Toast.makeText(this@IAPrediccionActivity, "Error de conexión.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun actualizarProgresoMeta() {
        val id = metaIdActual ?: return
        val acStr = findViewById<TextInputEditText>(R.id.etMetaAcumulado).text?.toString()?.trim().orEmpty()
        val ac = acStr.replace(",", ".").toDoubleOrNull()
        if (ac == null || ac < 0) {
            Toast.makeText(this, "Indica un monto acumulado válido.", Toast.LENGTH_SHORT).show()
            return
        }
        lifecycleScope.launch {
            try {
                val res = RetrofitClient.instance.actualizarProgresoMeta(
                    id,
                    ActualizarMetaProgresoRequest(montoAcumulado = ac)
                )
                if (res.isSuccessful && res.body() != null) {
                    Toast.makeText(this@IAPrediccionActivity, res.body()!!.mensaje, Toast.LENGTH_SHORT).show()
                    cargarIaCoach()
                } else {
                    Toast.makeText(this@IAPrediccionActivity, "No se pudo actualizar el progreso.", Toast.LENGTH_SHORT).show()
                }
            } catch (_: Exception) {
                Toast.makeText(this@IAPrediccionActivity, "Error de conexión.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun cargarIaCoach() {
        val mesApi = monthApiFormat.format(monthCalendar.time)
        val tvCoachNarrativa = findViewById<TextView>(R.id.tvCoachNarrativa)
        val llInd = findViewById<LinearLayout>(R.id.llCoachIndicadores)
        val llSug = findViewById<LinearLayout>(R.id.llCoachSugerencias)
        val pb = findViewById<ProgressBar>(R.id.pbMetaProgreso)
        val tvMetaResumen = findViewById<TextView>(R.id.tvMetaResumen)
        val tilAc = findViewById<TextInputLayout>(R.id.tilMetaAcumulado)
        val btnPausar = findViewById<MaterialButton>(R.id.btnPausarMeta)
        val btnProg = findViewById<MaterialButton>(R.id.btnActualizarProgreso)
        val btnGuardar = findViewById<MaterialButton>(R.id.btnGuardarMeta)

        lifecycleScope.launch {
            try {
                val response = RetrofitClient.instance.getIaCoach(mesApi)
                if (!response.isSuccessful || response.body() == null) {
                    tvCoachNarrativa.text = "No se pudo cargar el coach de IA."
                    llInd.removeAllViews()
                    llSug.removeAllViews()
                    return@launch
                }

                val data = response.body()!!
                tvCoachNarrativa.text = data.narrativa

                llInd.removeAllViews()
                data.indicadores.forEach { ind ->
                    val tv = TextView(this@IAPrediccionActivity)
                    tv.text = "${ind.etiqueta}: ${ind.valor}\n${ind.detalle}"
                    tv.setTextColor(Color.WHITE)
                    tv.textSize = 12f
                    tv.setPadding(0, 6, 0, 6)
                    llInd.addView(tv)
                }

                llSug.removeAllViews()
                if (data.sugerencias.isEmpty()) {
                    val empty = TextView(this@IAPrediccionActivity)
                    empty.text =
                        if (data.diasConPrediccion == 0) {
                            "Sin predicciones en este mes: genera datos con el job de IA para ver recortes con porcentaje hacia tu meta."
                        } else {
                            "Ajusta tu meta o el mes; el modelo aún no tiene suficiente variación para sugerencias."
                        }
                    empty.setTextColor(Color.WHITE)
                    empty.alpha = 0.9f
                    empty.textSize = 12f
                    llSug.addView(empty)
                } else {
                    data.sugerencias.forEach { s ->
                        val row = LayoutInflater.from(this@IAPrediccionActivity)
                            .inflate(R.layout.item_ia_coach_suggestion, llSug, false)
                        row.findViewById<TextView>(R.id.tvSuggestionTitle).text = s.titulo
                        row.findViewById<TextView>(R.id.tvSuggestionBody).text = s.mensaje
                        val badge = row.findViewById<TextView>(R.id.tvSuggestionBadge)
                        val pct = s.porcentajeAcercamientoMeta
                        badge.text = "+${decimalFormat.format(pct)}% meta"
                        llSug.addView(row)
                    }
                }

                val meta = data.meta
                if (data.tieneMeta && meta != null) {
                    metaIdActual = meta.id
                    pb.isVisible = true
                    pb.progress = meta.porcentajeCompletado.roundToInt().coerceIn(0, 100)
                    tvMetaResumen.isVisible = true
                    tvMetaResumen.typeface = Typeface.DEFAULT_BOLD
                    val pc = if (meta.porcentajeCompletado.isNaN()) 0.0 else meta.porcentajeCompletado
                    val mo = if (meta.montoObjetivo.isNaN()) 0.0 else meta.montoObjetivo
                    val ma = if (meta.montoAcumulado.isNaN()) 0.0 else meta.montoAcumulado
                    val mr = if (meta.montoRestante.isNaN()) 0.0 else meta.montoRestante
                    tvMetaResumen.text =
                        "Meta: ${meta.titulo} · Bs. ${decimalFormat.format(ma)} / ${decimalFormat.format(mo)} " +
                            "(${decimalFormat.format(pc)}%) · faltan Bs. ${decimalFormat.format(mr)}"
                    tilAc.isVisible = true
                    findViewById<TextInputEditText>(R.id.etMetaAcumulado).setText(decimalFormat.format(meta.montoAcumulado))
                    btnPausar.isVisible = true
                    btnProg.isVisible = true
                    btnGuardar.text = "Reemplazar meta"
                } else {
                    metaIdActual = null
                    pb.isVisible = false
                    tvMetaResumen.isVisible = false
                    tilAc.isVisible = false
                    btnPausar.isVisible = false
                    btnProg.isVisible = false
                    btnGuardar.text = "Activar meta"
                }
            } catch (_: Exception) {
                tvCoachNarrativa.text = "Error al cargar el coach de IA."
                llInd.removeAllViews()
                llSug.removeAllViews()
            }
        }
    }

    private fun cargarPrediccionMensual() {
        val monthTitle = findViewById<TextView>(R.id.tvMonthTitle)
        val monthTotal = findViewById<TextView>(R.id.tvMonthTotal)
        val insight = findViewById<TextView>(R.id.tvSelectedDayInsight)
        val chartBar = findViewById<LinearLayout>(R.id.llMonthlyChartBar)
        val categoryList = findViewById<LinearLayout>(R.id.llMonthlyCategoryList)
        val dayList = findViewById<LinearLayout>(R.id.llMonthlyDayList)

        val mesApi = monthApiFormat.format(monthCalendar.time)
        monthTitle.text = monthDisplayFormat.format(monthCalendar.time).replaceFirstChar { it.uppercase() }

        lifecycleScope.launch {
            try {
                val response = RetrofitClient.instance.getPrediccionMensual(mesApi)
                if (!response.isSuccessful || response.body() == null) {
                    monthTotal.text = "Bs. 0.00"
                    insight.text = "Sin predicciones disponibles para este mes."
                    chartBar.removeAllViews()
                    categoryList.removeAllViews()
                    dayList.removeAllViews()
                    return@launch
                }

                val data = response.body()!!
                val categorias = data.categorias.filter { it.monto > 0.0 }
                val dias = data.dias.sortedBy { it.fecha }
                monthTotal.text = "Bs. ${decimalFormat.format(data.total)}"
                chartBar.removeAllViews()
                categoryList.removeAllViews()
                dayList.removeAllViews()

                categorias.forEach { categoria ->
                    if (categoria.monto.isNaN()) return@forEach
                    val itemView = LayoutInflater.from(this@IAPrediccionActivity)
                        .inflate(R.layout.item_weekly_category, categoryList, false)
                    val percent = if (data.total > 0.0 && !data.total.isNaN()) ((categoria.monto / data.total) * 100).roundToInt() else 0
                    itemView.findViewById<TextView>(R.id.tvCategoryName).text = categoria.categoria
                    itemView.findViewById<TextView>(R.id.tvCategoryAmount).text = "Bs. ${decimalFormat.format(categoria.monto)}"
                    itemView.findViewById<TextView>(R.id.tvCategoryPercent).text = "$percent%"
                    itemView.findViewById<View>(R.id.viewCategoryColor).setBackgroundColor(parseCategoryColor(categoria.colorHex))
                    categoryList.addView(itemView)

                    val segment = View(this@IAPrediccionActivity)
                    val safeWeight = if (percent <= 0) 0.5f else percent.toFloat()
                    segment.layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, safeWeight)
                    segment.setBackgroundColor(parseCategoryColor(categoria.colorHex))
                    chartBar.addView(segment)
                }

                if (dias.isEmpty()) {
                    insight.text = "Sin detalle diario en el mes seleccionado."
                } else {
                    val topDay = dias.maxByOrNull { it.monto }!!
                    insight.text =
                        "Día más alto: ${formatDayLabel(topDay.fecha)} (${topDay.categoria}) Bs. ${decimalFormat.format(topDay.monto)}"
                }

                dias.forEach { dia ->
                    val row = LayoutInflater.from(this@IAPrediccionActivity)
                        .inflate(R.layout.item_prediction_day, dayList, false)
                    row.findViewById<TextView>(R.id.tvDayLabel).text = formatDayLabel(dia.fecha)
                    row.findViewById<TextView>(R.id.tvDayCategory).text = dia.categoria
                    row.findViewById<TextView>(R.id.tvDayAmount).text = "Bs. ${decimalFormat.format(dia.monto)}"
                    row.findViewById<View>(R.id.viewDayColor).setBackgroundColor(parseCategoryColor(dia.colorHex))
                    row.setOnClickListener {
                        insight.text =
                            "${formatDayLabel(dia.fecha)}: ${dia.categoria} - Bs. ${decimalFormat.format(dia.monto)} " +
                                "(confianza ${(dia.confianza * 100).roundToInt()}%)"
                    }
                    dayList.addView(row)
                }
            } catch (_: Exception) {
                monthTotal.text = "Bs. 0.00"
                insight.text = "No se pudo cargar la predicción mensual."
                chartBar.removeAllViews()
                categoryList.removeAllViews()
                dayList.removeAllViews()
            }
        }
    }

    private fun formatDayLabel(rawDate: String): String {
        val date = parseApiDate(rawDate) ?: return rawDate
        return dayLabelFormat.format(date).replaceFirstChar { it.uppercase() }
    }

    private fun parseApiDate(rawDate: String): Date? {
        for (format in apiDateFormats) {
            try {
                return format.parse(rawDate)
            } catch (_: ParseException) {
            }
        }
        return null
    }

    private fun parseCategoryColor(hex: String?): Int {
        return try {
            Color.parseColor(hex ?: "#9E9E9E")
        } catch (_: IllegalArgumentException) {
            Color.parseColor("#9E9E9E")
        }
    }
}
