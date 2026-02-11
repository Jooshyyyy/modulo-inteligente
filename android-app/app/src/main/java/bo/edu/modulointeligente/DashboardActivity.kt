package bo.edu.modulointeligente

import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

class DashboardActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dashboard) // El XML de la tarjeta azul

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