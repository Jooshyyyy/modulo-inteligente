package bo.edu.modulointeligente

import android.os.Bundle
import androidx.drawerlayout.widget.DrawerLayout
import com.google.android.material.navigation.NavigationView

class ContactosActivity : BaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_contactos)

        val drawerLayout = findViewById<DrawerLayout>(R.id.drawer_layout)
        val navView = findViewById<NavigationView>(R.id.nav_view)
        setupDrawer(drawerLayout, navView)
    }
}
