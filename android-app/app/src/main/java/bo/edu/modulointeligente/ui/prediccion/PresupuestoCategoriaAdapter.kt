package bo.edu.modulointeligente.ui.prediccion

import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.CheckBox
import androidx.recyclerview.widget.RecyclerView
import bo.edu.modulointeligente.CategoriaItem
import bo.edu.modulointeligente.R
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout

class PresupuestoCategoriaAdapter : RecyclerView.Adapter<PresupuestoCategoriaAdapter.VH>() {

    data class LimiteUi(
        val categoria: CategoriaItem,
        var seleccionada: Boolean = false,
        var topeTexto: String = ""
    )

    private var items: List<LimiteUi> = emptyList()

    fun submit(categorias: List<CategoriaItem>, preseleccion: Map<Int, Double> = emptyMap()) {
        items = categorias.map { cat ->
            val tope = preseleccion[cat.id]
            LimiteUi(
                categoria = cat,
                seleccionada = tope != null,
                topeTexto = tope?.let { if (it % 1.0 == 0.0) it.toLong().toString() else it.toString() } ?: ""
            )
        }
        notifyDataSetChanged()
    }

    fun limitesSeleccionados(): List<Pair<Int, Double>> {
        return items.mapNotNull { ui ->
            if (!ui.seleccionada) return@mapNotNull null
            val tope = ui.topeTexto.replace(",", ".").toDoubleOrNull() ?: return@mapNotNull null
            if (tope <= 0) return@mapNotNull null
            ui.categoria.id to tope
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_presupuesto_categoria_limite, parent, false)
        return VH(v as ViewGroup)
    }

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: VH, position: Int) {
        holder.bind(items[position]) { items[position] = it }
    }

    class VH(private val root: ViewGroup) : RecyclerView.ViewHolder(root) {
        private val check: CheckBox = root.findViewById(R.id.checkCategoria)
        private val til: TextInputLayout = root.findViewById(R.id.tilTopeCategoria)
        private val et: TextInputEditText = root.findViewById(R.id.etTopeCategoria)

        fun bind(ui: LimiteUi, onChange: (LimiteUi) -> Unit) {
            check.text = ui.categoria.nombre
            check.setOnCheckedChangeListener(null)
            check.isChecked = ui.seleccionada
            et.setText(ui.topeTexto)
            et.isEnabled = ui.seleccionada
            til.isEnabled = ui.seleccionada

            check.setOnCheckedChangeListener { _, checked ->
                ui.seleccionada = checked
                et.isEnabled = checked
                til.isEnabled = checked
                onChange(ui)
            }
            et.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
                override fun afterTextChanged(s: Editable?) {
                    ui.topeTexto = s?.toString()?.trim().orEmpty()
                    onChange(ui)
                }
            })
        }
    }
}
