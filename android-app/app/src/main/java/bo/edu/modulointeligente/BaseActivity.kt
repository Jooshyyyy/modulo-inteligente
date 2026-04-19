package bo.edu.modulointeligente
import bo.edu.modulointeligente.ProfileActivity
import android.content.Intent
import android.widget.TextView
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import com.google.android.material.navigation.NavigationView


open class BaseActivity : AppCompatActivity() {

    protected fun setupDrawer(drawerLayout: DrawerLayout, navView: NavigationView) {
        val toggle = ActionBarDrawerToggle(
            this, drawerLayout, R.string.navigation_drawer_open, R.string.navigation_drawer_close
        )
        drawerLayout.addDrawerListener(toggle)
        toggle.syncState()

        // Set User Name in Header
        val sessionManager = SessionManager(this)
        val headerView = navView.getHeaderView(0)
        val tvUserName = headerView.findViewById<TextView>(R.id.tvUserName)
        tvUserName.text = sessionManager.getUserName()

        headerView.setOnClickListener {
            if (this.javaClass != ProfileActivity::class.java) {
                val intent = Intent(this, ProfileActivity::class.java)
                startActivity(intent)
                drawerLayout.closeDrawer(GravityCompat.START)
            }
        }

        navView.setNavigationItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.nav_home -> {
                    if (this !is DashboardActivity) {
                        startActivity(Intent(this, DashboardActivity::class.java))
                        finish()
                    }
                }
                R.id.nav_contactos -> {
                    if (this !is ContactosActivity) {
                        startActivity(Intent(this, ContactosActivity::class.java))
                    }
                }
                R.id.nav_transferencia -> {
                    if (this !is TransferenciaActivity) {
                        startActivity(Intent(this, TransferenciaActivity::class.java))
                    }
                }
                R.id.nav_ia_prediccion -> {
                    if (this !is IAPrediccionActivity) {
                        startActivity(Intent(this, IAPrediccionActivity::class.java))
                    }
                }
            }
            drawerLayout.closeDrawer(GravityCompat.START)
            true
        }
    }

    override fun onBackPressed() {
        val drawerLayout = findViewById<DrawerLayout>(R.id.drawer_layout)
        if (drawerLayout != null && drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START)
        } else {
            super.onBackPressed()
        }
    }
}
