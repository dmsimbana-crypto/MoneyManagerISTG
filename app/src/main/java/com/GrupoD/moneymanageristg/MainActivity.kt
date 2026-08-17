package com.GrupoD.moneymanageristg
import android.content.Context
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.navigation.NavController
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import androidx.activity.OnBackPressedCallback
import com.google.android.material.navigation.NavigationView

class MainActivity : AppCompatActivity() {

    private lateinit var navController: NavController
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var appBarConfiguration: AppBarConfiguration

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)

        drawerLayout = findViewById(R.id.drawer_layout)
        val navView = findViewById<NavigationView>(R.id.nav_view)

        navController = findNavController(R.id.nav_host_fragment)

        appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.nav_home,
                R.id.nav_register_transaction,
                R.id.nav_categories,
                R.id.nav_configuration
            ), drawerLayout
        )

        setupActionBarWithNavController(navController, appBarConfiguration)
        navView.setupWithNavController(navController)
        navController.addOnDestinationChangedListener { _, destination, _ ->
            if (destination.id == R.id.nav_login) {
                drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED)
                supportActionBar?.setDisplayHomeAsUpEnabled(false)
                supportActionBar?.setHomeButtonEnabled(false)
            } else {
                drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED)
                supportActionBar?.setDisplayHomeAsUpEnabled(true)
                supportActionBar?.setHomeButtonEnabled(true)
            }
        }

        navView.setNavigationItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.nav_logout -> {
                    cerrarSesion() // Llamar al método unificado
                    true
                }
                else -> {
                    navController.navigate(menuItem.itemId)
                    drawerLayout.closeDrawer(GravityCompat.START)
                    true
                }
            }
        }





        // Método unificado para cerrar sesión desde cualquier lugar


        //  Manejo del botón de retroceso con callback
        val backCallback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
                    drawerLayout.closeDrawer(GravityCompat.START)
                } else {
                    // Permite la navegación atrás normal
                    isEnabled = false
                    onBackPressed()
                    isEnabled = true
                }
            }
        }
        onBackPressedDispatcher.addCallback(this, backCallback)
    }

    override fun onSupportNavigateUp(): Boolean {
        return navController.navigateUp(appBarConfiguration) || super.onSupportNavigateUp()
    }



    fun cerrarSesion() {
        val sharedPref = getSharedPreferences("login_prefs", Context.MODE_PRIVATE)

        // Si el usuario tenía marcado "Recordar", NO borramos las credenciales
        // Si no estaba marcado, borramos todo
        val recordar = sharedPref.getBoolean("recordar", false)
        if (recordar) {
            // Mantener usuario y contraseña, solo limpiamos la sesión activa
            sharedPref.edit().apply {
                putBoolean("sesion_activa", false)
                // NO borrar "usuario", "contrasena", "recordar"
                apply()
            }
        } else {
            // Borrar todo
            sharedPref.edit().apply {
                remove("usuario")
                remove("contrasena")
                putBoolean("recordar", false)
                putBoolean("sesion_activa", false)
                apply()
            }
        }

        // Navegar al Login
        navController.navigate(R.id.nav_login)
        drawerLayout.closeDrawer(GravityCompat.START)
        Toast.makeText(this, "Sesión cerrada", Toast.LENGTH_SHORT).show()
    }
}
