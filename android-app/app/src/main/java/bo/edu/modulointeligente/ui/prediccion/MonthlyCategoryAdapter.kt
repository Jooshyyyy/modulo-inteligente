package bo.edu.modulointeligente.ui.prediccion

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.DecelerateInterpolator
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import bo.edu.modulointeligente.PrediccionSemanalCategoria
import bo.edu.modulointeligente.R
import java.text.DecimalFormat
import kotlin.math.roundToInt

class MonthlyCategoryAdapter(
    private val decimalFormat: DecimalFormat,
    private val parseColor: (String?) -> Int,
    private val onToggle: (String) -> Unit
) : RecyclerView.Adapter<MonthlyCategoryAdapter.VH>() {

    private var total: Double = 0.0
    private var selected: String? = null
    private var items: List<PrediccionSemanalCategoria> = emptyList()

    fun submit(list: List<PrediccionSemanalCategoria>, totalMes: Double, selectedCategoria: String?) {
        items = list
        total = totalMes
        selected = selectedCategoria
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_weekly_category, parent, false)
        return VH(v as ViewGroup, decimalFormat, parseColor)
    }

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: VH, position: Int) {
        val item = items[position]
        holder.bind(item, total, item.categoria == selected)
        holder.itemView.setOnClickListener { onToggle(item.categoria) }
        holder.itemView.alpha = 0f
        holder.itemView.translationY = 10f
        holder.itemView.animate()
            .alpha(1f)
            .translationY(0f)
            .setDuration(180)
            .setStartDelay((position * 18L).coerceAtMost(160L))
            .setInterpolator(DecelerateInterpolator())
            .start()
    }

    class VH(
        root: ViewGroup,
        private val decimalFormat: DecimalFormat,
        private val parseColor: (String?) -> Int
    ) : RecyclerView.ViewHolder(root) {

        private val tvName: TextView = root.findViewById(R.id.tvCategoryName)
        private val tvAmount: TextView = root.findViewById(R.id.tvCategoryAmount)
        private val tvPercent: TextView = root.findViewById(R.id.tvCategoryPercent)
        private val vColor: View = root.findViewById(R.id.viewCategoryColor)
        private val vRoot: View = root.findViewById(R.id.rowCategoryRoot)

        fun bind(item: PrediccionSemanalCategoria, total: Double, isSelected: Boolean) {
            tvName.text = item.categoria
            tvAmount.text = "Bs. ${decimalFormat.format(item.monto)}"
            val percent = if (total > 0.0 && !total.isNaN()) ((item.monto / total) * 100).roundToInt() else 0
            tvPercent.text = "$percent%"
            vColor.setBackgroundColor(parseColor(item.colorHex))
            vRoot.setBackgroundColor(if (isSelected) Color.parseColor("#332A55") else Color.TRANSPARENT)
        }
    }
}

