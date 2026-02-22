package bo.edu.modulointeligente

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class ContactoAdapter(
    private var contactos: List<ContactoResponse>,
    private val onEditClick: (ContactoResponse) -> Unit,
    private val onDeleteClick: (ContactoResponse) -> Unit
) : RecyclerView.Adapter<ContactoAdapter.ContactoViewHolder>() {

    class ContactoViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvNombreBanco: TextView = view.findViewById(R.id.tvNombreBanco)
        val tvCuentaBancaria: TextView = view.findViewById(R.id.tvCuentaBancaria)
        val tvNombreContacto: TextView = view.findViewById(R.id.tvNombreContacto)
        val btnEditar: ImageButton = view.findViewById(R.id.btnEditar)
        val btnEliminar: ImageButton = view.findViewById(R.id.btnEliminar)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ContactoViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_contacto, parent, false)
        return ContactoViewHolder(view)
    }

    override fun onBindViewHolder(holder: ContactoViewHolder, position: Int) {
        val contacto = contactos[position]
        holder.tvNombreBanco.text = contacto.nombre_banco
        holder.tvCuentaBancaria.text = contacto.cuenta_bancaria
        holder.tvNombreContacto.text = contacto.nombre
        
        holder.btnEditar.setOnClickListener { onEditClick(contacto) }
        holder.btnEliminar.setOnClickListener { onDeleteClick(contacto) }
    }

    override fun getItemCount() = contactos.size

    fun updateData(newContactos: List<ContactoResponse>) {
        contactos = newContactos
        notifyDataSetChanged()
    }
}
