package bo.edu.modulointeligente

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.drawerlayout.widget.DrawerLayout
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.navigation.NavigationView
import kotlinx.coroutines.launch

class ContactosActivity : BaseActivity() {

    private lateinit var adapter: ContactoAdapter
    private var usuarioId: Int = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_contactos)

        val drawerLayout = findViewById<DrawerLayout>(R.id.drawer_layout)
        val navView = findViewById<NavigationView>(R.id.nav_view)
        setupDrawer(drawerLayout, navView)

        val sessionManager = SessionManager(this)
        usuarioId = sessionManager.getUserId()

        val rvContactos = findViewById<RecyclerView>(R.id.rvContactos)
        rvContactos.layoutManager = LinearLayoutManager(this)
        
        adapter = ContactoAdapter(
            contactos = emptyList(),
            onEditClick = { contacto ->
                val intent = Intent(this, ContactoFormActivity::class.java).apply {
                    putExtra("CONTACTO_ID", contacto.id)
                    putExtra("NOMBRE", contacto.nombre)
                    putExtra("ALIAS", contacto.alias)
                    putExtra("CUENTA", contacto.cuenta_bancaria)
                    putExtra("BANCO", contacto.nombre_banco)
                }
                startActivityForResult(intent, 100)
            },
            onDeleteClick = { contacto ->
                eliminarContacto(contacto.id)
            }
        )
        rvContactos.adapter = adapter

        findViewById<Button>(R.id.btnAgregarContacto).setOnClickListener {
            val intent = Intent(this, ContactoFormActivity::class.java)
            startActivityForResult(intent, 100)
        }

        cargarContactos()
    }

    private fun cargarContactos() {
        lifecycleScope.launch {
            try {
                val response = RetrofitClient.instance.getContactos(usuarioId)
                if (response.isSuccessful) {
                    adapter.updateData(response.body() ?: emptyList())
                } else {
                    Toast.makeText(this@ContactosActivity, "Error al cargar contactos", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@ContactosActivity, "Error de conexión", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun eliminarContacto(id: Int) {
        lifecycleScope.launch {
            try {
                val response = RetrofitClient.instance.eliminarContacto(id)
                if (response.isSuccessful) {
                    Toast.makeText(this@ContactosActivity, "Contacto eliminado", Toast.LENGTH_SHORT).show()
                    cargarContactos()
                }
            } catch (e: Exception) {
                Toast.makeText(this@ContactosActivity, "Error de conexión", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 100 && resultCode == RESULT_OK) {
            cargarContactos()
        }
    }
}
