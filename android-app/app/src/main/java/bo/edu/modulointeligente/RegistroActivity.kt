package bo.edu.modulointeligente

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

class RegistroActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_registro)

        val btnRegistrar = findViewById<Button>(R.id.btnRegistrarUsuario)

        btnRegistrar.setOnClickListener {
            // Capturar datos de la UI
            val req = RegistroRequest(
                primer_nombre = findViewById<EditText>(R.id.etPrimerNombre).text.toString(),
                segundo_nombre = findViewById<EditText>(R.id.etSegundoNombre).text.toString(),
                apellido_paterno = findViewById<EditText>(R.id.etApellidoPaterno).text.toString(),
                apellido_materno = findViewById<EditText>(R.id.etApellidoMaterno).text.toString(),
                email = findViewById<EditText>(R.id.etEmail).text.toString(),
                password = findViewById<EditText>(R.id.etPassword).text.toString(),
                numero_carnet = findViewById<EditText>(R.id.etCarnet).text.toString(),
                telefono = findViewById<EditText>(R.id.etTelefono).text.toString(),
                direccion = findViewById<EditText>(R.id.etDireccion).text.toString(),
                ocupacion = findViewById<EditText>(R.id.etOcupacion).text.toString()
            )

            if (req.primer_nombre.isEmpty() || req.email.isEmpty() || req.password.isEmpty()) {
                Toast.makeText(this, "Completa los campos obligatorios", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            lifecycleScope.launch {
                try {
                    val response = RetrofitClient.instance.registrarUsuario(req)
                    if (response.isSuccessful) {
                        Toast.makeText(this@RegistroActivity, "¡Cuenta creada! Inicia sesión", Toast.LENGTH_LONG).show()
                        finish()
                    } else {
                        // Aquí verás si el carnet ya existe (Error 400)
                        Toast.makeText(this@RegistroActivity, "Error: Carnet o Email ya registrados", Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    android.util.Log.e("REGISTRO", "Error: ${e.message}")
                    Toast.makeText(this@RegistroActivity, "Fallo de conexión", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}