package com.GrupoD.moneymanageristg

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.findNavController
import com.google.android.material.textfield.TextInputEditText

class RegisterFragment : Fragment() {

    private lateinit var etUser: TextInputEditText
    private lateinit var etEmail: TextInputEditText
    private lateinit var etPassword: TextInputEditText
    private lateinit var etConfirmPassword: TextInputEditText
    private lateinit var btnRegister: Button
    private lateinit var btnCancel: Button
    private lateinit var dbHelper: DatabaseHelper

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_register, container, false)

        // Inicializar DatabaseHelper
        dbHelper = DatabaseHelper(requireContext())

        // Referencias
        etUser = view.findViewById(R.id.et_user)
        etEmail = view.findViewById(R.id.et_email)
        etPassword = view.findViewById(R.id.et_password)
        etConfirmPassword = view.findViewById(R.id.et_confirm_password)
        btnRegister = view.findViewById(R.id.btn_register)
        btnCancel = view.findViewById(R.id.btn_cancel)

        // Listener Registrar
        btnRegister.setOnClickListener {
            registrarUsuario()
        }

        // Listener Cancelar
        btnCancel.setOnClickListener {
            // Navegar de vuelta al Login
            view.findNavController().navigate(R.id.nav_login)
        }

        return view
    }

    private fun registrarUsuario() {
        val user = etUser.text.toString().trim()
        val email = etEmail.text.toString().trim()
        val password = etPassword.text.toString().trim()
        val confirm = etConfirmPassword.text.toString().trim()

        // Validar campos
        if (user.isEmpty() || password.isEmpty() || confirm.isEmpty()) {
            Toast.makeText(requireContext(), "Completa todos los campos", Toast.LENGTH_SHORT).show()
            return
        }

        if (password != confirm) {
            Toast.makeText(requireContext(), "Las contraseñas no coinciden", Toast.LENGTH_SHORT).show()
            return
        }

        if (dbHelper.userExists(user)) {
            Toast.makeText(requireContext(), "El usuario ya existe", Toast.LENGTH_SHORT).show()
            return
        }

        // Guardar usuario
        val result = dbHelper.registerUser(user, email, password)
        if (result != -1L) {
            Toast.makeText(requireContext(), "Usuario registrado con éxito", Toast.LENGTH_SHORT).show()
            // Navegar al Login
            view?.findNavController()?.navigate(R.id.nav_login)
        } else {
            Toast.makeText(requireContext(), "Error al registrar usuario", Toast.LENGTH_SHORT).show()
        }
    }
}