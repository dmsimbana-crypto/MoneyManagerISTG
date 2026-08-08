package com.GrupoD.moneymanageristg
import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.CheckBox
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.findNavController
import com.google.android.material.textfield.TextInputEditText

class LoginFragment : Fragment() {

    private lateinit var sharedPref: SharedPreferences
    private lateinit var etUser: TextInputEditText
    private lateinit var etPassword: TextInputEditText
    private lateinit var chkRemember: CheckBox
    private lateinit var btnLogin: Button
    private lateinit var tvRegister: TextView

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_login, container, false)

        // Inicializar SharedPreferences
        sharedPref = requireActivity().getSharedPreferences("login_prefs", Context.MODE_PRIVATE)

        // Referencias a los elementos
        etUser = view.findViewById(R.id.et_user)
        etPassword = view.findViewById(R.id.et_password)
        chkRemember = view.findViewById(R.id.chk_remember)
        btnLogin = view.findViewById(R.id.btn_login)
        tvRegister = view.findViewById(R.id.tv_register)

        // Cargar credenciales guardadas si existen
        cargarCredenciales()

        // Listener del botón Login
        btnLogin.setOnClickListener {
            validarLogin()
        }

        // Listener para el enlace "Regístrate"
        tvRegister.setOnClickListener {
            view?.findNavController()?.navigate(R.id.action_login_to_register)   }

        return view
    }

    private fun cargarCredenciales() {
        val user = sharedPref.getString("usuario", "")
        val pass = sharedPref.getString("contrasena", "")
        val remember = sharedPref.getBoolean("recordar", false)

        if (remember && user != null && pass != null) {
            etUser.setText(user)
            etPassword.setText(pass)
            chkRemember.isChecked = true
        }
    }

    private fun validarLogin() {
        val user = etUser.text.toString().trim()
        val pass = etPassword.text.toString().trim()

        if (user.isEmpty() || pass.isEmpty()) {
            Toast.makeText(requireContext(), "Completa todos los campos", Toast.LENGTH_SHORT).show()
            return
        }

        // Validación simple (usuario fijo admin/1234)
        // Más adelante conectaremos con SQLite
        if (user == "admin" && pass == "1234") {
            // Guardar credenciales si el CheckBox está marcado
            if (chkRemember.isChecked) {
                sharedPref.edit().apply {
                    putString("usuario", user)
                    putString("contrasena", pass)
                    putBoolean("recordar", true)
                    apply()
                }
            } else {
                // Limpiar credenciales si no se marcó
                sharedPref.edit().apply {
                    remove("usuario")
                    remove("contrasena")
                    putBoolean("recordar", false)
                    apply()
                }
            }

            // Navegar al Home (nav_home)
            view?.findNavController()?.navigate(R.id.nav_home)
            Toast.makeText(requireContext(), "Bienvenido $user", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(requireContext(), "Credenciales incorrectas", Toast.LENGTH_SHORT).show()
        }
    }
}