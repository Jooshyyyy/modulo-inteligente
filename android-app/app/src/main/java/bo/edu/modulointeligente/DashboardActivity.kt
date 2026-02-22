package bo.edu.modulointeligente

import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.drawerlayout.widget.DrawerLayout
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import com.google.android.material.navigation.NavigationView

class DashboardActivity : BaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dashboard)

        // Setup Drawer
        val drawerLayout = findViewById<DrawerLayout>(R.id.drawer_layout)
        val navView = findViewById<NavigationView>(R.id.nav_view)
        setupDrawer(drawerLayout, navView)

        val sessionManager = SessionManager(this)
        val userId = sessionManager.getUserId()
        val tvSaldo = findViewById<TextView>(R.id.tvSaldo)

        // Llamada al servidor para traer el saldo de la tabla 'cuentas'
        lifecycleScope.launch {
            try {
                // Usamos el ID que guardamos en el login
                val response = RetrofitClient.instance.getSaldo(userId)
                if (response.isSuccessful) {
                    val cuenta = response.body()
                    tvSaldo.text = "${cuenta?.moneda} ${cuenta?.saldo}"
                }
            } catch (e: Exception) {
                Toast.makeText(this@DashboardActivity, "Error al cargar saldo", Toast.LENGTH_SHORT).show()
            }
        }
    }
}