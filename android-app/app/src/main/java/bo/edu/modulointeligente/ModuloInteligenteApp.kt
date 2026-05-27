package bo.edu.modulointeligente

import android.app.Application
import androidx.appcompat.app.AppCompatDelegate

class ModuloInteligenteApp : Application() {
    override fun onCreate() {
        super.onCreate()
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
    }
}
