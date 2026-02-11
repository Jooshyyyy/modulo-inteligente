package bo.edu.modulointeligente

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
private const val TAG = "LOGIN_FINANZAS"
class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Vincular componentes
        val etCarnet = findViewById<EditText>(R.id.etCarnet)
        val etPassword = findViewById<EditText>(R.id.etPassword)
        val btnLogin = findViewById<Button>(R.id.btnLogin)
        val btnIrRegistro = findViewById<Button>(R.id.btnIrRegistro)
        btnIrRegistro.setOnClickListener {
            val intent = android.content.Intent(this, RegistroActivity::class.java)
            startActivity(intent)
        }

        btnLogin.setOnClickListener {
            val carnet = etCarnet.text.toString()
            val pass = etPassword.text.toString()

            if (carnet.isEmpty() || pass.isEmpty()) {
                Toast.makeText(this, "Por favor llena los campos", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Llamada al servidor usando Corrutinas
            lifecycleScope.launch {
                try {
                    val request = LoginRequest(carnet, pass)
                    val response = RetrofitClient.instance.login(request)

                    if (response.isSuccessful) {
                        val body = response.body()
                        // ESTO ES LO QUE BUSCAREMOS EN LOGCAT:
                        android.util.Log.d(TAG, "Token recibido: ${body?.token}")
                        android.util.Log.d(TAG, "Usuario: ${body?.usuario?.nombre}")

                        Toast.makeText(this@MainActivity, "Bienvenido ${body?.usuario?.nombre}", Toast.LENGTH_SHORT).show()
                    } else {
                        android.util.Log.e(TAG, "Error en el servidor: ${response.code()}")
                    }
                } catch (e: Exception) {
                    Toast.makeText(this@MainActivity, "Error de conexión: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}