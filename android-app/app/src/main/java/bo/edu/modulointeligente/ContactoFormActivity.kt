package bo.edu.modulointeligente

import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.launch

class ContactoFormActivity : AppCompatActivity() {

    private var contactoId: Int? = null
    private val bancos = arrayOf(
        "Banco Unión",
        "Banco Mercantil Santa Cruz",
        "Banco Nacional",
        "Banco BISA",
        "Banco de Crédito de Bolivia",
        "Banco Fie",
        "Banco Sol",
        "Banco Ganadero",
        "Banco Económico",
        "Banco Prodem",
        "Banco de Desarrollo Productivo",
        "Banco Fortaleza"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_contacto_form)

        val etNombre = findViewById<TextInputEditText>(R.id.etNombre)
        val etAlias = findViewById<TextInputEditText>(R.id.etAlias)
        val etCuentaBancaria = findViewById<TextInputEditText>(R.id.etCuentaBancaria)
        val etNombreBanco = findViewById<AutoCompleteTextView>(R.id.etNombreBanco)
        val btnGuardar = findViewById<Button>(R.id.btnGuardar)
        val tvTitle = findViewById<TextView>(R.id.tvFormTitle)

        // Setup Dropdown Adapter
        val adapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, bancos)
        etNombreBanco.setAdapter(adapter)

        // Ver si estamos editando
        contactoId = intent.getIntExtra("CONTACTO_ID", -1).takeIf { it != -1 }
        if (contactoId != null) {
            tvTitle.text = "Editar Contacto"
            etNombre.setText(intent.getStringExtra("NOMBRE"))
            etAlias.setText(intent.getStringExtra("ALIAS"))
            etCuentaBancaria.setText(intent.getStringExtra("CUENTA"))
            etNombreBanco.setText(intent.getStringExtra("BANCO"), false)
        }

        btnGuardar.setOnClickListener {
            val nombre = etNombre.text.toString()
            val alias = etAlias.text.toString()
            val cuenta = etCuentaBancaria.text.toString()
            val banco = etNombreBanco.text.toString()

            if (nombre.isEmpty() || cuenta.isEmpty() || banco.isEmpty()) {
                Toast.makeText(this, "Por favor completa los campos obligatorios", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val sessionManager = SessionManager(this)
            val usuario_id = sessionManager.getUserId()

            val request = ContactoRequest(
                usuario_id = if (contactoId == null) usuario_id else null,
                nombre = nombre,
                alias = alias,
                cuenta_bancaria = cuenta,
                nombre_banco = banco
            )

            lifecycleScope.launch {
                try {
                    val response = if (contactoId == null) {
                        RetrofitClient.instance.crearContacto(request)
                    } else {
                        RetrofitClient.instance.actualizarContacto(contactoId!!, request)
                    }

                    if (response.isSuccessful) {
                        Toast.makeText(this@ContactoFormActivity, "Contacto guardado exitosamente", Toast.LENGTH_SHORT).show()
                        setResult(RESULT_OK)
                        finish()
                    } else {
                        Toast.makeText(this@ContactoFormActivity, "Error al guardar contacto", Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    Toast.makeText(this@ContactoFormActivity, "Error de conexión: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}
