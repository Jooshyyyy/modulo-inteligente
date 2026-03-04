package bo.edu.modulointeligente

import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Button
import android.widget.Toast
import androidx.drawerlayout.widget.DrawerLayout
import androidx.lifecycle.lifecycleScope
import com.google.android.material.navigation.NavigationView
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.launch

class TransferenciaActivity : BaseActivity() {

    private var contactos: List<ContactoResponse> = emptyList()
    private var cuentasPropias: List<CuentaResponse> = emptyList()
    private var cuentaSeleccionadaId: Int = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_transferencia)

        val sessionManager = SessionManager(this)
        RetrofitClient.authToken = sessionManager.fetchAuthToken()

        val drawerLayout = findViewById<DrawerLayout>(R.id.drawer_layout)
        val navView = findViewById<NavigationView>(R.id.nav_view)
        setupDrawer(drawerLayout, navView)

        val etCuentaOrigen = findViewById<AutoCompleteTextView>(R.id.etCuentaOrigen)
        val etContacto = findViewById<AutoCompleteTextView>(R.id.etContacto)
        val etNombreDestino = findViewById<TextInputEditText>(R.id.etNombreDestino)
        val etCuentaDestino = findViewById<TextInputEditText>(R.id.etCuentaDestino)
        val etBancoDestino = findViewById<TextInputEditText>(R.id.etBancoDestino)
        val etMonto = findViewById<TextInputEditText>(R.id.etMonto)
        val btnTransferir = findViewById<Button>(R.id.btnTransferir)

        cargarDatos(etCuentaOrigen, etContacto)

        etContacto.setOnItemClickListener { _, _, position, _ ->
            val contacto = contactos[position]
            etNombreDestino.setText(contacto.nombre)
            etCuentaDestino.setText(contacto.cuenta_bancaria)
            etBancoDestino.setText(contacto.nombre_banco)
        }

        etCuentaOrigen.setOnItemClickListener { _, _, position, _ ->
            cuentaSeleccionadaId = cuentasPropias[position].id
        }

        btnTransferir.setOnClickListener {
            val montoStr = etMonto.text.toString()
            val cuentaDestino = etCuentaDestino.text.toString()

            if (cuentaSeleccionadaId == -1 || montoStr.isEmpty() || cuentaDestino.isEmpty()) {
                Toast.makeText(this, "Por favor completa todos los campos", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val monto = montoStr.toDoubleOrNull() ?: 0.0
            if (monto <= 0) {
                Toast.makeText(this, "Ingresa un monto válido", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            realizarTransferencia(cuentaSeleccionadaId, monto, cuentaDestino)
        }
    }

    private fun cargarDatos(etCuentas: AutoCompleteTextView, etContactos: AutoCompleteTextView) {
        val sessionManager = SessionManager(this)
        val userId = sessionManager.getUserId()

        lifecycleScope.launch {
            try {
                // 1. Cargar Cuentas Propias
                val resCuentas = RetrofitClient.instance.getCuentas()
                if (resCuentas.isSuccessful) {
                    cuentasPropias = resCuentas.body() ?: emptyList()
                    val nombresCuentas = cuentasPropias.map { "${it.numero_cuenta} (${it.moneda} ${it.saldo})" }
                    val adapterCuentas = ArrayAdapter(this@TransferenciaActivity, android.R.layout.simple_dropdown_item_1line, nombresCuentas)
                    etCuentas.setAdapter(adapterCuentas)
                }

                // 2. Cargar Contactos
                val resContactos = RetrofitClient.instance.getContactos(userId)
                if (resContactos.isSuccessful) {
                    contactos = resContactos.body() ?: emptyList()
                    val nombresContactos = contactos.map { it.nombre }
                    val adapterContactos = ArrayAdapter(this@TransferenciaActivity, android.R.layout.simple_dropdown_item_1line, nombresContactos)
                    etContactos.setAdapter(adapterContactos)
                }
            } catch (e: Exception) {
                Toast.makeText(this@TransferenciaActivity, "Error al cargar datos", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun realizarTransferencia(origenId: Int, monto: Double, destinoNumero: String) {
        lifecycleScope.launch {
            try {
                val request = TransferRequest(
                    cuenta_id = origenId,
                    monto = monto,
                    numero_cuenta_destino = destinoNumero
                )
                val response = RetrofitClient.instance.transferir(request)
                if (response.isSuccessful) {
                    Toast.makeText(this@TransferenciaActivity, "¡Transferencia Exitosa!", Toast.LENGTH_LONG).show()
                    finish() // Regresar
                } else {
                    Toast.makeText(this@TransferenciaActivity, "Error: ${response.message()}", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@TransferenciaActivity, "Error de conexión", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
