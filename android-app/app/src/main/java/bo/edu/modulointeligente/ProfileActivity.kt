package bo.edu.modulointeligente

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

class ProfileActivity : BaseActivity() {

    private lateinit var etCarnet: EditText
    private lateinit var etPrimerNombre: EditText
    private lateinit var etSegundoNombre: EditText
    private lateinit var etApellidoPaterno: EditText
    private lateinit var etApellidoMaterno: EditText
    private lateinit var etFechaNacimiento: EditText

    private lateinit var etEmail: EditText
    private lateinit var etPassword: EditText
    private lateinit var etTelefono: EditText
    private lateinit var etOcupacion: EditText
    private lateinit var etDireccion: EditText
    private lateinit var btnGuardar: Button

    private var userId: Int = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)

        val sessionManager = SessionManager(this)
        userId = sessionManager.getUserId()

        if (userId == -1) {
            Toast.makeText(this, "Debe iniciar sesión primero", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        bindViews()
        cargarPerfil()

        btnGuardar.setOnClickListener {
            guardarPerfil()
        }
    }

    private fun bindViews() {
        etCarnet = findViewById(R.id.etProfileCarnet)
        etPrimerNombre = findViewById(R.id.etProfilePrimerNombre)
        etSegundoNombre = findViewById(R.id.etProfileSegundoNombre)
        etApellidoPaterno = findViewById(R.id.etProfileApellidoPaterno)
        etApellidoMaterno = findViewById(R.id.etProfileApellidoMaterno)
        etFechaNacimiento = findViewById(R.id.etProfileFechaNacimiento)

        etEmail = findViewById(R.id.etProfileEmail)
        etPassword = findViewById(R.id.etProfilePassword)
        etTelefono = findViewById(R.id.etProfileTelefono)
        etOcupacion = findViewById(R.id.etProfileOcupacion)
        etDireccion = findViewById(R.id.etProfileDireccion)
        btnGuardar = findViewById(R.id.btnGuardarPerfil)
    }

    private fun cargarPerfil() {
        lifecycleScope.launch {
            try {
                val response = RetrofitClient.instance.getPerfil(userId)
                if (response.isSuccessful) {
                    val perfil = response.body()
                    if (perfil != null) {
                        etCarnet.setText(perfil.numero_carnet ?: "")
                        etPrimerNombre.setText(perfil.primer_nombre ?: "")
                        etSegundoNombre.setText(perfil.segundo_nombre ?: "")
                        etApellidoPaterno.setText(perfil.apellido_paterno ?: "")
                        etApellidoMaterno.setText(perfil.apellido_materno ?: "")
                        val fechaCorta = perfil.fecha_nacimiento?.split("T")?.get(0) ?: ""
                        etFechaNacimiento.setText(fechaCorta)

                        etEmail.setText(perfil.email ?: "")
                        etTelefono.setText(perfil.telefono ?: "")
                        etOcupacion.setText(perfil.ocupacion ?: "")
                        etDireccion.setText(perfil.direccion ?: "")

                        // Validar Rol Actual del Token
                        val rolActual = getRoleFromToken(RetrofitClient.authToken)
                        if (rolActual == "CLIENTE") {
                            // Bloquear campos para usuario Cliente
                            etCarnet.isEnabled = false
                            etPrimerNombre.isEnabled = false
                            etSegundoNombre.isEnabled = false
                            etApellidoPaterno.isEnabled = false
                            etApellidoMaterno.isEnabled = false
                            etFechaNacimiento.isEnabled = false
                        }
                    }
                } else {
                    Toast.makeText(this@ProfileActivity, "Error al cargar el perfil", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@ProfileActivity, "Error de conexión: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun guardarPerfil() {
        // Enviar solo lo que está escrito y permitido. El backend también filtrará según el rol
        val req = ProfileRequest(
            email = etEmail.text.toString().takeIf { it.isNotBlank() },
            password = etPassword.text.toString().takeIf { it.isNotBlank() },
            telefono = etTelefono.text.toString().takeIf { it.isNotBlank() },
            ocupacion = etOcupacion.text.toString().takeIf { it.isNotBlank() },
            direccion = etDireccion.text.toString().takeIf { it.isNotBlank() },
            
            numero_carnet = etCarnet.text.toString().takeIf { it.isNotBlank() },
            primer_nombre = etPrimerNombre.text.toString().takeIf { it.isNotBlank() },
            segundo_nombre = etSegundoNombre.text.toString().takeIf { it.isNotBlank() },
            apellido_paterno = etApellidoPaterno.text.toString().takeIf { it.isNotBlank() },
            apellido_materno = etApellidoMaterno.text.toString().takeIf { it.isNotBlank() },
            fecha_nacimiento = etFechaNacimiento.text.toString().takeIf { it.isNotBlank() }
        )

        lifecycleScope.launch {
            try {
                val response = RetrofitClient.instance.actualizarPerfil(userId, req)
                if (response.isSuccessful) {
                    Toast.makeText(this@ProfileActivity, "Perfil actualizado correctamente", Toast.LENGTH_LONG).show()
                    // Reflejamos el nuevo nombre en Session, si se modificó
                    if (req.primer_nombre != null || req.apellido_paterno != null) {
                        val sessionManager = SessionManager(this@ProfileActivity)
                        val nuevoNombre = listOfNotNull(
                            req.primer_nombre ?: etPrimerNombre.text.toString(),
                            req.segundo_nombre ?: etSegundoNombre.text.toString(),
                            req.apellido_paterno ?: etApellidoPaterno.text.toString(),
                            req.apellido_materno ?: etApellidoMaterno.text.toString()
                        ).filter { it.isNotBlank() }.joinToString(" ")
                        sessionManager.saveUserName(nuevoNombre.ifEmpty { "Usuario" })
                    }
                    finish()
                } else {
                    Toast.makeText(this@ProfileActivity, "Error al actualizar (Sin Permisos/Datos inválidos)", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@ProfileActivity, "Error de conexión", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun getRoleFromToken(token: String?): String {
        if (token == null) return "CLIENTE"
        try {
            val parts = token.split(".")
            if (parts.size == 3) {
                val payload = String(android.util.Base64.decode(parts[1], android.util.Base64.URL_SAFE))
                if (payload.contains("\"rol\":\"ADMIN\"")) {
                    return "ADMIN"
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return "CLIENTE"
    }
}
