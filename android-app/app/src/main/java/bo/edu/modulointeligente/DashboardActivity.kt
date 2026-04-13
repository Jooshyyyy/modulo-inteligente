package bo.edu.modulointeligente

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.drawerlayout.widget.DrawerLayout
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.navigation.NavigationView
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
    private val rangeFormat = SimpleDateFormat("d MMM", Locale("es", "ES"))
    private val decimalFormat = DecimalFormat("#,##0.00", DecimalFormatSymbols(Locale("es", "ES")))

    private val apiDateFormats = listOf(
        "yyyy-MM-dd'T'HH:mm:ss.SSSX",
        "yyyy-MM-dd'T'HH:mm:ssX",
        "yyyy-MM-dd HH:mm:ss",
        "yyyy-MM-dd"
    ).map { pattern -> SimpleDateFormat(pattern, Locale.getDefault()) }

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
                        
                        val llLista = findViewById<LinearLayout>(R.id.llProbabilidadesList)
                        llLista.removeAllViews()
                        
                        pred.probabilidades.forEach { item ->
                            val view = LayoutInflater.from(this@DashboardActivity)
                                .inflate(R.layout.item_probabilidad_ia, llLista, false)
                            
                            view.findViewById<TextView>(R.id.tvProbItemNombre).text = item.nombre
                            view.findViewById<TextView>(R.id.tvProbItemDetalle).text = "${item.hora} • ${item.porcentaje}% probabilidad"
                            view.findViewById<TextView>(R.id.tvProbItemMonto).text = "Bs. ${item.monto}"
                            
                            llLista.addView(view)
                        }
                    }
                } else {
                    // Si no es exitoso (ej. 404), limpiamos la vista
                    findViewById<TextView>(R.id.tvPrediccionMonto).text = "Bs. 0.00"
                    findViewById<TextView>(R.id.tvPrediccionNivel).text = "Sin predicción"
                    findViewById<TextView>(R.id.tvPrediccionDiferencia).text = "No hay datos IA para este día"
                    findViewById<LinearLayout>(R.id.llProbabilidadesList).removeAllViews()
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
        val emptyText = findViewById<TextView>(R.id.tvWeeklyExpenseEmpty)
        val categoryList = findViewById<LinearLayout>(R.id.llWeeklyCategoryList)

        val endDate = Calendar.getInstance()
        val startDate = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -6) }
        subtitle.text = "${rangeFormat.format(startDate.time)} - ${rangeFormat.format(endDate.time)}"

        lifecycleScope.launch {
            try {
                val cuentasResponse = RetrofitClient.instance.getCuentas()
                if (!cuentasResponse.isSuccessful) {
                    return@launch
                }

                val movimientosSemana = mutableListOf<MovimientoResponse>()
                val allCuentas = cuentasResponse.body().orEmpty()
                allCuentas.forEach { cuenta ->
                    val movResponse = RetrofitClient.instance.getMovimientos(cuenta.id)
                    if (movResponse.isSuccessful) {
                        movimientosSemana.addAll(
                            movResponse.body().orEmpty().filter { movimiento ->
                                movimiento.tipo.equals("EGRESO", ignoreCase = true) &&
                                    isWithinLastWeek(movimiento.fecha)
                            }
                        )
                    }
                }

                if (movimientosSemana.isEmpty()) {
                    totalText.text = "Bs. 0.00"
                    categoryList.removeAllViews()
                    emptyText.visibility = View.VISIBLE
                    return@launch
                }

                val totalsByCategory = movimientosSemana
                    .groupBy { resolveCategoryName(it) }
                    .mapValues { (_, movimientos) -> movimientos.sumOf { it.monto } }
                    .toList()
                    .sortedByDescending { it.second }

                val totalSemana = totalsByCategory.sumOf { it.second }
                totalText.text = "Bs. ${decimalFormat.format(totalSemana)}"
                emptyText.visibility = View.GONE
                categoryList.removeAllViews()

                totalsByCategory.forEach { (categoria, monto) ->
                    val percent = if (totalSemana > 0.0) ((monto / totalSemana) * 100).roundToInt() else 0
                    val itemView = LayoutInflater.from(this@DashboardActivity)
                        .inflate(R.layout.item_weekly_category, categoryList, false)

                    itemView.findViewById<TextView>(R.id.tvCategoryName).text = categoria
                    itemView.findViewById<TextView>(R.id.tvCategoryAmount).text = "Bs. ${decimalFormat.format(monto)}"
                    itemView.findViewById<TextView>(R.id.tvCategoryPercent).text = "$percent%"
                    itemView.findViewById<ProgressBar>(R.id.pbCategoryShare).progress = percent

                    categoryList.addView(itemView)
                }
            } catch (e: Exception) {
                // Si la API falla, mantenemos el dashboard estable
                totalText.text = "Bs. 0.00"
                categoryList.removeAllViews()
                emptyText.visibility = View.VISIBLE
            }
        }
    }

    private fun isWithinLastWeek(rawDate: String): Boolean {
        val parsedDate = parseMovementDate(rawDate) ?: return false
        val start = Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, -6)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.time
        return !parsedDate.before(start)
    }

    private fun parseMovementDate(rawDate: String): Date? {
        for (formatter in apiDateFormats) {
            try {
                return formatter.parse(rawDate)
            } catch (_: ParseException) {
                // Intentamos el siguiente formato soportado por backend
            }
        }
        return null
    }

    private fun resolveCategoryName(movimiento: MovimientoResponse): String {
        val fromApi = movimiento.categoriaNombre?.trim()
        if (!fromApi.isNullOrEmpty()) return fromApi

        return when (movimiento.categoriaId) {
            1 -> "Alimentación"
            2 -> "Transporte"
            3 -> "Vivienda"
            4 -> "Entretenimiento"
            5 -> "Salud"
            6 -> "Educación"
            7 -> "Compras"
            8 -> "Servicios"
            9 -> "Otros"
            else -> inferCategoryByConcept(movimiento.concepto)
        }
    }

    private fun inferCategoryByConcept(concepto: String): String {
        val value = concepto.lowercase(Locale.getDefault())
        return when {
            listOf("uber", "taxi", "bus", "pasaje", "trufi", "transporte", "combustible").any { it in value } -> "Transporte"
            listOf("cafe", "almuerzo", "desayuno", "cena", "restaurant", "comida", "mercado").any { it in value } -> "Alimentación"
            listOf("farmacia", "clinica", "medico", "salud").any { it in value } -> "Salud"
            listOf("colegio", "universidad", "curso", "educacion").any { it in value } -> "Educación"
            listOf("luz", "agua", "internet", "alquiler", "servicio").any { it in value } -> "Servicios"
            listOf("tienda", "compra", "super", "ropa").any { it in value } -> "Compras"
            listOf("cine", "netflix", "spotify", "juego", "fiesta").any { it in value } -> "Entretenimiento"
            else -> "Otros"
        }
    }
}