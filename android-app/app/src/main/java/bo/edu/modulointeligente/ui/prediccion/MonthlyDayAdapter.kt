package bo.edu.modulointeligente.ui.prediccion

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.DecelerateInterpolator
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import bo.edu.modulointeligente.PrediccionDetalleDia
import bo.edu.modulointeligente.R
import java.text.DecimalFormat

class MonthlyDayAdapter(
    private val decimalFormat: DecimalFormat,
    private val parseColor: (String?) -> Int,
    private val formatDayLabel: (String) -> String,
    private val onClick: (PrediccionDetalleDia) -> Unit
) : RecyclerView.Adapter<MonthlyDayAdapter.VH>() {

    private var items: List<PrediccionDetalleDia> = emptyList()

    fun submit(list: List<PrediccionDetalleDia>) {
        items = list
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_prediction_day, parent, false)
        return VH(v as ViewGroup, decimalFormat, parseColor, formatDayLabel, onClick)
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
            .setStartDelay((position * 12L).coerceAtMost(180L))
            .setInterpolator(DecelerateInterpolator())
            .start()
    }

    class VH(
        root: ViewGroup,
        private val decimalFormat: DecimalFormat,
        private val parseColor: (String?) -> Int,
        private val formatDayLabel: (String) -> String,
        private val onClick: (PrediccionDetalleDia) -> Unit
    ) : RecyclerView.ViewHolder(root) {

        private val tvDay: TextView = root.findViewById(R.id.tvDayLabel)
        private val tvCat: TextView = root.findViewById(R.id.tvDayCategory)
        private val tvAmount: TextView = root.findViewById(R.id.tvDayAmount)
        private val vColor: View = root.findViewById(R.id.viewDayColor)

        fun bind(item: PrediccionDetalleDia) {
            tvDay.text = formatDayLabel(item.fecha)
            tvCat.text = item.categoria
            tvAmount.text = "Bs. ${decimalFormat.format(item.monto)}"
            vColor.setBackgroundColor(parseColor(item.colorHex))
            itemView.setOnClickListener { onClick(item) }
        }
    }
}

