package bo.edu.modulointeligente

import android.content.Context
import androidx.core.content.ContextCompat

object BankColors {
    fun success(context: Context) = ContextCompat.getColor(context, R.color.success)
    fun warning(context: Context) = ContextCompat.getColor(context, R.color.warning)
    fun error(context: Context) = ContextCompat.getColor(context, R.color.error)
    fun muted(context: Context) = ContextCompat.getColor(context, R.color.text_secondary)
    fun chartPrimary(context: Context) = ContextCompat.getColor(context, R.color.chart_primary)
    fun chartGrid(context: Context) = ContextCompat.getColor(context, R.color.chart_grid)
    fun chartBar(context: Context) = ContextCompat.getColor(context, R.color.chart_bar)
    fun chartLabel(context: Context) = ContextCompat.getColor(context, R.color.chart_label)
    fun highlightRow(context: Context) = ContextCompat.getColor(context, R.color.highlight_row)
    fun surface(context: Context) = ContextCompat.getColor(context, R.color.surface_white)
}
