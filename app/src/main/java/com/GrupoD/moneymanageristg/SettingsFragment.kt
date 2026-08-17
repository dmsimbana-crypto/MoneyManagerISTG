package com.GrupoD.moneymanageristg

import android.app.AlertDialog
import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.text.InputType
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.navigation.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class SettingsFragment : Fragment() {

    private lateinit var dbHelper: DatabaseHelper
    private lateinit var sharedPref: SharedPreferences

    // Componentes del layout
    private lateinit var etPresupuesto: EditText
    private lateinit var spinnerMoneda: Spinner
    private lateinit var btnGuardarConfig: Button
    private lateinit var rvCuentas: RecyclerView
    private lateinit var btnAgregarCuenta: Button
    private lateinit var btnCerrarSesion: Button

    private lateinit var cuentasAdapter: CuentaAdapter
    private var cuentasList = mutableListOf<Cuenta>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_settings, container, false)

        dbHelper = DatabaseHelper(requireContext())
        sharedPref = requireActivity().getSharedPreferences("login_prefs", Context.MODE_PRIVATE)

        // Inicializar vistas
        etPresupuesto = view.findViewById(R.id.et_presupuesto)
        spinnerMoneda = view.findViewById(R.id.spinner_moneda)
        btnGuardarConfig = view.findViewById(R.id.btn_guardar_configuracion)
        rvCuentas = view.findViewById(R.id.rv_cuentas)
        btnAgregarCuenta = view.findViewById(R.id.btn_agregar_cuenta)
        btnCerrarSesion = view.findViewById(R.id.btn_cerrar_sesion)

        // Configurar RecyclerView de cuentas
        rvCuentas.layoutManager = LinearLayoutManager(requireContext())
        cuentasAdapter = CuentaAdapter(emptyList(), object : CuentaAdapter.OnCuentaActionListener {
            override fun onEditCuenta(cuentaId: Long, currentName: String, currentSaldo: Double) {
                mostrarDialogoEditarCuenta(cuentaId, currentName, currentSaldo)
            }

            override fun onDeleteCuenta(cuentaId: Long) {
                mostrarDialogoEliminarCuenta(cuentaId)
            }
        })
        rvCuentas.adapter = cuentasAdapter

        // Configurar Spinner de monedas
        val monedas = arrayOf("USD", "EUR", "COP", "MXN", "ARS", "PEN")
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, monedas)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerMoneda.adapter = adapter

        // Cargar datos guardados
        cargarConfiguracion()
        cargarCuentas()

        // Listeners
        btnGuardarConfig.setOnClickListener {
            guardarConfiguracion()
        }

        btnAgregarCuenta.setOnClickListener {
            mostrarDialogoAgregarCuenta()
        }

        btnCerrarSesion.setOnClickListener {
            val sharedPref = requireActivity().getSharedPreferences("login_prefs", Context.MODE_PRIVATE)
            val recordar = sharedPref.getBoolean("recordar", false)
            if (recordar) {
                sharedPref.edit().apply {
                    putBoolean("sesion_activa", false)
                    apply()
                }
            } else {
                sharedPref.edit().apply {
                    remove("usuario")
                    remove("contrasena")
                    putBoolean("recordar", false)
                    putBoolean("sesion_activa", false)
                    apply()
                }
            }
            view?.findNavController()?.navigate(R.id.nav_login)
            Toast.makeText(requireContext(), "Sesión cerrada", Toast.LENGTH_SHORT).show()
        }

        return view
    }

    // CONFIGURACIÓN (Presupuesto y Moneda)

    private fun cargarConfiguracion() {
        val username = sharedPref.getString("usuario", "admin") ?: "admin"
        val presupuesto = sharedPref.getFloat("${username}_presupuesto_mensual", 500.0f)
        etPresupuesto.setText(String.format("%.2f", presupuesto))

        val moneda = sharedPref.getString("${username}_moneda_base", "USD") ?: "USD"
        val position = (0 until spinnerMoneda.count).firstOrNull { spinnerMoneda.getItemAtPosition(it) == moneda } ?: 0
        spinnerMoneda.setSelection(position)
    }

    private fun guardarConfiguracion() {
        val presupuestoStr = etPresupuesto.text.toString().trim()
        if (presupuestoStr.isEmpty()) {
            Toast.makeText(requireContext(), "Ingresa un presupuesto válido", Toast.LENGTH_SHORT).show()
            return
        }
        val presupuesto = presupuestoStr.toDoubleOrNull()
        if (presupuesto == null || presupuesto <= 0) {
            Toast.makeText(requireContext(), "El presupuesto debe ser mayor a 0", Toast.LENGTH_SHORT).show()
            return
        }

        val moneda = spinnerMoneda.selectedItem.toString()
        val username = sharedPref.getString("usuario", "admin") ?: "admin"

        // Guardar con clave única por usuario
        sharedPref.edit().apply {
            putFloat("${username}_presupuesto_mensual", presupuesto.toFloat())
            putString("${username}_moneda_base", moneda)
            apply()
        }

        Toast.makeText(requireContext(), "Configuración guardada para $username", Toast.LENGTH_SHORT).show()
    }

    // GESTIÓN DE CUENTAS

    private fun cargarCuentas() {
        val userId = dbHelper.getUserIdByUsername(
            sharedPref.getString("usuario", "admin") ?: "admin"
        )
        // Obtener cuentas desde la base de datos con su saldo
        val cuentas = dbHelper.getCuentasByUser(userId).map { (id, nombre) ->
            // Obtener el saldo real desde la base de datos
            val saldo = dbHelper.obtenerSaldoCuenta(id)
            Cuenta(id, nombre, saldo)
        }
        cuentasList.clear()
        cuentasList.addAll(cuentas)
        cuentasAdapter.updateData(cuentasList)
    }

    private fun mostrarDialogoAgregarCuenta() {
        val context = requireContext()
        val inflater = LayoutInflater.from(context)
        val dialogView = inflater.inflate(R.layout.dialog_cuenta, null)
        val etNombre = dialogView.findViewById<EditText>(R.id.et_cuenta_nombre)
        val etSaldo = dialogView.findViewById<EditText>(R.id.et_cuenta_saldo)
        etSaldo.hint = "Saldo inicial (opcional)"
        etSaldo.inputType = InputType.TYPE_NUMBER_FLAG_DECIMAL

        AlertDialog.Builder(context)
            .setTitle("Agregar cuenta")
            .setView(dialogView)
            .setPositiveButton("Guardar") { _, _ ->
                val nombre = etNombre.text.toString().trim()
                val saldoStr = etSaldo.text.toString().trim()
                val saldo = if (saldoStr.isNotEmpty()) saldoStr.toDoubleOrNull() ?: 0.0 else 0.0

                if (nombre.isNotEmpty()) {
                    val userId = dbHelper.getUserIdByUsername(
                        sharedPref.getString("usuario", "admin") ?: "admin"
                    )
                    val success = dbHelper.insertCuentaConSaldo(nombre, saldo, userId)
                    if (success) {
                        Toast.makeText(context, "Cuenta agregada", Toast.LENGTH_SHORT).show()
                        cargarCuentas()
                    } else {
                        Toast.makeText(context, "Error o nombre duplicado", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(context, "Ingresa un nombre", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun mostrarDialogoEditarCuenta(cuentaId: Long, currentName: String, currentSaldo: Double) {
        val context = requireContext()
        val inflater = LayoutInflater.from(context)
        val dialogView = inflater.inflate(R.layout.dialog_cuenta, null)
        val etNombre = dialogView.findViewById<EditText>(R.id.et_cuenta_nombre)
        val etSaldo = dialogView.findViewById<EditText>(R.id.et_cuenta_saldo)
        etNombre.setText(currentName)
        etSaldo.setText(String.format("%.2f", currentSaldo))
        etSaldo.inputType = InputType.TYPE_NUMBER_FLAG_DECIMAL

        AlertDialog.Builder(context)
            .setTitle("Editar cuenta")
            .setView(dialogView)
            .setPositiveButton("Actualizar") { _, _ ->
                val nuevoNombre = etNombre.text.toString().trim()
                val saldoStr = etSaldo.text.toString().trim()
                val nuevoSaldo = if (saldoStr.isNotEmpty()) saldoStr.toDoubleOrNull() ?: currentSaldo else currentSaldo

                if (nuevoNombre.isNotEmpty()) {
                    val success = dbHelper.updateCuentaConSaldo(cuentaId, nuevoNombre, nuevoSaldo)
                    if (success) {
                        Toast.makeText(context, "Cuenta actualizada", Toast.LENGTH_SHORT).show()
                        cargarCuentas()
                    } else {
                        Toast.makeText(context, "Error o nombre duplicado", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(context, "Ingresa un nombre válido", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun mostrarDialogoEliminarCuenta(cuentaId: Long) {
        AlertDialog.Builder(requireContext())
            .setTitle("Eliminar cuenta")
            .setMessage("¿Estás seguro de que deseas eliminar esta cuenta?")
            .setPositiveButton("Eliminar") { _, _ ->
                val success = dbHelper.deleteCuenta(cuentaId)
                if (success) {
                    Toast.makeText(requireContext(), "Cuenta eliminada", Toast.LENGTH_SHORT).show()
                    cargarCuentas()
                } else {
                    Toast.makeText(requireContext(), "Error al eliminar (tiene transacciones)", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }


    // CERRAR SESIÓN

    private fun cerrarSesion() {
        AlertDialog.Builder(requireContext())
            .setTitle("Cerrar sesión")
            .setMessage("¿Estás seguro de que deseas cerrar sesión?")
            .setPositiveButton("Cerrar sesión") { _, _ ->
                sharedPref.edit().apply {
                    remove("usuario")
                    remove("contrasena")
                    putBoolean("recordar", false)
                    apply()
                }
                view?.findNavController()?.navigate(R.id.nav_login)
                Toast.makeText(requireContext(), "Sesión cerrada", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }
}

// DATA CLASS Y ADAPTADOR PARA CUENTAS

data class Cuenta(val id: Long, val nombre: String, var saldo: Double)

class CuentaAdapter(
    private var cuentas: List<Cuenta>,
    private val listener: OnCuentaActionListener
) : RecyclerView.Adapter<CuentaAdapter.CuentaViewHolder>() {

    interface OnCuentaActionListener {
        fun onEditCuenta(cuentaId: Long, currentName: String, currentSaldo: Double)
        fun onDeleteCuenta(cuentaId: Long)
    }

    class CuentaViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvNombre: TextView = itemView.findViewById(R.id.tv_cuenta_nombre)
        val tvSaldo: TextView = itemView.findViewById(R.id.tv_cuenta_saldo)
        val btnEditar: ImageButton = itemView.findViewById(R.id.btn_editar_cuenta)
        val btnEliminar: ImageButton = itemView.findViewById(R.id.btn_eliminar_cuenta)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CuentaViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_cuenta, parent, false)
        return CuentaViewHolder(view)
    }

    override fun onBindViewHolder(holder: CuentaViewHolder, position: Int) {
        val cuenta = cuentas[position]
        holder.tvNombre.text = cuenta.nombre
        holder.tvSaldo.text = "Saldo: $${String.format("%.2f", cuenta.saldo)}"

        holder.btnEditar.setOnClickListener {
            listener.onEditCuenta(cuenta.id, cuenta.nombre, cuenta.saldo)
        }

        holder.btnEliminar.setOnClickListener {
            listener.onDeleteCuenta(cuenta.id)
        }
    }

    override fun getItemCount(): Int = cuentas.size

    fun updateData(newCuentas: List<Cuenta>) {
        cuentas = newCuentas
        notifyDataSetChanged()
    }
}