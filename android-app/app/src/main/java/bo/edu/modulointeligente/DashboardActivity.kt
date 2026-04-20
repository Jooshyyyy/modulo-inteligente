package bo.edu.modulointeligente

import android.os.Bundle
import android.graphics.Color
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.drawerlayout.widget.DrawerLayout
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.navigation.NavigationView
import bo.edu.modulointeligente.ui.prediccion.MonthlyCategoryAdapter
import bo.edu.modulointeligente.ui.prediccion.MonthlyDayAdapter
import bo.edu.modulointeligente.ui.prediccion.ProbabilidadAdapter
import kotlinx.coroutines.launch
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

class DashboardActivity : BaseActivity() {

    private lateinit var adapter: CuentaAdapter
    private val calendar = Calendar.getInstance()
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    private val displayFormat = SimpleDateFormat("EEEE, d 'de' MMMM", Locale("es", "ES"))
    private val rangeFormat = SimpleDateFormat("dd/MM", Locale("es", "ES"))
    private val dayLabelFormat = SimpleDateFormat("EEE dd/MM", Locale("es", "ES"))
    private val decimalFormat = DecimalFormat("#,##0.00", DecimalFormatSymbols(Locale("es", "ES")))
    private val apiDateFormats = listOf(
        "yyyy-MM-dd",
        "yyyy-MM-dd'T'HH:mm:ss.SSSX",
        "yyyy-MM-dd'T'HH:mm:ssX"
    ).map { SimpleDateFormat(it, Locale.getDefault()) }
    private lateinit var probabilidadAdapter: ProbabilidadAdapter
    private lateinit var weeklyCategoryAdapter: MonthlyCategoryAdapter
    private lateinit var weeklyDayAdapter: MonthlyDayAdapter
    private var weeklyCategoriasCache: List<PrediccionSemanalCategoria> = emptyList()
    private var weeklyDiasCache: List<PrediccionDetalleDia> = emptyList()
    private var weeklyTotalCache: Double = 0.0
    private var weeklyFiltroCategoria: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dashboard)

        // Initialize Retrofit Token
        val sessionManager = SessionManager(this)
        RetrofitClient.authToken = sessionManager.fetchAuthToken()

        // Setup Drawer
        val drawerLayout = findViewById<DrawerLayout>(R.id.drawer_layout)
        val navView = findViewById<NavigationView>(R.id.nav_view)
        setupDrawer(drawerLayout, navView)

        val rvCuentas = findViewById<RecyclerView>(R.id.rvCuentas)
        rvCuentas.layoutManager = LinearLayoutManager(this)
        adapter = CuentaAdapter(emptyList())
        rvCuentas.adapter = adapter
        setupPredictionAdapters()

        findViewById<com.google.android.material.floatingactionbutton.FloatingActionButton>(R.id.fabRefresh).setOnClickListener {
            cargarCuentas()
            cargarPrediccion()
            cargarResumenSemanal()
            Toast.makeText(this, "Actualizando...", Toast.LENGTH_SHORT).show()
        }

        findViewById<Button>(R.id.btnCambiarFalsaFecha).setOnClickListener {
            calendar.add(Calendar.DAY_OF_YEAR, 1)
            cargarPrediccion()
            Toast.makeText(this, "Avanzando al siguiente día: " + displayFormat.format(calendar.time), Toast.LENGTH_SHORT).show()
        }

        cargarCuentas()
        cargarPrediccion()
        cargarResumenSemanal()
    }

    private fun setupPredictionAdapters() {
        probabilidadAdapter = ProbabilidadAdapter { item ->
            val monto = item.monto.toDoubleOrNull() ?: 0.0
            val sugerido = (monto * 0.3).coerceAtLeast(0.0)
            MaterialAlertDialogBuilder(this)
                .setTitle("Consejo IA · ${item.nombre}")
                .setMessage(
                    "Proyección: Bs. ${decimalFormat.format(monto)}\n" +
                        "Si reducís ese gasto en un 30%, podrías ahorrar Bs. ${decimalFormat.format(sugerido)}.\n" +
                        "Acción sugerida: definí un tope diario para ${item.nombre.lowercase()}."
                )
                .setPositiveButton("Aplicar idea", null)
                .show()
        }
        findViewById<RecyclerView>(R.id.rvProbabilidadesList).apply {
            layoutManager = LinearLayoutManager(this@DashboardActivity)
            adapter = probabilidadAdapter
            itemAnimator = null
        }

        weeklyCategoryAdapter = MonthlyCategoryAdapter(decimalFormat, ::parseCategoryColor) { cat ->
            weeklyFiltroCategoria = if (weeklyFiltroCategoria == cat) null else cat
            renderWeeklyDays()
        }
        findViewById<RecyclerView>(R.id.rvWeeklyCategoryList).apply {
            layoutManager = LinearLayoutManager(this@DashboardActivity)
            adapter = weeklyCategoryAdapter
            itemAnimator = null
        }

        weeklyDayAdapter = MonthlyDayAdapter(decimalFormat, ::parseCategoryColor, ::formatDayLabel) { dia ->
            val ahorro = dia.monto * 0.2
            MaterialAlertDialogBuilder(this@DashboardActivity)
                .setTitle("Alerta de ahorro · ${formatDayLabel(dia.fecha)}")
                .setMessage(
                    "Gasto estimado en ${dia.categoria}: Bs. ${decimalFormat.format(dia.monto)}.\n" +
                        "Si ajustás un 20%, ahorrarías aprox. Bs. ${decimalFormat.format(ahorro)}."
                )
                .setPositiveButton("Entendido", null)
                .show()
        }
        findViewById<RecyclerView>(R.id.rvWeeklyDayList).apply {
            layoutManager = LinearLayoutManager(this@DashboardActivity)
            adapter = weeklyDayAdapter
            itemAnimator = null
        }
    }

    override fun onResume() {
        super.onResume()
        cargarCuentas()
        cargarResumenSemanal()
        // Opcional: cargarPrediccion() tmb se podria para refrescar, pero dejemos que no cambie la fecha simulada
    }

    private fun cargarCuentas() {
        lifecycleScope.launch {
            try {
                val response = RetrofitClient.instance.getCuentas()
                if (response.isSuccessful) {
                    adapter.updateData(response.body() ?: emptyList())
                } else {
                    Toast.makeText(this@DashboardActivity, "Error al cargar cuentas", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@DashboardActivity, "Error de conexión", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun cargarPrediccion() {
        val fechaApi = dateFormat.format(calendar.time)
        val fechaMostrar = displayFormat.format(calendar.time).replaceFirstChar { it.uppercase() }
        
        findViewById<TextView>(R.id.tvPrediccionFecha).text = fechaMostrar
        
        lifecycleScope.launch {
            try {
                val response = RetrofitClient.instance.getPrediccionDia(fechaApi)
                if (response.isSuccessful) {
                    response.body()?.let { pred ->
                        findViewById<TextView>(R.id.tvPrediccionMonto).text = "Bs. ${pred.total}"
                        findViewById<TextView>(R.id.tvPrediccionNivel).text = pred.nivel
                        findViewById<TextView>(R.id.tvPrediccionDiferencia).text = pred.diferenciaPrevia
                        probabilidadAdapter.submit(pred.probabilidades)
                        val monto = pred.total.toDoubleOrNull() ?: 0.0
                        val tvSem = findViewById<TextView>(R.id.tvPrediccionSemaforo)
                        when {
                            monto < 80 -> {
                                tvSem.text = "Alerta IA: Verde (gasto diario bajo)"
                                tvSem.setTextColor(Color.parseColor("#7CFFB2"))
                            }
                            monto < 180 -> {
                                tvSem.text = "Alerta IA: Amarillo (vigilar gasto)"
                                tvSem.setTextColor(Color.parseColor("#FFD166"))
                            }
                            else -> {
                                tvSem.text = "Alerta IA: Rojo (riesgo de sobrepaso)"
                                tvSem.setTextColor(Color.parseColor("#FF6B6B"))
                            }
                        }
                    }
                } else {
                    // Si no es exitoso (ej. 404), limpiamos la vista
                    findViewById<TextView>(R.id.tvPrediccionMonto).text = "Bs. 0.00"
                    findViewById<TextView>(R.id.tvPrediccionNivel).text = "Sin predicción"
                    findViewById<TextView>(R.id.tvPrediccionDiferencia).text = "No hay datos IA para este día"
                    findViewById<TextView>(R.id.tvPrediccionSemaforo).text = "Alerta IA: —"
                    probabilidadAdapter.submit(emptyList())
                }
            } catch (e: Exception) {
                // Silencioso para caso de error de api
                e.printStackTrace()
            }
        }
    }

    private fun cargarResumenSemanal() {
        val subtitle = findViewById<TextView>(R.id.tvWeeklyExpenseSubtitle)
        val totalText = findViewById<TextView>(R.id.tvWeeklyExpenseTotal)
        val riskText = findViewById<TextView>(R.id.tvWeeklyRisk)
        val emptyText = findViewById<TextView>(R.id.tvWeeklyExpenseEmpty)
        val chartBar = findViewById<LinearLayout>(R.id.llWeeklyChartBar)

        val startDate = Calendar.getInstance().apply {
            set(Calendar.DAY_OF_WEEK, firstDayOfWeek)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val endDate = (startDate.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, 6) }
        val fechaInicio = dateFormat.format(startDate.time)
        subtitle.text = "Semana ${rangeFormat.format(startDate.time)} - ${rangeFormat.format(endDate.time)}"

        lifecycleScope.launch {
            try {
                val response = RetrofitClient.instance.getPrediccionSemanal(fechaInicio)
                if (!response.isSuccessful || response.body() == null) {
                    totalText.text = "Bs. 0.00"
                    chartBar.removeAllViews()
                    riskText.text = "Riesgo semanal: —"
                    weeklyCategoriasCache = emptyList()
                    weeklyDiasCache = emptyList()
                    weeklyTotalCache = 0.0
                    weeklyFiltroCategoria = null
                    weeklyCategoryAdapter.submit(emptyList(), 0.0, null)
                    weeklyDayAdapter.submit(emptyList())
                    emptyText.visibility = View.VISIBLE
                    return@launch
                }

                val predSemanal = response.body()!!
                val categorias = predSemanal.categorias
                    .filter { it.monto > 0.0 }
                    .sortedByDescending { it.monto }

                if (categorias.isEmpty()) {
                    totalText.text = "Bs. 0.00"
                    chartBar.removeAllViews()
                    riskText.text = "Riesgo semanal: —"
                    weeklyCategoriasCache = emptyList()
                    weeklyDiasCache = emptyList()
                    weeklyTotalCache = 0.0
                    weeklyFiltroCategoria = null
                    weeklyCategoryAdapter.submit(emptyList(), 0.0, null)
                    weeklyDayAdapter.submit(emptyList())
                    emptyText.visibility = View.VISIBLE
                    return@launch
                }

                val totalSemana = categorias.sumOf { it.monto }
                totalText.text = "Bs. ${decimalFormat.format(totalSemana)}"
                weeklyCategoriasCache = categorias
                weeklyDiasCache = predSemanal.dias
                weeklyTotalCache = totalSemana
                weeklyFiltroCategoria = null
                emptyText.visibility = View.GONE
                chartBar.removeAllViews()
                weeklyCategoryAdapter.submit(categorias, totalSemana, null)
                renderWeeklyDays()

                categorias.forEach { categoria ->
                    val percent = if (totalSemana > 0.0) ((categoria.monto / totalSemana) * 100).roundToInt() else 0
                    val segment = View(this@DashboardActivity)
                    val safeWeight = if (percent <= 0) 0.5f else percent.toFloat()
                    segment.layoutParams = LinearLayout.LayoutParams(
                        0,
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        safeWeight
                    )
                    segment.setBackgroundColor(parseCategoryColor(categoria.colorHex))
                    chartBar.addView(segment)
                }

                val topPct = categorias.maxOfOrNull { if (totalSemana > 0) (it.monto / totalSemana) * 100 else 0.0 } ?: 0.0
                when {
                    topPct < 45 -> {
                        riskText.text = "Riesgo semanal: Verde (gasto diversificado)"
                        riskText.setTextColor(Color.parseColor("#7CFFB2"))
                    }
                    topPct < 70 -> {
                        riskText.text = "Riesgo semanal: Amarillo (dependencia moderada)"
                        riskText.setTextColor(Color.parseColor("#FFD166"))
                    }
                    else -> {
                        riskText.text = "Riesgo semanal: Rojo (alta concentración en una categoría)"
                        riskText.setTextColor(Color.parseColor("#FF6B6B"))
                    }
                }
            } catch (e: Exception) {
                totalText.text = "Bs. 0.00"
                chartBar.removeAllViews()
                riskText.text = "Riesgo semanal: —"
                weeklyCategoriasCache = emptyList()
                weeklyDiasCache = emptyList()
                weeklyTotalCache = 0.0
                weeklyFiltroCategoria = null
                weeklyCategoryAdapter.submit(emptyList(), 0.0, null)
                weeklyDayAdapter.submit(emptyList())
                emptyText.visibility = View.VISIBLE
            }
        }
    }

    private fun renderWeeklyDays() {
        val days =
            if (weeklyFiltroCategoria == null) weeklyDiasCache
            else weeklyDiasCache.filter { it.categoria == weeklyFiltroCategoria }
        weeklyDayAdapter.submit(days)
        weeklyCategoryAdapter.submit(weeklyCategoriasCache, weeklyTotalCache, weeklyFiltroCategoria)
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