package bo.edu.modulointeligente

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import kotlin.jvm.java

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
                        val userId = body?.usuario?.id
                        val token = body?.token
                        val nombre = body?.usuario?.nombre

                        if (token != null && userId != null) {
                            // 1. Inicializar el persistidor de datos
                            val sessionManager = SessionManager(this@MainActivity)

                            // 2. Guardar ID y Token para que estén disponibles en toda la app
                            sessionManager.saveAuthToken(token)
                            sessionManager.saveUserId(userId)

                            android.util.Log.d(TAG, "Sesión guardada - ID: $userId, Token: ${token.take(10)}...")
                            Toast.makeText(this@MainActivity, "¡Bienvenido $nombre!", Toast.LENGTH_SHORT).show()

                            // 3. Saltar a la pantalla de Dashboard (Consulta de Saldo)
                            val intent = android.content.Intent(this@MainActivity, DashboardActivity::class.java)
                            startActivity(intent)

                            // Cerramos la pantalla de Login para que no pueda volver atrás al loguearse
                            finish()
                        } else {
                            android.util.Log.e(TAG, "El servidor no envió Token o ID")
                            Toast.makeText(this@MainActivity, "Error en los datos del servidor", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        android.util.Log.e(TAG, "Error en el servidor: ${response.code()}")
                        Toast.makeText(this@MainActivity, "Credenciales incorrectas", Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    Toast.makeText(this@MainActivity, "Error de conexión: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}