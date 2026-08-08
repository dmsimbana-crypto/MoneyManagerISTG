package com.GrupoD.moneymanageristg

import android.app.DatePickerDialog
import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.chip.Chip
import com.google.android.material.floatingactionbutton.FloatingActionButton
import java.util.Calendar

class HomeFragment : Fragment() {
    private lateinit var etFechaDesde: EditText
    private lateinit var etFechaHasta: EditText
    private var fechaDesde: String? = null
    private var fechaHasta: String? = null
    private lateinit var rvTransactions: RecyclerView
    private lateinit var fabAdd: FloatingActionButton
    private lateinit var adapter: TransactionAdapter
    private lateinit var dbHelper: DatabaseHelper
    private lateinit var sharedPref: SharedPreferences

    // Elementos del encabezado
    private lateinit var tvWelcome: TextView
    private lateinit var tvBudget: TextView
    private lateinit var tvSpent: TextView
    private lateinit var progressBudget: ProgressBar

    // Búsqueda y filtros
    private lateinit var etSearch: EditText
    private lateinit var chipAll: Chip
    private lateinit var chipFood: Chip
    private lateinit var chipTransport: Chip
    private lateinit var chipHigh: Chip

    // Variables para filtros
    private var allTransactions = listOf<Transaction>()
    private var currentFilter = "Todos" // Todos, Comida, Transporte, > $50

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_home, container, false)

        // Inicializar DatabaseHelper y SharedPreferences
        dbHelper = DatabaseHelper(requireContext())
        sharedPref = requireActivity().getSharedPreferences("login_prefs", Context.MODE_PRIVATE)

        // Referencias del layout
        rvTransactions = view.findViewById(R.id.rv_transactions)
        fabAdd = view.findViewById(R.id.fab_add)
        tvWelcome = view.findViewById(R.id.tv_welcome)
        tvBudget = view.findViewById(R.id.tv_budget)
        tvSpent = view.findViewById(R.id.tv_spent)
        progressBudget = view.findViewById(R.id.progress_budget)
        etSearch = view.findViewById(R.id.et_search)
        chipAll = view.findViewById(R.id.chip_all)
        chipFood = view.findViewById(R.id.chip_food)
        chipTransport = view.findViewById(R.id.chip_transport)
        chipHigh = view.findViewById(R.id.chip_high)
        etFechaDesde = view.findViewById(R.id.et_fecha_desde)
        etFechaHasta = view.findViewById(R.id.et_fecha_hasta)
        etFechaDesde.setOnClickListener {
            mostrarDatePicker { fecha ->
                etFechaDesde.setText(fecha)
                fechaDesde = fecha
                applyFilters()
            }
        }

        etFechaHasta.setOnClickListener {
            mostrarDatePicker { fecha ->
                etFechaHasta.setText(fecha)
                fechaHasta = fecha
                applyFilters()
            }
        }
        // Configurar RecyclerView
        rvTransactions.layoutManager = LinearLayoutManager(requireContext())
        adapter = TransactionAdapter(emptyList())
        rvTransactions.adapter = adapter

        // Obtener usuario logueado (desde SharedPreferences)
        val username = sharedPref.getString("usuario", "admin") ?: "admin"
        tvWelcome.text = "Bienvenido, $username"

        // Cargar datos
        loadTransactions(username)

        // Configurar búsqueda (filtra en tiempo real)
        etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                applyFilters()
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        // Configurar filtros rápidos
        chipAll.setOnClickListener {
            currentFilter = "Todos"
            // Limpiar filtros de fecha
            etFechaDesde.text?.clear()
            etFechaHasta.text?.clear()
            fechaDesde = null
            fechaHasta = null
            applyFilters()
            highlightChip(chipAll)
        }
        chipFood.setOnClickListener {
            currentFilter = "Comida"
            applyFilters()
            highlightChip(chipFood)
        }
        chipTransport.setOnClickListener {
            currentFilter = "Transporte"
            applyFilters()
            highlightChip(chipTransport)
        }
        chipHigh.setOnClickListener {
            currentFilter = "> $50"
            applyFilters()
            highlightChip(chipHigh)
        }

        // FAB: navegar a Registrar Transacción (más adelante)
        fabAdd.setOnClickListener {
            Toast.makeText(requireContext(), "Agregar transacción", Toast.LENGTH_SHORT).show()
        }

        return view
    }

    // ============================================
    // Cargar transacciones desde SQLite
    // ============================================
    private fun loadTransactions(username: String) {
        etSearch.setText("")
        currentFilter = "Todos"
        highlightChip(chipAll)
        val userId = dbHelper.getUserIdByUsername(username)
        if (userId == -1L) {
            Toast.makeText(requireContext(), "Usuario no encontrado", Toast.LENGTH_SHORT).show()
            return
        }

        // Obtener transacciones
        allTransactions = dbHelper.getTransactions(userId)
        adapter.updateData(allTransactions)

        // Calcular y mostrar resumen
        updateSummary(userId)
    }
    // Muestra un DatePicker y devuelve la fecha en formato "dd/MM/yyyy"
    private fun mostrarDatePicker(onDateSelected: (String) -> Unit) {
        val calendar = Calendar.getInstance()
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val day = calendar.get(Calendar.DAY_OF_MONTH)

        val datePicker = DatePickerDialog(
            requireContext(),
            { _, selectedYear, selectedMonth, selectedDay ->
                val fecha = "${selectedDay.toString().padStart(2, '0')}/${
                    (selectedMonth + 1).toString().padStart(2, '0')
                }/$selectedYear"
                onDateSelected(fecha)
            },
            year, month, day
        )
        datePicker.show()
    }
    // ============================================
    // Actualizar resumen (presupuesto, gastado, porcentaje)
    // ============================================
    private fun updateSummary(userId: Long) {
        // Leer presupuesto desde SharedPreferences (por defecto 500)
        val budget = sharedPref.getFloat("presupuesto_mensual", 500.0f).toDouble()
        tvBudget.text = "Presupuesto: $${String.format("%.2f", budget)}"

        // Obtener total gastado del mes
        val spent = dbHelper.getMonthlySpent(userId)
        tvSpent.text = "Gastado este mes: $${String.format("%.2f", spent)} (${String.format("%.0f", (spent / budget) * 100)}%)"

        // Actualizar barra de progreso
        val progress = ((spent / budget) * 100).toInt()
        progressBudget.max = 100
        progressBudget.progress = if (progress > 100) 100 else progress

        // Cambiar color de la barra según el porcentaje
        when {
            progress > 80 -> progressBudget.progressTintList = ContextCompat.getColorStateList(requireContext(), R.color.red)
            progress > 50 -> progressBudget.progressTintList = ContextCompat.getColorStateList(requireContext(), R.color.yellow)
            else -> progressBudget.progressTintList = ContextCompat.getColorStateList(requireContext(), R.color.green)
        }
    }

    // ============================================
    // Aplicar filtros (búsqueda + filtros rápidos)
    // ============================================
    private fun applyFilters() {
        val searchText = etSearch.text.toString().trim().lowercase()
        var filteredList = allTransactions

        // 1. Filtro por búsqueda (concepto o categoría)
        if (searchText.isNotEmpty()) {
            filteredList = filteredList.filter {
                it.concepto.lowercase().contains(searchText) ||
                        it.categoria.lowercase().contains(searchText) ||
                        (it.subcategoria?.lowercase()?.contains(searchText) ?: false)
            }
        }
// 3. Filtro por rango de fechas
        fechaDesde?.let { desde ->
            // Convertir "dd/MM/yyyy" a "yyyy-MM-dd" para comparar con SQLite
            val desdeFormat = desde.split("/").reversed().joinToString("-")
            filteredList = filteredList.filter { it.fecha >= desdeFormat }
        }
        fechaHasta?.let { hasta ->
            val hastaFormat = hasta.split("/").reversed().joinToString("-")
            filteredList = filteredList.filter { it.fecha <= hastaFormat }
        }
        // 2. Filtro rápido por chip
        when (currentFilter) {
            "Comida" -> filteredList = filteredList.filter { it.categoria.lowercase() == "alimentación" || it.categoria.lowercase() == "comida" }
            "Transporte" -> filteredList = filteredList.filter { it.categoria.lowercase() == "transporte" }
            "> $50" -> filteredList = filteredList.filter { it.monto > 50 }
            else -> { /* Todos */ }
        }

        // Actualizar adapter
        adapter.updateData(filteredList)
    }

    // ============================================
    // Resaltar el chip seleccionado
    // ============================================
    private fun highlightChip(selectedChip: Chip) {
        val chips = listOf(chipAll, chipFood, chipTransport, chipHigh)
        chips.forEach { chip ->
            chip.isChecked = chip == selectedChip
        }
    }
    override fun onResume() {
        super.onResume()
        val username = sharedPref.getString("usuario", "admin") ?: "admin"
        loadTransactions(username)
    }
}