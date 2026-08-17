package com.GrupoD.moneymanageristg

import android.app.DatePickerDialog
import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.navigation.findNavController
import com.google.android.material.textfield.TextInputEditText
import java.util.*

class RegisterTransactionFragment : Fragment() {

    private var isEditing = false
    private lateinit var rbIngreso: RadioButton
    private lateinit var rbEgreso: RadioButton
    private lateinit var rbTraspaso: RadioButton
    private var editingTransactionId: Long = -1
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
        arguments?.let {
            editingTransactionId = it.getLong("id_transaccion", -1)
        }
        sharedPref = requireActivity().getSharedPreferences("login_prefs", Context.MODE_PRIVATE)

        // Inicializar vistas
        rgTipo = view.findViewById(R.id.rg_tipo)
        rbIngreso = view.findViewById(R.id.rb_ingreso)
        rbEgreso = view.findViewById(R.id.rb_egreso)
        rbTraspaso = view.findViewById(R.id.rb_traspaso)
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
                // Solo cargar subcategorías si NO estamos en modo edición
                if (!isEditing && categoriasList.isNotEmpty()) {
                    cargarSubcategorias(categoriasList[position].first)
                }
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
        if (editingTransactionId != -1L) {
            cargarDatosParaEdicion()
        }
        // Botón Guardar
        btnGuardar.setOnClickListener { guardarTransaccion() }

        // Botón Cancelar
        btnCancelar.setOnClickListener { view.findNavController().navigateUp() }

        return view
    }
    private fun cargarDatosParaEdicion() {
        isEditing = true  // <--- BLOQUEAR EL LISTENER
        Log.d("EditarTransaccion", "=== INICIO cargarDatosParaEdicion ===")

        val userId = dbHelper.getUserIdByUsername(
            sharedPref.getString("usuario", "admin") ?: "admin"
        )
        val transactions = dbHelper.getTransactions(userId)
        val transaction = transactions.find { it.id == editingTransactionId }

        if (transaction == null) {
            Toast.makeText(requireContext(), "Transacción no encontrada", Toast.LENGTH_SHORT).show()
            isEditing = false
            return
        }

        Log.d("EditarTransaccion", "Transacción: id=${transaction.id}, tipo=${transaction.tipo}, categoria=${transaction.categoria}, subcategoria=${transaction.subcategoria}")

        // 1. Precargar campos básicos
        etConcepto.setText(transaction.concepto)
        etMonto.setText(transaction.monto.toString())

        // 2. Fecha
        val fechaParts = transaction.fecha.split("-")
        if (fechaParts.size == 3) {
            val fechaFormateada = "${fechaParts[2]}/${fechaParts[1]}/${fechaParts[0]}"
            etFecha.setText(fechaFormateada)
        }

        // 3. Tipo
        when (transaction.tipo) {
            "Ingreso" -> rbIngreso.isChecked = true
            "Egreso" -> rbEgreso.isChecked = true
            "Traspaso" -> rbTraspaso.isChecked = true
        }
        actualizarVisibilidadCampos(rgTipo.checkedRadioButtonId)

        // 4. Seleccionar categoría
        val categoriaId = getCategoriaId(transaction.categoria)
        Log.d("EditarTransaccion", "Buscando categoría: '${transaction.categoria}' -> ID: $categoriaId")

        val categoriaPos = categoriasList.indexOfFirst { it.first == categoriaId }
        Log.d("EditarTransaccion", "categoriasList: ${categoriasList.map { it.second }}")
        Log.d("EditarTransaccion", "categoriaPos: $categoriaPos")

        if (categoriaPos >= 0) {
            spinnerCategoria.setSelection(categoriaPos)
            Log.d("EditarTransaccion", "Categoría seleccionada en posición: $categoriaPos")

            // 5. Cargar subcategorías MANUALMENTE (solo una vez)
            cargarSubcategorias(categoriasList[categoriaPos].first)
            Log.d("EditarTransaccion", "subcategoriasList después de cargar: ${subcategoriasList.map { it.second }}")

            // 6. Seleccionar subcategoría con postDelayed
            spinnerSubcategoria.postDelayed({
                Log.d("EditarTransaccion", "Ejecutando post para seleccionar subcategoría")
                if (!transaction.subcategoria.isNullOrEmpty()) {
                    val subPos = subcategoriasList.indexOfFirst { it.second == transaction.subcategoria }
                    Log.d("EditarTransaccion", "Buscando subcategoría: '${transaction.subcategoria}' -> posición en lista (sin contar 'Ninguna'): $subPos")
                    if (subPos >= 0) {
                        val positionToSet = subPos + 1
                        Log.d("EditarTransaccion", "Estableciendo spinnerSubcategoria en posición: $positionToSet")
                        spinnerSubcategoria.setSelection(positionToSet, false)
                    } else {
                        Log.d("EditarTransaccion", "Subcategoría NO encontrada, seleccionando 'Ninguna'")
                        spinnerSubcategoria.setSelection(0)
                    }
                } else {
                    Log.d("EditarTransaccion", "La transacción NO tiene subcategoría, seleccionando 'Ninguna'")
                    spinnerSubcategoria.setSelection(0)
                }
            }, 100)
        } else {
            Log.d("EditarTransaccion", "Categoría NO encontrada en la lista")
        }

        // 7. Seleccionar medio de pago
        val medioPos = mediosList.indexOfFirst { it.second == transaction.medioPago }
        Log.d("EditarTransaccion", "Buscando medio de pago: '${transaction.medioPago}' -> posición: $medioPos")
        if (medioPos >= 0) {
            spinnerMedioPago.setSelection(medioPos + 1)
        }

        // 8. Cuentas (pendiente)
        Log.d("EditarTransaccion", "=== FIN cargarDatosParaEdicion ===")

        isEditing = false  // <--- DESBLOQUEAR EL LISTENER
    }
    // Método auxiliar para obtener ID de categoría por nombre
    private fun getCategoriaId(nombre: String): Long {
        return categoriasList.firstOrNull { it.second == nombre }?.first ?: -1
    }

    // MÉTODOS PARA CARGAR SPINNERS DESDE BD

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
        Log.d("EditarTransaccion", "cargarSubcategorias para categoriaId: $categoriaId")
        subcategoriasList = dbHelper.getSubcategorias(categoriaId).toMutableList()
        Log.d("EditarTransaccion", "subcategoriasList obtenida: ${subcategoriasList.map { it.second }}")
        val adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_item,
            listOf("Ninguna") + subcategoriasList.map { it.second }
        )
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerSubcategoria.adapter = adapter
        Log.d("EditarTransaccion", "Adaptador de subcategorías actualizado")
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


    // ACTUALIZAR VISIBILIDAD SEGÚN TIPO

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

    // MÉTODOS PARA FECHA

    private fun obtenerFechaActual(): String {
        val calendar = Calendar.getInstance()
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH) + 1
        val day = calendar.get(Calendar.DAY_OF_MONTH)
        // Si el día es 0, corregimos a 1
        val diaCorrecto = if (day == 0) 1 else day
        return "${diaCorrecto.toString().padStart(2, '0')}/${month.toString().padStart(2, '0')}/$year"
    }
    private fun mostrarDatePicker() {
        // Obtener fecha actual sin ajuste horario usando Calendar
        val calendar = Calendar.getInstance()
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val day = calendar.get(Calendar.DAY_OF_MONTH)

        // Si el día es 0, significa que Calendar se fue al mes anterior, lo corregimos
        val diaCorrecto = if (day == 0) 1 else day
        val mesCorrecto = if (day == 0) month + 1 else month

        DatePickerDialog(
            requireContext(),
            { _, selectedYear, selectedMonth, selectedDay ->
                val fecha = "${selectedDay.toString().padStart(2, '0')}/${
                    (selectedMonth + 1).toString().padStart(2, '0')
                }/$selectedYear"
                etFecha.setText(fecha)
            },
            year, mesCorrecto, diaCorrecto
        ).show()
    }

    // GUARDAR TRANSACCIÓN

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
        val subcategoriaId = if (subcategoriaPos > 0 && subcategoriasList.isNotEmpty()) {
            subcategoriasList[subcategoriaPos - 1].first
        } else {
            null
        }

        val medioPos = spinnerMedioPago.selectedItemPosition
        val medioId = if (medioPos > 0 && mediosList.isNotEmpty()) {
            mediosList[medioPos - 1].first
        } else {
            null
        }

        val cuentaOrigenId = if (spinnerCuentaOrigen.visibility == View.VISIBLE && cuentasList.isNotEmpty()) {
            cuentasList[spinnerCuentaOrigen.selectedItemPosition].first
        } else {
            null
        }

        val cuentaDestinoId = if (spinnerCuentaDestino.visibility == View.VISIBLE && cuentasList.isNotEmpty()) {
            cuentasList[spinnerCuentaDestino.selectedItemPosition].first
        } else {
            null
        }

        val userId = dbHelper.getUserIdByUsername(
            sharedPref.getString("usuario", "admin") ?: "admin"
        )

        val fechaBD = convertirFechaParaBD(fecha)

        // LOGS PARA DEPURAR
        Log.d("Guardar", "Concepto: $concepto")
        Log.d("Guardar", "Monto: $monto")
        Log.d("Guardar", "FechaBD: $fechaBD")
        Log.d("Guardar", "CategoriaId: $categoriaId")
        Log.d("Guardar", "SubcategoriaId: $subcategoriaId")
        Log.d("Guardar", "MedioId: $medioId")
        Log.d("Guardar", "CuentaOrigenId: $cuentaOrigenId")
        Log.d("Guardar", "CuentaDestinoId: $cuentaDestinoId")
        Log.d("Guardar", "Tipo: $tipo")
        Log.d("Guardar", "UserId: $userId")

        val success = if (editingTransactionId == -1L) {
            dbHelper.insertTransaction(
                concepto = concepto,
                monto = monto,
                fecha = fechaBD,
                categoriaId = categoriaId,
                subcategoriaId = subcategoriaId,
                medioId = medioId,
                cuentaOrigenId = cuentaOrigenId,
                cuentaDestinoId = cuentaDestinoId,
                tipo = tipo,
                userId = userId
            )
        } else {
            dbHelper.updateTransaction(
                transactionId = editingTransactionId,
                concepto = concepto,
                monto = monto,
                fecha = fechaBD, // USAMOS FECHABD
                categoriaId = categoriaId,
                subcategoriaId = subcategoriaId,
                medioId = medioId,
                cuentaOrigenId = cuentaOrigenId,
                cuentaDestinoId = cuentaDestinoId,
                tipo = tipo,
                userId = userId
            )
        }

        if (success) {
            Toast.makeText(requireContext(), "Transacción guardada", Toast.LENGTH_SHORT).show()
            view?.findNavController()?.navigateUp()
        } else {
            Toast.makeText(requireContext(), "Error al guardar", Toast.LENGTH_SHORT).show()
        }
    }


    private fun convertirFechaParaBD(fechaStr: String): String {
        // fechaStr viene en "dd/MM/yyyy"
        val parts = fechaStr.split("/")
        return "${parts[2]}-${parts[1].padStart(2, '0')}-${parts[0].padStart(2, '0')}"
    }
}