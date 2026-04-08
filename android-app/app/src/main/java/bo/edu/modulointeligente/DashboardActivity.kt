package bo.edu.modulointeligente

import android.os.Bundle
import android.widget.Toast
import android.widget.TextView
import android.widget.Button
import android.widget.LinearLayout
import android.view.LayoutInflater
import androidx.drawerlayout.widget.DrawerLayout
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.navigation.NavigationView
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class DashboardActivity : BaseActivity() {
    
    private lateinit var adapter: CuentaAdapter
    private val calendar = Calendar.getInstance()
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    private val displayFormat = SimpleDateFormat("EEEE, d 'de' MMMM", Locale("es", "ES"))

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
            Toast.makeText(this, "Actualizando...", Toast.LENGTH_SHORT).show()
        }

        findViewById<Button>(R.id.btnCambiarFalsaFecha).setOnClickListener {
            calendar.add(Calendar.DAY_OF_YEAR, 1)
            cargarPrediccion()
            Toast.makeText(this, "Avanzando al siguiente día: " + displayFormat.format(calendar.time), Toast.LENGTH_SHORT).show()
        }

        cargarCuentas()
        cargarPrediccion()
    }

    override fun onResume() {
        super.onResume()
        cargarCuentas()
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
}