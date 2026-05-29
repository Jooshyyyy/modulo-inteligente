package bo.edu.modulointeligente

import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import androidx.core.content.ContextCompat
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale
import kotlin.math.roundToInt

object SavingsHeroBinder {

    private val decimalFormat =
        DecimalFormat("#,##0.00", DecimalFormatSymbols(Locale("es", "ES")))

    fun bind(
        amountView: TextView,
        labelView: TextView,
        insightView: TextView,
        progressBar: ProgressBar,
        estado: EstadoPresupuestoCoach?,
        tieneMeta: Boolean,
        gastoProyectadoMes: Double = Double.NaN
    ) {
        if (!tieneMeta || estado == null || estado.tope <= 0) {
            amountView.text = "Bs. —"
            labelView.text = "activá tu tope mensual"
            insightView.text = "La IA compara tu gasto previsto con un límite que vos definís"
            progressBar.visibility = View.GONE
            return
        }

        progressBar.visibility = View.VISIBLE
        progressBar.progress = estado.usoPct.roundToInt().coerceIn(0, 100)

        if (estado.exceso > 0) {
            amountView.text = "Bs. ${decimalFormat.format(estado.exceso)}"
            labelView.text = "por encima del tope"
            insightView.text = "Recortá gastos — la IA te sugiere dónde empezar abajo"
            amountView.setTextColor(ContextCompat.getColor(amountView.context, R.color.savings_gold))
        } else {
            amountView.setTextColor(ContextCompat.getColor(amountView.context, R.color.white))
            amountView.text = "Bs. ${decimalFormat.format(estado.ahorroProyectado)}"
            labelView.text = "de margen este mes"
            val pct = estado.usoPct.roundToInt()
            insightView.text = when {
                pct <= 60 -> "Vas muy bien — podés apartar este monto hoy"
                pct <= 85 -> "Buen ritmo — cuidá los picos de fin de mes"
                else -> "Cerca del tope — mirá las acciones sugeridas"
            }
        }

        if (!gastoProyectadoMes.isNaN() && gastoProyectadoMes > 0) {
            labelView.text = "${labelView.text} · gasto previsto Bs. ${decimalFormat.format(gastoProyectadoMes)}"
        }
    }
}
