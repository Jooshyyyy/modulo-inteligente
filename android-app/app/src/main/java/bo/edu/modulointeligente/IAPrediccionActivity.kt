package bo.edu.modulointeligente

import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.drawerlayout.widget.DrawerLayout
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.google.android.material.chip.ChipGroup
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.navigation.NavigationView
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import bo.edu.modulointeligente.ui.prediccion.CoachIndicatorAdapter
import bo.edu.modulointeligente.ui.prediccion.CoachSuggestionAdapter
import bo.edu.modulointeligente.ui.prediccion.MonthlyCategoryAdapter
import bo.edu.modulointeligente.ui.prediccion.MonthlyDayAdapter
import bo.edu.modulointeligente.ui.prediccion.PresupuestoCategoriaAdapter
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.charts.RadarChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.github.mikephil.charting.data.RadarData
import com.github.mikephil.charting.data.RadarDataSet
import com.github.mikephil.charting.data.RadarEntry
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.github.mikephil.charting.formatter.ValueFormatter
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

    private var planIdActual: Int? = null
    private var diasMesCache: List<PrediccionDetalleDia> = emptyList()
    private var categoriasMesCache: List<PrediccionSemanalCategoria> = emptyList()
    private var totalMesCache: Double = 0.0
    private var categoriaFiltro: String? = null
    private var semanaFiltro: Int? = null
    private var textoInsightPredeterminado: String = ""
    private var planActual: PlanPresupuesto? = null
    private var estadoPresupuestoActual: EstadoPresupuestoCoach? = null
    private var gastoProyectadoMesActual: Double? = null

    private lateinit var indicatorAdapter: CoachIndicatorAdapter
    private lateinit var suggestionAdapter: CoachSuggestionAdapter
    private lateinit var categoryAdapter: MonthlyCategoryAdapter
    private lateinit var dayAdapter: MonthlyDayAdapter
    private lateinit var presupuestoCategoriaAdapter: PresupuestoCategoriaAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_ia_prediccion)

        RetrofitClient.authToken = SessionManager(this).fetchAuthToken()

        val drawerLayout = findViewById<DrawerLayout>(R.id.drawer_layout)
        val navView = findViewById<NavigationView>(R.id.nav_view)
        setupDrawer(drawerLayout, navView)

        findViewById<ChipGroup>(R.id.chipGroupTipoPlan).check(R.id.chipPresupuestoGeneral)
        actualizarUiTipoPlan()

        findViewById<ChipGroup>(R.id.chipGroupTipoPlan).setOnCheckedStateChangeListener { _, checkedIds ->
            if (checkedIds.isEmpty()) {
                findViewById<ChipGroup>(R.id.chipGroupTipoPlan).check(R.id.chipPresupuestoGeneral)
                return@setOnCheckedStateChangeListener
            }
            actualizarUiTipoPlan()
        }

        findViewById<MaterialButton>(R.id.btnGuardarMeta).setOnClickListener { guardarPlanPresupuesto() }
        findViewById<MaterialButton>(R.id.btnPausarMeta).setOnClickListener { pausarPlanActual() }

        findViewById<Button>(R.id.btnPrevMonth).setOnClickListener {
            monthCalendar.add(Calendar.MONTH, -1)
            refrescarPantalla()
        }
        findViewById<Button>(R.id.btnNextMonth).setOnClickListener {
            monthCalendar.add(Calendar.MONTH, 1)
            refrescarPantalla()
        }

        setupRecyclerUis()
        cargarCategoriasParaLimites()
        refrescarPantalla()
    }

    private fun setupRecyclerUis() {
        findViewById<ChipGroup>(R.id.chipGroupSemanas)?.setOnCheckedStateChangeListener { _, checkedIds ->
            semanaFiltro = when (checkedIds.firstOrNull()) {
                R.id.chipSemana1 -> 1
                R.id.chipSemana2 -> 2
                R.id.chipSemana3 -> 3
                R.id.chipSemana4 -> 4
                else -> null
            }
            renderDiasDelMes()
        }

        indicatorAdapter = CoachIndicatorAdapter()
        suggestionAdapter = CoachSuggestionAdapter(decimalFormat) { s ->
            MaterialAlertDialogBuilder(this)
                .setTitle(s.titulo)
                .setMessage(s.mensaje)
                .setPositiveButton("Entendido", null)
                .show()
        }

        findViewById<RecyclerView>(R.id.rvCoachIndicadores).apply {
            layoutManager = LinearLayoutManager(this@IAPrediccionActivity)
            adapter = indicatorAdapter
            itemAnimator = null
        }
        findViewById<RecyclerView>(R.id.rvCoachSugerencias).apply {
            layoutManager = LinearLayoutManager(this@IAPrediccionActivity)
            adapter = suggestionAdapter
            itemAnimator = null
        }

        categoryAdapter = MonthlyCategoryAdapter(decimalFormat, ::parseCategoryColor) { cat ->
            categoriaFiltro = if (categoriaFiltro == cat) null else cat
            renderDiasDelMes()
        }
        dayAdapter = MonthlyDayAdapter(decimalFormat, ::parseCategoryColor, ::formatDayLabel) { dia ->
            mostrarConsejoRapidoDia(dia)
        }

        findViewById<RecyclerView>(R.id.rvMonthlyCategoryList).apply {
            layoutManager = LinearLayoutManager(this@IAPrediccionActivity)
            adapter = categoryAdapter
            itemAnimator = null
        }
        findViewById<RecyclerView>(R.id.rvMonthlyDayList).apply {
            layoutManager = LinearLayoutManager(this@IAPrediccionActivity)
            adapter = dayAdapter
            itemAnimator = null
        }

        presupuestoCategoriaAdapter = PresupuestoCategoriaAdapter()
        findViewById<RecyclerView>(R.id.rvLimitesCategoria).apply {
            layoutManager = LinearLayoutManager(this@IAPrediccionActivity)
            adapter = presupuestoCategoriaAdapter
            itemAnimator = null
        }
    }

    private fun esPlanGeneral(): Boolean =
        findViewById<ChipGroup>(R.id.chipGroupTipoPlan).checkedChipId == R.id.chipPresupuestoGeneral

    private fun actualizarUiTipoPlan() {
        val general = esPlanGeneral()
        findViewById<TextInputLayout>(R.id.tilMetaMonto).isVisible = general
        findViewById<TextView>(R.id.tvHintCategorias).isVisible = !general
        findViewById<RecyclerView>(R.id.rvLimitesCategoria).isVisible = !general
    }

    private fun cargarCategoriasParaLimites(preseleccion: Map<Int, Double> = emptyMap()) {
        lifecycleScope.launch {
            try {
                val res = RetrofitClient.instance.getCategorias()
                if (res.isSuccessful && res.body() != null) {
                    presupuestoCategoriaAdapter.submit(res.body()!!, preseleccion)
                }
            } catch (_: Exception) {
            }
        }
    }

    private fun mostrarConsejoRapidoDia(dia: PrediccionDetalleDia) {
        val fecha = formatDayLabel(dia.fecha)
        val base = "Día: $fecha\nCategoría dominante: ${dia.categoria}\nGasto proyectado: Bs. ${decimalFormat.format(dia.monto)}\n"
        val plan = planActual
        val estado = estadoPresupuestoActual

        val categoriaLower = dia.categoria.lowercase()
        val esNoMitigable = categoriaLower.contains("vivienda") || categoriaLower.contains("servicio") || categoriaLower.contains("educación") || categoriaLower.contains("educacion")

        val extra =
            if (esNoMitigable) {
                "\nConsejo: Estos gastos son fijos o esenciales y no se sugiere reducirlos. ¡Enfocate en otros picos!"
            } else if (plan != null && estado != null && estado.tope > 0) {
                val ahorro = (dia.monto * 0.2).coerceAtLeast(0.0)
                val ref = if (estado.exceso > 0) estado.exceso else estado.tope
                val impacto = if (ref > 0) (ahorro / ref) * 100 else 0.0
                val sugerenciaEspecifica = if (categoriaLower.contains("alimentación") || categoriaLower.contains("alimentacion")) {
                    " (ej. cociná en casa)"
                } else if (categoriaLower.contains("entretenimiento")) {
                    " (ej. buscá planes gratis o más baratos)"
                } else {
                    ""
                }
                val contexto = if (estado.exceso > 0) "hacia volver a tu presupuesto" else "de margen de ahorro"
                "\nConsejo: si reducís un 20% ese día$sugerenciaEspecifica, liberarías Bs. ${decimalFormat.format(ahorro)} (~${decimalFormat.format(impacto)}% $contexto)."
            } else {
                "\nConsejo: activá un presupuesto (tope general o por categoría) para medir el impacto del recorte."
            }

        MaterialAlertDialogBuilder(this)
            .setTitle("Consejo de ahorro")
            .setMessage(base + extra)
            .setPositiveButton("Ok", null)
            .show()
    }

    private fun refrescarPantalla() {
        cargarPlanActivo()
        cargarPrediccionMensual()
        cargarIaCoach()
    }

    private fun cargarPlanActivo() {
        lifecycleScope.launch {
            try {
                val res = RetrofitClient.instance.getMetaActiva()
                if (res.isSuccessful && res.body()?.tieneMeta == true) {
                    aplicarPlanEnFormulario(res.body()!!.plan ?: res.body()!!.meta)
                }
            } catch (_: Exception) {
            }
        }
    }

    private fun guardarPlanPresupuesto() {
        val titulo = findViewById<TextInputEditText>(R.id.etMetaTitulo).text?.toString()?.trim()
            .orEmpty().ifEmpty { "Mi plan de ahorro" }
        val general = esPlanGeneral()

        lifecycleScope.launch {
            try {
                val req = if (general) {
                    val montoStr = findViewById<TextInputEditText>(R.id.etMetaMonto).text?.toString()?.trim().orEmpty()
                    val monto = montoStr.replace(",", ".").toDoubleOrNull()
                    if (monto == null || monto <= 0) {
                        Toast.makeText(this@IAPrediccionActivity, "Indica un tope mensual válido.", Toast.LENGTH_SHORT).show()
                        return@launch
                    }
                    CrearMetaRequest(titulo = titulo, tipo = "GENERAL", topeMensual = monto)
                } else {
                    val limites = presupuestoCategoriaAdapter.limitesSeleccionados()
                    if (limites.isEmpty()) {
                        Toast.makeText(
                            this@IAPrediccionActivity,
                            "Seleccioná al menos una categoría con tope.",
                            Toast.LENGTH_SHORT
                        ).show()
                        return@launch
                    }
                    CrearMetaRequest(
                        titulo = titulo,
                        tipo = "CATEGORIAS",
                        limites = limites.map { (id, tope) ->
                            LimiteCategoriaItem(categoriaId = id, topeMensual = tope)
                        }
                    )
                }
                val res = RetrofitClient.instance.crearMeta(req)
                if (res.isSuccessful && res.body() != null) {
                    Toast.makeText(this@IAPrediccionActivity, res.body()!!.mensaje, Toast.LENGTH_SHORT).show()
                    aplicarPlanEnFormulario(res.body()!!.plan ?: res.body()!!.meta)
                    cargarIaCoach()
                } else {
                    Toast.makeText(this@IAPrediccionActivity, "No se pudo guardar el plan.", Toast.LENGTH_SHORT).show()
                }
            } catch (_: Exception) {
                Toast.makeText(this@IAPrediccionActivity, "Error de conexión.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun aplicarPlanEnFormulario(plan: PlanPresupuesto?) {
        if (plan == null) return
        planIdActual = plan.id
        planActual = plan
        findViewById<TextInputEditText>(R.id.etMetaTitulo).setText(plan.titulo)
        if (plan.tipo == "GENERAL") {
            findViewById<ChipGroup>(R.id.chipGroupTipoPlan).check(R.id.chipPresupuestoGeneral)
            plan.topeMensual?.let {
                findViewById<TextInputEditText>(R.id.etMetaMonto).setText(
                    if (it % 1.0 == 0.0) it.toLong().toString() else it.toString()
                )
            }
        } else {
            findViewById<ChipGroup>(R.id.chipGroupTipoPlan).check(R.id.chipPresupuestoCategorias)
            val map = plan.limites.associate { it.categoriaId to it.topeMensual }
            cargarCategoriasParaLimites(map)
        }
        actualizarUiTipoPlan()
    }

    private fun pausarPlanActual() {
        val id = planIdActual ?: return
        lifecycleScope.launch {
            try {
                val res = RetrofitClient.instance.pausarMeta(id)
                if (res.isSuccessful) {
                    Toast.makeText(this@IAPrediccionActivity, "Plan quitado.", Toast.LENGTH_SHORT).show()
                    planIdActual = null
                    planActual = null
                    cargarIaCoach()
                } else {
                    Toast.makeText(this@IAPrediccionActivity, "No se pudo quitar el plan.", Toast.LENGTH_SHORT).show()
                }
            } catch (_: Exception) {
                Toast.makeText(this@IAPrediccionActivity, "Error de conexión.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun cargarIaCoach() {
        val mesApi = monthApiFormat.format(monthCalendar.time)
        val tvCoachNarrativa = findViewById<TextView>(R.id.tvCoachNarrativa)
        val tvKpiGasto = findViewById<TextView>(R.id.tvCoachKpiGasto)
        val tvKpiDias = findViewById<TextView>(R.id.tvCoachKpiDias)
        val tvSemaforo = findViewById<TextView>(R.id.tvCoachSemaforo)
        val pb = findViewById<ProgressBar>(R.id.pbMetaProgreso)
        val tvMetaResumen = findViewById<TextView>(R.id.tvMetaResumen)
        val btnPausar = findViewById<MaterialButton>(R.id.btnPausarMeta)
        val btnGuardar = findViewById<MaterialButton>(R.id.btnGuardarMeta)

        lifecycleScope.launch {
            try {
                val response = RetrofitClient.instance.getIaCoach(mesApi)
                if (!response.isSuccessful || response.body() == null) {
                    tvCoachNarrativa.text = "No se pudo cargar el coach de IA."
                    tvKpiGasto.text = "—"
                    tvKpiDias.text = "—"
                    tvSemaforo.text = "Riesgo: —"
                    indicatorAdapter.submit(emptyList())
                    suggestionAdapter.submit(emptyList())
                    return@launch
                }

                val data = response.body()!!
                val plan = data.plan ?: data.meta
                planActual = plan
                estadoPresupuestoActual = data.estadoPresupuesto
                gastoProyectadoMesActual = data.gastoProyectadoMes
                tvCoachNarrativa.text = data.narrativa
                val gp = data.gastoProyectadoMes
                tvKpiGasto.text =
                    if (gp.isNaN()) "—" else "Bs. ${decimalFormat.format(gp)}"
                tvKpiDias.text = data.diasConPrediccion.toString()

                indicatorAdapter.submit(data.indicadores)

                if (data.sugerencias.isEmpty()) {
                    suggestionAdapter.submit(emptyList())
                } else {
                    suggestionAdapter.submit(data.sugerencias.sortedBy { it.prioridad }.take(5))
                }

                val estado = data.estadoPresupuesto
                if (data.tieneMeta && plan != null) {
                    planIdActual = plan.id
                    aplicarPlanEnFormulario(plan)
                    pb.isVisible = estado != null
                    val uso = estado?.usoPct ?: 0.0
                    pb.progress = uso.roundToInt().coerceIn(0, 100)
                    tvMetaResumen.isVisible = true
                    tvMetaResumen.typeface = Typeface.DEFAULT_BOLD
                    if (estado != null) {
                        val tipoTxt = if (plan.tipo == "GENERAL") "Tope general" else "${plan.limites.size} categoría(s)"
                        val margenTxt = if (estado.exceso > 0) {
                            "exceso Bs. ${decimalFormat.format(estado.exceso)}"
                        } else {
                            "ahorro proyectado Bs. ${decimalFormat.format(estado.ahorroProyectado)}"
                        }
                        tvMetaResumen.text =
                            "${plan.titulo} · $tipoTxt Bs. ${decimalFormat.format(estado.tope)} · $margenTxt · ${decimalFormat.format(estado.usoPct)}% usado"
                    } else {
                        tvMetaResumen.text = plan.titulo
                    }
                    btnPausar.isVisible = true
                    btnGuardar.text = "Actualizar presupuesto"
                } else {
                    planIdActual = null
                    planActual = null
                    pb.isVisible = false
                    tvMetaResumen.isVisible = false
                    btnPausar.isVisible = false
                    btnGuardar.text = "Activar presupuesto"
                }

                if (estado != null && estado.tope > 0) {
                    when {
                        estado.usoPct <= 75 -> {
                            tvSemaforo.text = "Presupuesto: Verde (margen de ahorro)"
                            tvSemaforo.setTextColor(Color.parseColor("#7CFFB2"))
                        }
                        estado.usoPct <= 100 -> {
                            tvSemaforo.text = "Presupuesto: Amarillo (cerca del tope)"
                            tvSemaforo.setTextColor(Color.parseColor("#FFD166"))
                        }
                        else -> {
                            tvSemaforo.text = "Presupuesto: Rojo (exceso proyectado)"
                            tvSemaforo.setTextColor(Color.parseColor("#FF6B6B"))
                        }
                    }
                } else {
                    tvSemaforo.text = "Presupuesto: — (activá un tope para comparar)"
                    tvSemaforo.setTextColor(Color.parseColor("#D0D0E0"))
                }
            } catch (_: Exception) {
                tvCoachNarrativa.text = "Error al cargar el coach de IA."
                tvKpiGasto.text = "—"
                tvKpiDias.text = "—"
                findViewById<TextView>(R.id.tvCoachSemaforo).text = "Riesgo: —"
                indicatorAdapter.submit(emptyList())
                suggestionAdapter.submit(emptyList())
            }
        }
    }

    private fun cargarPrediccionMensual() {
        val monthTitle = findViewById<TextView>(R.id.tvMonthTitle)
        val monthTotal = findViewById<TextView>(R.id.tvMonthTotal)
        val insight = findViewById<TextView>(R.id.tvSelectedDayInsight)
        val pieChartCategorias = findViewById<PieChart>(R.id.pieChartCategorias)

        val mesApi = monthApiFormat.format(monthCalendar.time)
        monthTitle.text = monthDisplayFormat.format(monthCalendar.time).replaceFirstChar { it.uppercase() }

        lifecycleScope.launch {
            try {
                val response = RetrofitClient.instance.getPrediccionMensual(mesApi)
                if (!response.isSuccessful || response.body() == null) {
                    monthTotal.text = "Bs. 0.00"
                    insight.text = "Sin predicciones disponibles para este mes."
                    diasMesCache = emptyList()
                    categoriaFiltro = null
                    semanaFiltro = null
                    findViewById<ChipGroup>(R.id.chipGroupSemanas)?.clearCheck()
                    textoInsightPredeterminado = insight.text.toString()
                    pieChartCategorias.clear()
                    categoryAdapter.submit(emptyList(), 0.0, null)
                    dayAdapter.submit(emptyList())
                    return@launch
                }

                val data = response.body()!!
                val categorias = data.categorias.filter { it.monto > 0.0 }
                val dias = data.dias.sortedBy { it.fecha }
                diasMesCache = dias
                categoriasMesCache = categorias
                totalMesCache = data.total
                categoriaFiltro = null
                semanaFiltro = null
                findViewById<ChipGroup>(R.id.chipGroupSemanas)?.clearCheck()
                monthTotal.text = if (data.total.isNaN()) "Bs. 0.00" else "Bs. ${decimalFormat.format(data.total)}"
                
                val pieEntries = ArrayList<PieEntry>()
                val colors = ArrayList<Int>()

                categorias.forEach { categoria ->
                    if (!categoria.monto.isNaN() && categoria.monto > 0) {
                        pieEntries.add(PieEntry(categoria.monto.toFloat(), categoria.categoria))
                        colors.add(parseCategoryColor(categoria.colorHex))
                    }
                }

                if (pieEntries.isNotEmpty()) {
                    val dataSet = PieDataSet(pieEntries, "")
                    dataSet.colors = colors
                    dataSet.setDrawValues(false)
                    dataSet.sliceSpace = 3f
                    dataSet.selectionShift = 5f

                    val pieData = PieData(dataSet)
                    pieChartCategorias.data = pieData
                    pieChartCategorias.description.isEnabled = false
                    pieChartCategorias.legend.isEnabled = false
                    pieChartCategorias.isDrawHoleEnabled = true
                    pieChartCategorias.holeRadius = 58f
                    pieChartCategorias.transparentCircleRadius = 61f
                    pieChartCategorias.setHoleColor(android.graphics.Color.TRANSPARENT)
                    pieChartCategorias.setDrawEntryLabels(false)
                    pieChartCategorias.animateY(1000)
                    pieChartCategorias.invalidate()
                } else {
                    pieChartCategorias.clear()
                }

                textoInsightPredeterminado = if (dias.isEmpty()) {
                    "Sin detalle diario en el mes seleccionado."
                } else {
                    val topDay = dias.maxByOrNull { it.monto }!!
                    "Pico del mes: ${formatDayLabel(topDay.fecha)} · ${topDay.categoria} · Bs. ${decimalFormat.format(topDay.monto)} " +
                        "(confianza ${(topDay.confianza * 100).roundToInt()}%). Tocá un día para comparar."
                }
                categoryAdapter.submit(categorias, data.total, categoriaFiltro)
                renderDiasDelMes()
            } catch (_: Exception) {
                monthTotal.text = "Bs. 0.00"
                insight.text = "No se pudo cargar la predicción mensual."
                diasMesCache = emptyList()
                categoriasMesCache = emptyList()
                totalMesCache = 0.0
                categoriaFiltro = null
                semanaFiltro = null
                findViewById<ChipGroup>(R.id.chipGroupSemanas)?.clearCheck()
                textoInsightPredeterminado = insight.text.toString()
                pieChartCategorias.clear()
                findViewById<RadarChart>(R.id.radarChartCategorias).clear()
                findViewById<BarChart>(R.id.barChartSemanas).clear()
                categoryAdapter.submit(emptyList(), 0.0, null)
                dayAdapter.submit(emptyList())
            }
        }
    }

    private fun renderDiasDelMes() {
        val insight = findViewById<TextView>(R.id.tvSelectedDayInsight)
        var dias = diasMesCache
        
        if (categoriaFiltro != null) {
            dias = dias.filter { it.categoria == categoriaFiltro }
        }
        
        if (semanaFiltro != null) {
            dias = dias.filter { 
                val date = parseApiDate(it.fecha)
                if (date != null) {
                    val cal = Calendar.getInstance()
                    cal.time = date
                    val weekOfMonth = cal.get(Calendar.WEEK_OF_MONTH)
                    if (semanaFiltro == 4) weekOfMonth >= 4 else weekOfMonth == semanaFiltro
                } else true
            }
        }

        if (categoriaFiltro != null || semanaFiltro != null) {
            val sub = dias.sumOf { d -> if (d.monto.isNaN()) 0.0 else d.monto }
            val filtroCatText = if (categoriaFiltro != null) "Categoría: $categoriaFiltro" else ""
            val filtroSemText = if (semanaFiltro != null) "Semana: $semanaFiltro" else ""
            val filtros = listOf(filtroCatText, filtroSemText).filter { it.isNotEmpty() }.joinToString(" · ")
            insight.text = "Filtrando: $filtros · subtotal visible Bs. ${decimalFormat.format(sub)}. Tocá para limpiar."
        } else {
            insight.text = textoInsightPredeterminado
        }
        val topDias = dias.sortedByDescending { it.monto }
        dayAdapter.submit(topDias)
        categoryAdapter.submit(categoriasMesCache, totalMesCache, categoriaFiltro)

        val lineChartDias = findViewById<LineChart>(R.id.lineChartDias)
        val entries = ArrayList<Entry>()
        val labels = ArrayList<String>()

        dias.forEachIndexed { index, dia ->
            if (!dia.monto.isNaN()) {
                entries.add(Entry(index.toFloat(), dia.monto.toFloat()))
                val date = parseApiDate(dia.fecha)
                val dayStr = if (date != null) SimpleDateFormat("dd/MM", Locale("es", "ES")).format(date) else ""
                labels.add(dayStr)
            }
        }

        if (entries.isNotEmpty()) {
            val dataSet = LineDataSet(entries, "Gasto Diario")
            dataSet.color = android.graphics.Color.parseColor("#6E56FF")
            dataSet.valueTextColor = android.graphics.Color.WHITE
            dataSet.lineWidth = 2f
            dataSet.circleRadius = 4f
            dataSet.setCircleColor(android.graphics.Color.parseColor("#6E56FF"))
            dataSet.setDrawValues(false)
            dataSet.mode = LineDataSet.Mode.CUBIC_BEZIER
            dataSet.setDrawFilled(true)
            dataSet.fillColor = android.graphics.Color.parseColor("#6E56FF")
            dataSet.fillAlpha = 50

            val lineData = LineData(dataSet)
            lineChartDias.data = lineData
            lineChartDias.description.isEnabled = false
            lineChartDias.legend.isEnabled = false
            
            lineChartDias.xAxis.apply {
                position = XAxis.XAxisPosition.BOTTOM
                textColor = android.graphics.Color.parseColor("#A0A0A0")
                setDrawGridLines(false)
                granularity = 1f
                valueFormatter = object : ValueFormatter() {
                    override fun getFormattedValue(value: Float): String {
                        val index = value.toInt()
                        return if (index >= 0 && index < labels.size) labels[index] else ""
                    }
                }
            }

            lineChartDias.axisLeft.apply {
                textColor = android.graphics.Color.parseColor("#A0A0A0")
                setDrawGridLines(true)
                gridColor = android.graphics.Color.parseColor("#2A2A40")
                axisMinimum = 0f
            }
            lineChartDias.axisRight.isEnabled = false
            lineChartDias.animateX(500)
            lineChartDias.invalidate()
        } else {
            lineChartDias.clear()
        }

        // RadarChart Categorias
        val radarChart = findViewById<RadarChart>(R.id.radarChartCategorias)
        val radarEntries = ArrayList<RadarEntry>()
        val radarLabels = ArrayList<String>()
        categoriasMesCache.forEach { cat ->
            if (cat.monto > 0 && !cat.monto.isNaN()) {
                radarEntries.add(RadarEntry(cat.monto.toFloat()))
                radarLabels.add(cat.categoria)
            }
        }
        if (radarEntries.isNotEmpty()) {
            val radarDataSet = RadarDataSet(radarEntries, "Categorías")
            radarDataSet.color = android.graphics.Color.parseColor("#6E56FF")
            radarDataSet.fillColor = android.graphics.Color.parseColor("#6E56FF")
            radarDataSet.setDrawFilled(true)
            radarDataSet.fillAlpha = 180
            radarDataSet.lineWidth = 2f
            val radarData = RadarData(radarDataSet)
            radarChart.data = radarData
            radarChart.xAxis.valueFormatter = IndexAxisValueFormatter(radarLabels)
            radarChart.xAxis.textColor = android.graphics.Color.parseColor("#A0A0A0")
            radarChart.xAxis.textSize = 10f
            radarChart.yAxis.setDrawLabels(false)
            radarChart.yAxis.axisMinimum = 0f
            radarChart.legend.isEnabled = false
            radarChart.description.isEnabled = false
            radarChart.animateY(1000)
            radarChart.invalidate()
        } else {
            radarChart.clear()
        }

        // BarChart Semanas
        val barChart = findViewById<BarChart>(R.id.barChartSemanas)
        val weekTotals = FloatArray(4)
        diasMesCache.forEach { dia ->
            val date = parseApiDate(dia.fecha)
            if (date != null && !dia.monto.isNaN()) {
                val cal = Calendar.getInstance()
                cal.time = date
                val w = cal.get(Calendar.WEEK_OF_MONTH) - 1
                if (w in 0..3) {
                    weekTotals[w] += dia.monto.toFloat()
                } else if (w > 3) {
                    weekTotals[3] += dia.monto.toFloat()
                }
            }
        }
        val barEntries = ArrayList<BarEntry>()
        weekTotals.forEachIndexed { i, total ->
            barEntries.add(BarEntry(i.toFloat(), total))
        }
        if (barEntries.any { it.y > 0 }) {
            val barDataSet = BarDataSet(barEntries, "Gasto Semanal")
            barDataSet.color = android.graphics.Color.parseColor("#7CFFB2")
            barDataSet.valueTextColor = android.graphics.Color.WHITE
            barDataSet.valueTextSize = 10f
            val barData = BarData(barDataSet)
            barChart.data = barData
            barChart.xAxis.position = XAxis.XAxisPosition.BOTTOM
            barChart.xAxis.textColor = android.graphics.Color.parseColor("#A0A0A0")
            barChart.xAxis.valueFormatter = IndexAxisValueFormatter(listOf("Sem 1", "Sem 2", "Sem 3", "Sem 4+"))
            barChart.xAxis.setDrawGridLines(false)
            barChart.axisLeft.textColor = android.graphics.Color.parseColor("#A0A0A0")
            barChart.axisLeft.axisMinimum = 0f
            barChart.axisRight.isEnabled = false
            barChart.legend.isEnabled = false
            barChart.description.isEnabled = false
            barChart.animateY(1000)
            barChart.invalidate()
        } else {
            barChart.clear()
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
