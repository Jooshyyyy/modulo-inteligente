package bo.edu.modulointeligente.ui.prediccion

import android.view.LayoutInflater
import android.view.ViewGroup
import android.view.animation.DecelerateInterpolator
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import bo.edu.modulointeligente.ProbabilidadItem
import bo.edu.modulointeligente.R

class ProbabilidadAdapter(
    private val onClick: (ProbabilidadItem) -> Unit
) : RecyclerView.Adapter<ProbabilidadAdapter.VH>() {

    private var items: List<ProbabilidadItem> = emptyList()

    fun submit(list: List<ProbabilidadItem>) {
        items = list
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_probabilidad_ia, parent, false)
        return VH(view as ViewGroup, onClick)
    }

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: VH, position: Int) {
        holder.bind(items[position])
        holder.itemView.alpha = 0f
        holder.itemView.translationY = 10f
        holder.itemView.animate()
            .alpha(1f)
            .translationY(0f)
            .setDuration(180)
            .setStartDelay((position * 18L).coerceAtMost(150L))
            .setInterpolator(DecelerateInterpolator())
            .start()
    }

    class VH(
        root: ViewGroup,
        private val onClick: (ProbabilidadItem) -> Unit
    ) : RecyclerView.ViewHolder(root) {
        private val tvNombre: TextView = root.findViewById(R.id.tvProbItemNombre)
        private val tvDetalle: TextView = root.findViewById(R.id.tvProbItemDetalle)
        private val tvMonto: TextView = root.findViewById(R.id.tvProbItemMonto)

        fun bind(item: ProbabilidadItem) {
            tvNombre.text = item.nombre
            tvDetalle.text = "${item.hora} • ${item.porcentaje}% probabilidad"
            tvMonto.text = "Bs. ${item.monto}"
            itemView.setOnClickListener { onClick(item) }
        }
    }
}

