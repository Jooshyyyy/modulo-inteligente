package bo.edu.modulointeligente

import android.os.Bundle
import android.util.Log
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.net.HttpURLConnection
import java.net.URL

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val btn = findViewById<Button>(R.id.btnProbar)

        btn.setOnClickListener {
            probarBackend()
        }
    }

    private fun probarBackend() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val url = URL("http://192.168.0.19:3000/api/test/estado")
                val conn = url.openConnection() as HttpURLConnection
                conn.requestMethod = "GET"

                val response = conn.inputStream.bufferedReader().readText()
                Log.d("BACKEND", response)

            } catch (e: Exception) {
                Log.e("ERROR", e.message ?: "Error desconocido")
            }
        }
    }
}
