package bo.edu.modulointeligente

import android.graphics.Bitmap
import android.graphics.Canvas
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream

class TransferDetailsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_transfer_details)

        val tvMonto = findViewById<TextView>(R.id.tvMonto)
        val tvNombreOrigen = findViewById<TextView>(R.id.tvNombreOrigen)
        val tvCuentaOrigen = findViewById<TextView>(R.id.tvCuentaOrigen)
        val tvNombreDestino = findViewById<TextView>(R.id.tvNombreDestino)
        val tvCuentaDestino = findViewById<TextView>(R.id.tvCuentaDestino)
        val tvBancoDestino = findViewById<TextView>(R.id.tvBancoDestino)
        val tvConcepto = findViewById<TextView>(R.id.tvConcepto)
        val tvFecha = findViewById<TextView>(R.id.tvFecha)
        val btnGuardar = findViewById<Button>(R.id.btnGuardar)
        val btnVolver = findViewById<Button>(R.id.btnVolver)
        val receiptContainer = findViewById<LinearLayout>(R.id.receiptContainer)

        // Obtener datos del Intent
        val monto = intent.getDoubleExtra("monto", 0.0)
        val concepto = intent.getStringExtra("concepto") ?: "---"
        val destinoNumero = intent.getStringExtra("destinoNumero") ?: "---"
        val nombreDestino = intent.getStringExtra("nombreDestino") ?: "---"
        val bancoDestino = intent.getStringExtra("bancoDestino") ?: "---"
        val nombreOrigen = intent.getStringExtra("nombreOrigen") ?: "---"
        val cuentaOrigen = intent.getStringExtra("cuentaOrigen") ?: "---"
        val fecha = intent.getStringExtra("fecha") ?: "---"

        // Mostrar datos
        tvMonto.text = "BOB %.2f".format(monto)
        tvNombreOrigen.text = nombreOrigen
        tvCuentaOrigen.text = cuentaOrigen
        tvNombreDestino.text = nombreDestino
        tvCuentaDestino.text = destinoNumero
        tvBancoDestino.text = bancoDestino
        tvConcepto.text = concepto
        tvFecha.text = fecha

        btnVolver.setOnClickListener {
            finish()
        }

        btnGuardar.setOnClickListener {
            saveReceiptAsImage(receiptContainer)
        }
    }

    private fun saveReceiptAsImage(view: View) {
        val bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        view.draw(canvas)

        try {
            val filename = "Comprobante_${System.currentTimeMillis()}.jpg"
            val storageDir = getExternalFilesDir(android.os.Environment.DIRECTORY_PICTURES)
            val imageFile = File(storageDir, filename)
            
            val outputStream: OutputStream = FileOutputStream(imageFile)
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream)
            outputStream.flush()
            outputStream.close()

            Toast.makeText(this, "Comprobante guardado en: ${imageFile.absolutePath}", Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Error al guardar el comprobante", Toast.LENGTH_SHORT).show()
        }
    }
}
