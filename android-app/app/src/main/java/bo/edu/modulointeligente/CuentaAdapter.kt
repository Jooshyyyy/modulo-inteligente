package bo.edu.modulointeligente

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class CuentaAdapter(
    private var cuentas: List<CuentaResponse>
) : RecyclerView.Adapter<CuentaAdapter.CuentaViewHolder>() {

    class CuentaViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvTipoCuenta: TextView = view.findViewById(R.id.tvTipoCuenta)
        val tvNumeroCuenta: TextView = view.findViewById(R.id.tvNumeroCuenta)
        val tvSaldoCuenta: TextView = view.findViewById(R.id.tvSaldoCuenta)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CuentaViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_cuenta, parent, false)
        return CuentaViewHolder(view)
    }

    override fun onBindViewHolder(holder: CuentaViewHolder, position: Int) {
        val cuenta = cuentas[position]
        holder.tvTipoCuenta.text = cuenta.tipo_cuenta
        holder.tvNumeroCuenta.text = cuenta.numero_cuenta
        holder.tvSaldoCuenta.text = "${cuenta.moneda} ${String.format("%.2f", cuenta.saldo)}"
    }

    override fun getItemCount() = cuentas.size

    fun updateData(newCuentas: List<CuentaResponse>) {
        cuentas = newCuentas
        notifyDataSetChanged()
    }
}
