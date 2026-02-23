package bo.edu.modulointeligente

import android.os.Bundle
import android.widget.Toast
import androidx.drawerlayout.widget.DrawerLayout
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.navigation.NavigationView
import kotlinx.coroutines.launch

class DashboardActivity : BaseActivity() {
    
    private lateinit var adapter: CuentaAdapter

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
            Toast.makeText(this, "Actualizando cuentas...", Toast.LENGTH_SHORT).show()
        }

        cargarCuentas()
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
}