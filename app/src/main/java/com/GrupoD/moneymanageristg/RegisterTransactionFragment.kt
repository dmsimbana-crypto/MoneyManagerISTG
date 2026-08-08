package com.GrupoD.moneymanageristg

import android.app.DatePickerDialog
import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.navigation.findNavController
import com.google.android.material.textfield.TextInputEditText
import java.util.*

class RegisterTransactionFragment : Fragment() {

    private lateinit var dbHelper: DatabaseHelper
    private lateinit var sharedPref: SharedPreferences

    // Componentes del layout
    private lateinit var rgTipo: RadioGroup
    private lateinit var etConcepto: TextInputEditText
    private lateinit var spinnerCategoria: Spinner
    private lateinit var spinnerSubcategoria: Spinner
    private lateinit var spinnerMedioPago: Spinner
    private lateinit var spinnerCuentaOrigen: Spinner
    private lateinit var spinnerCuentaDestino: Spinner
    private lateinit var etMonto: TextInputEditText
    private lateinit var etFecha: EditText
    private lateinit var btnGuardar: Button
    private lateinit var btnCancelar: Button

    // Listas de datos reales desde la BD
    private var categoriasList = mutableListOf<Pair<Long, String>>()
    private var subcategoriasList = mutableListOf<Pair<Long, String>>()
    private var mediosList = mutableListOf<Pair<Long, String>>()
    private var cuentasList = mutableListOf<Pair<Long, String>>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_register_transaction, container, false)

        dbHelper = DatabaseHelper(requireContext())
        sharedPref = requireActivity().getSharedPreferences("login_prefs", Context.MODE_PRIVATE)

        // Inicializar vistas
        rgTipo = view.findViewById(R.id.rg_tipo)
        etConcepto = view.findViewById(R.id.et_concepto)
        spinnerCategoria = view.findViewById(R.id.spinner_categoria)
        spinnerSubcategoria = view.findViewById(R.id.spinner_subcategoria)
        spinnerMedioPago = view.findViewById(R.id.spinner_medio_pago)
        spinnerCuentaOrigen = view.findViewById(R.id.spinner_cuenta_origen)
        spinnerCuentaDestino = view.findViewById(R.id.spinner_cuenta_destino)
        etMonto = view.findViewById(R.id.et_monto)
        etFecha = view.findViewById(R.id.et_fecha)
        btnGuardar = view.findViewById(R.id.btn_guardar)
        btnCancelar = view.findViewById(R.id.btn_cancelar)

        // Cargar datos desde la base de datos
        cargarCategorias()
        cargarMediosPago()
        cargarCuentas()

        // Configurar DatePicker
        etFecha.setOnClickListener { mostrarDatePicker() }
        etFecha.setText(obtenerFechaActual())

        // Listener para cambios en el tipo (mostrar/ocultar campos)
        rgTipo.setOnCheckedChangeListener { _, checkedId ->
            actualizarVisibilidadCampos(checkedId)
        }
        actualizarVisibilidadCampos(R.id.rb_egreso) // Egreso por defecto

        // Listener para cargar subcategorías al cambiar categoría
        spinnerCategoria.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                if (categoriasList.isNotEmpty()) {
                    cargarSubcategorias(categoriasList[position].first)
                }
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        // Botón Guardar
        btnGuardar.setOnClickListener { guardarTransaccion() }

        // Botón Cancelar
        btnCancelar.setOnClickListener { view.findNavController().navigateUp() }

        return view
    }

    // ============================================
    // MÉTODOS PARA CARGAR SPINNERS DESDE BD
    // ============================================

    private fun cargarCategorias() {
        categoriasList = dbHelper.getCategorias().toMutableList()
        val adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_item,
            categoriasList.map { it.second }
        )
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerCategoria.adapter = adapter
        if (categoriasList.isNotEmpty()) {
            cargarSubcategorias(categoriasList[0].first)
        }
    }

    private fun cargarSubcategorias(categoriaId: Long) {
        subcategoriasList = dbHelper.getSubcategorias(categoriaId).toMutableList()
        val adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_item,
            listOf("Ninguna") + subcategoriasList.map { it.second }
        )
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerSubcategoria.adapter = adapter
    }

    private fun cargarMediosPago() {
        mediosList = dbHelper.getMediosPago().toMutableList()
        val adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_item,
            listOf("Ninguno") + mediosList.map { it.second }
        )
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerMedioPago.adapter = adapter
    }

    private fun cargarCuentas() {
        val userId = dbHelper.getUserIdByUsername(
            sharedPref.getString("usuario", "admin") ?: "admin"
        )
        cuentasList = dbHelper.getCuentasByUser(userId).toMutableList()
        val adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_item,
            cuentasList.map { it.second }
        )
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerCuentaOrigen.adapter = adapter
        spinnerCuentaDestino.adapter = adapter
    }

    // ============================================
    // ACTUALIZAR VISIBILIDAD SEGÚN TIPO
    // ============================================

    private fun actualizarVisibilidadCampos(checkedId: Int) {
        val mostrarOrigen = checkedId == R.id.rb_egreso || checkedId == R.id.rb_traspaso
        val mostrarDestino = checkedId == R.id.rb_ingreso || checkedId == R.id.rb_traspaso

        view?.findViewById<TextView>(R.id.tv_cuenta_origen)?.visibility =
            if (mostrarOrigen) View.VISIBLE else View.GONE
        spinnerCuentaOrigen.visibility =
            if (mostrarOrigen) View.VISIBLE else View.GONE

        view?.findViewById<TextView>(R.id.tv_cuenta_destino)?.visibility =
            if (mostrarDestino) View.VISIBLE else View.GONE
        spinnerCuentaDestino.visibility =
            if (mostrarDestino) View.VISIBLE else View.GONE
    }

    // ============================================
    // MÉTODOS PARA FECHA
    // ============================================

    private fun obtenerFechaActual(): String {
        val calendar = Calendar.getInstance()
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH) + 1
        val day = calendar.get(Calendar.DAY_OF_MONTH)
        return "${day.toString().padStart(2, '0')}/${month.toString().padStart(2, '0')}/$year"
    }

    private fun mostrarDatePicker() {
        val calendar = Calendar.getInstance()
        DatePickerDialog(
            requireContext(),
            { _, year, month, day ->
                val fecha = "${day.toString().padStart(2, '0')}/${
                    (month + 1).toString().padStart(2, '0')
                }/$year"
                etFecha.setText(fecha)
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    // ============================================
    // GUARDAR TRANSACCIÓN
    // ============================================

    private fun guardarTransaccion() {
        val tipo = when (rgTipo.checkedRadioButtonId) {
            R.id.rb_ingreso -> "Ingreso"
            R.id.rb_egreso -> "Egreso"
            R.id.rb_traspaso -> "Traspaso"
            else -> "Egreso"
        }

        val concepto = etConcepto.text.toString().trim()
        val montoStr = etMonto.text.toString().trim()
        val fecha = etFecha.text.toString().trim()

        if (concepto.isEmpty() || montoStr.isEmpty() || fecha.isEmpty()) {
            Toast.makeText(requireContext(), "Completa todos los campos obligatorios", Toast.LENGTH_SHORT).show()
            return
        }

        val monto = montoStr.toDoubleOrNull()
        if (monto == null || monto <= 0) {
            Toast.makeText(requireContext(), "Monto inválido", Toast.LENGTH_SHORT).show()
            return
        }

        if (categoriasList.isEmpty()) {
            Toast.makeText(requireContext(), "No hay categorías disponibles", Toast.LENGTH_SHORT).show()
            return
        }

        val categoriaId = categoriasList[spinnerCategoria.selectedItemPosition].first
        val subcategoriaPos = spinnerSubcategoria.selectedItemPosition
        val subcategoriaId = if (subcategoriaPos > 0) {
            subcategoriasList[subcategoriaPos - 1].first
        } else null

        val medioPos = spinnerMedioPago.selectedItemPosition
        val medioId = if (medioPos > 0) {
            mediosList[medioPos - 1].first
        } else null

        val cuentaOrigenId = if (spinnerCuentaOrigen.visibility == View.VISIBLE && cuentasList.isNotEmpty()) {
            cuentasList[spinnerCuentaOrigen.selectedItemPosition].first
        } else null

        val cuentaDestinoId = if (spinnerCuentaDestino.visibility == View.VISIBLE && cuentasList.isNotEmpty()) {
            cuentasList[spinnerCuentaDestino.selectedItemPosition].first
        } else null

        val userId = dbHelper.getUserIdByUsername(
            sharedPref.getString("usuario", "admin") ?: "admin"
        )

        val success = dbHelper.insertTransaction(
            concepto = concepto,
            monto = monto,
            fecha = fecha,
            categoriaId = categoriaId,
            subcategoriaId = subcategoriaId,
            medioId = medioId,
            cuentaOrigenId = cuentaOrigenId,
            cuentaDestinoId = cuentaDestinoId,
            tipo = tipo,
            userId = userId
        )

        if (success) {
            Toast.makeText(requireContext(), "Transacción guardada", Toast.LENGTH_SHORT).show()
            view?.findNavController()?.navigateUp()
        } else {
            Toast.makeText(requireContext(), "Error al guardar", Toast.LENGTH_SHORT).show()
        }
    }
}