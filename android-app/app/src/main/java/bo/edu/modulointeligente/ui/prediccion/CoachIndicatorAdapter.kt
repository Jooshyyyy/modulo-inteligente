package bo.edu.modulointeligente.ui.prediccion

import android.view.LayoutInflater
import android.view.ViewGroup
import android.view.animation.DecelerateInterpolator
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import bo.edu.modulointeligente.IaCoachIndicador
import bo.edu.modulointeligente.R

class CoachIndicatorAdapter : RecyclerView.Adapter<CoachIndicatorAdapter.VH>() {

    private var items: List<IaCoachIndicador> = emptyList()

    fun submit(list: List<IaCoachIndicador>) {
        items = list
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_ia_coach_indicator, parent, false)
        return VH(v as ViewGroup)
    }

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: VH, position: Int) {
        holder.bind(items[position])
        holder.itemView.alpha = 0f
        holder.itemView.translationY = 12f
        holder.itemView.animate()
            .alpha(1f)
            .translationY(0f)
            .setDuration(220)
            .setStartDelay((position * 25L).coerceAtMost(200L))
            .setInterpolator(DecelerateInterpolator())
            .start()
    }

    class VH(root: ViewGroup) : RecyclerView.ViewHolder(root) {
        private val tvLabel: TextView = root.findViewById(R.id.tvIndLabel)
        private val tvValue: TextView = root.findViewById(R.id.tvIndValue)
        private val tvDetail: TextView = root.findViewById(R.id.tvIndDetail)

        fun bind(item: IaCoachIndicador) {
            tvLabel.text = item.etiqueta
            tvValue.text = item.valor
            tvDetail.text = item.detalle
        }
    }
}

