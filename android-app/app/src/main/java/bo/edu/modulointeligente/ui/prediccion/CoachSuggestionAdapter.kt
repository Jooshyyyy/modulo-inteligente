package bo.edu.modulointeligente.ui.prediccion

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.DecelerateInterpolator
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import bo.edu.modulointeligente.IaSugerencia
import bo.edu.modulointeligente.R
import java.text.DecimalFormat

class CoachSuggestionAdapter(
    private val decimalFormat: DecimalFormat,
    private val onClick: (IaSugerencia) -> Unit
) : RecyclerView.Adapter<CoachSuggestionAdapter.VH>() {

    private var items: List<IaSugerencia> = emptyList()

    fun submit(list: List<IaSugerencia>) {
        items = list
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_ia_coach_suggestion, parent, false)
        return VH(v as ViewGroup, decimalFormat, onClick)
    }

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: VH, position: Int) {
        holder.bind(items[position])
        holder.itemView.alpha = 0f
        holder.itemView.translationY = 14f
        holder.itemView.animate()
            .alpha(1f)
            .translationY(0f)
            .setDuration(240)
            .setStartDelay((position * 35L).coerceAtMost(240L))
            .setInterpolator(DecelerateInterpolator())
            .start()
    }

    class VH(
        root: ViewGroup,
        private val decimalFormat: DecimalFormat,
        private val onClick: (IaSugerencia) -> Unit
    ) : RecyclerView.ViewHolder(root) {

        private val tvTitle: TextView = root.findViewById(R.id.tvSuggestionTitle)
        private val tvBadge: TextView = root.findViewById(R.id.tvSuggestionBadge)
        private val tvBody: TextView = root.findViewById(R.id.tvSuggestionBody)
        private val tvSav: TextView = root.findViewById(R.id.tvSuggestionSavings)
        private val tvProj: TextView = root.findViewById(R.id.tvSuggestionProjected)
        private val tvCtx: TextView = root.findViewById(R.id.tvSuggestionContext)

        fun bind(item: IaSugerencia) {
            tvTitle.text = item.titulo
            tvBody.text = item.mensaje
            tvBadge.text = "+${decimalFormat.format(item.porcentajeAcercamientoMeta)}% hacia la meta"

            if (!item.montoAhorroSugerido.isNaN() && item.montoAhorroSugerido > 0) {
                tvSav.visibility = View.VISIBLE
                tvSav.text = "Ahorro orientable: Bs. ${decimalFormat.format(item.montoAhorroSugerido)}"
            } else tvSav.visibility = View.GONE

            if (!item.montoProyectado.isNaN() && item.montoProyectado > 0) {
                tvProj.visibility = View.VISIBLE
                tvProj.text = "Gasto proyectado en ese punto: Bs. ${decimalFormat.format(item.montoProyectado)}"
            } else tvProj.visibility = View.GONE

            val ctxParts = mutableListOf<String>()
            item.categoria?.takeIf { it.isNotBlank() }?.let { ctxParts.add(it) }
            item.fecha?.takeIf { it.isNotBlank() }?.let { ctxParts.add(it) }
            if (ctxParts.isNotEmpty()) {
                tvCtx.visibility = View.VISIBLE
                tvCtx.text = ctxParts.joinToString(" · ")
            } else tvCtx.visibility = View.GONE

            itemView.setOnClickListener { onClick(item) }
        }
    }
}

