package com.GrupoD.moneymanageristg

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.findNavController
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

class TransactionDetailFragment : Fragment() {

    private lateinit var dbHelper: DatabaseHelper
    private lateinit var sharedPref: SharedPreferences
    private var transactionId: Long = -1

    // Componentes del layout
    private lateinit var tvConcepto: TextView
    private lateinit var tvCategoria: TextView
    private lateinit var tvSubcategoria: TextView
    private lateinit var tvMonto: TextView
    private lateinit var tvTipo: TextView
    private lateinit var tvMedioPago: TextView
    private lateinit var tvCuentaOrigen: TextView
    private lateinit var tvCuentaDestino: TextView
    private lateinit var tvFecha: TextView
    private lateinit var tvMontoConvertido: TextView
    private lateinit var tvTasa: TextView
    private lateinit var tvPorcentaje: TextView
    private lateinit var tvFrasePoder: TextView
    private lateinit var btnEditar: Button
    private lateinit var btnEliminar: Button
    private lateinit var btnVolver: Button

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_transaction_detail, container, false)

        dbHelper = DatabaseHelper(requireContext())
        sharedPref = requireActivity().getSharedPreferences("login_prefs", Context.MODE_PRIVATE)

        // Obtener ID de la transacción desde los argumentos
        arguments?.let {
            transactionId = it.getLong("id_transaccion", -1)
        }

        if (transactionId == -1L) {
            Toast.makeText(requireContext(), "Error: ID de transacción no válido", Toast.LENGTH_SHORT).show()
            view.findNavController().navigateUp()
            return view
        }

        // Inicializar vistas
        tvConcepto = view.findViewById(R.id.tv_concepto)
        tvCategoria = view.findViewById(R.id.tv_categoria)
        tvSubcategoria = view.findViewById(R.id.tv_subcategoria)
        tvMonto = view.findViewById(R.id.tv_monto)
        tvTipo = view.findViewById(R.id.tv_tipo)
        tvMedioPago = view.findViewById(R.id.tv_medio_pago)
        tvCuentaOrigen = view.findViewById(R.id.tv_cuenta_origen)
        tvCuentaDestino = view.findViewById(R.id.tv_cuenta_destino)
        tvFecha = view.findViewById(R.id.tv_fecha)
        tvMontoConvertido = view.findViewById(R.id.tv_monto_convertido)
        tvTasa = view.findViewById(R.id.tv_tasa)
        tvPorcentaje = view.findViewById(R.id.tv_porcentaje)
        tvFrasePoder = view.findViewById(R.id.tv_frase_poder)
        btnEditar = view.findViewById(R.id.btn_editar)
        btnEliminar = view.findViewById(R.id.btn_eliminar)
        btnVolver = view.findViewById(R.id.btn_volver)

        // Cargar datos de la transacción
        cargarDetalle()

        // Listeners
        btnVolver.setOnClickListener {
            view.findNavController().navigateUp()
        }

        btnEditar.setOnClickListener {
            val bundle = Bundle().apply {
                putLong("id_transaccion", transactionId)
            }
            view?.findNavController()?.navigate(
                R.id.action_nav_transaction_detail_to_nav_register_transaction,
                bundle
            )
        }

        btnEliminar.setOnClickListener {
            mostrarDialogoConfirmacion()
        }

        return view
    }

    private fun cargarDetalle() {
        val username = sharedPref.getString("usuario", "admin") ?: "admin"
        val userId = dbHelper.getUserIdByUsername(username)

        // Obtener todas las transacciones y filtrar por ID
        val transactions = dbHelper.getTransactions(userId)
        val transaction = transactions.find { it.id == transactionId }

        if (transaction == null) {
            Toast.makeText(requireContext(), "Transacción no encontrada", Toast.LENGTH_SHORT).show()
            view?.findNavController()?.navigateUp()
            return
        }



        //Mostrar datos básicos
        tvConcepto.text = transaction.concepto
        tvCategoria.text = transaction.categoria
        tvSubcategoria.text = transaction.subcategoria ?: "Ninguna"
        tvMonto.text = "$${String.format("%.2f", transaction.monto)}"
        tvTipo.text = transaction.tipo
        tvMedioPago.text = transaction.medioPago ?: "No especificado"

        // Ajustar origen/destino según el tipo
        tvCuentaOrigen.text = when (transaction.tipo) {
            "Ingreso" -> "No aplica"
            else -> transaction.cuenta ?: "No especificada"
        }
        tvCuentaDestino.text = when (transaction.tipo) {
            "Egreso" -> "No aplica"
            else -> transaction.cuenta ?: "No especificada"
        }
        tvFecha.text = transaction.fecha

        // Calcular porcentaje del presupuesto (personalizado por usuario)
        val budgetKey = "${username}_presupuesto_mensual"
        val budget = sharedPref.getFloat(budgetKey, 500.0f).toDouble()

        val porcentaje = (transaction.monto / budget) * 100
        val restante = budget - transaction.monto
        tvPorcentaje.text = """
        Este gasto consume el ${String.format("%.1f", porcentaje)}% de tu presupuesto 
        ($${String.format("%.2f", budget)}). Te quedan $${String.format("%.2f", restante)} 
        para el resto del mes.
    """.trimIndent()

        //  Frase de poder adquisitivo (se actualizará después de la API)
        tvFrasePoder.text = "Obteniendo tasa de cambio..."

        // Llamar a la API para obtener la tasa de cambio
        obtenerTasaCambio(transaction.monto)
    }



    private fun mostrarDialogoConfirmacion() {
        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle("Eliminar transacción")
            .setMessage("¿Estás seguro de que deseas eliminar esta transacción? Esta acción no se puede deshacer.")
            .setPositiveButton("Eliminar") { _, _ ->
                eliminarTransaccion()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun eliminarTransaccion() {
        val deleted = dbHelper.deleteTransaction(transactionId)
        if (deleted) {
            Toast.makeText(requireContext(), "Transacción eliminada", Toast.LENGTH_SHORT).show()
            // Navegar de vuelta al Home
            view?.findNavController()?.navigateUp()
        } else {
            Toast.makeText(requireContext(), "Error al eliminar", Toast.LENGTH_SHORT).show()
        }
    }
    private fun obtenerTasaCambio(monto: Double) {
        val username = sharedPref.getString("usuario", "admin") ?: "admin"
        val monedaBase = sharedPref.getString("${username}_moneda_base", "USD") ?: "USD"
        // Si la moneda base no es USD, usar USD como base para la API (que trabaja con USD)
        val urlBase = if (monedaBase == "USD") "USD" else "USD"
        val urlString = "https://api.exchangerate-api.com/v4/latest/$monedaBase"

        Thread {
            try {
                val url = URL(urlString)
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                connection.connectTimeout = 5000
                connection.readTimeout = 5000

                val responseCode = connection.responseCode
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    val reader = BufferedReader(InputStreamReader(connection.inputStream))
                    val response = StringBuilder()
                    var line: String?
                    while (reader.readLine().also { line = it } != null) {
                        response.append(line)
                    }
                    reader.close()

                    // Procesar respuesta JSON
                    val json = JSONObject(response.toString())
                    val rates = json.getJSONObject("rates")
                    val base = json.getString("base")

                    // Determinar moneda destino (si la base es USD, usar EUR; si no, convertir a USD)
                    val monedaDestino = if (base == "USD") {
                        // Si la moneda base del usuario es EUR, MXN, etc., usar esa
                        if (monedaBase != "USD") monedaBase else "EUR"
                    } else {
                        "USD"
                    }

                    val tasa = rates.optDouble(monedaDestino, 0.0)

                    if (tasa > 0) {
                        val montoConvertido = monto * tasa
                        activity?.runOnUiThread {
                            tvMontoConvertido.text = "${String.format("%.2f", montoConvertido)} $monedaDestino"
                            tvTasa.text = "1 $base = ${String.format("%.4f", tasa)} $monedaDestino"

                            // Frase de poder adquisitivo mejorada
                            val producto = when (monedaDestino) {
                                "EUR" -> "cafés"
                                "COP" -> "arepas"
                                "MXN" -> "tacos"
                                "ARS" -> "empanadas"
                                "USD" -> "almuerzos"
                                else -> "productos"
                            }
                            // Estimación del costo de una unidad del producto en esa moneda
                            val precioUnitario = when (monedaDestino) {
                                "EUR" -> 3.0   // café ~3 EUR
                                "COP" -> 5000.0 // arepa ~5000 COP
                                "MXN" -> 60.0   // taco ~60 MXN
                                "ARS" -> 800.0  // empanada ~800 ARS
                                "USD" -> 10.0   // almuerzo ~10 USD
                                else -> 1.0
                            }
                            val cantidad = (montoConvertido / precioUnitario).toInt()
                            tvFrasePoder.text = if (cantidad > 0) {
                                "En $monedaDestino, equivale a $cantidad $producto."
                            } else {
                                "En $monedaDestino, equivale a menos de 1 $producto."
                            }
                        }
                    } else {
                        activity?.runOnUiThread {
                            tvMontoConvertido.text = "No disponible"
                            tvTasa.text = "No disponible"
                            tvFrasePoder.text = "No se pudo obtener la tasa de cambio para $monedaDestino"
                        }
                    }
                } else {
                    activity?.runOnUiThread {
                        Toast.makeText(requireContext(), "Error al consultar API (código $responseCode)", Toast.LENGTH_SHORT).show()
                        tvMontoConvertido.text = "No disponible"
                        tvTasa.text = "No disponible"
                        tvFrasePoder.text = "Error al obtener tasa de cambio"
                    }
                }
                connection.disconnect()
            } catch (e: Exception) {
                activity?.runOnUiThread {
                    Toast.makeText(requireContext(), "Sin conexión: ${e.message}", Toast.LENGTH_SHORT).show()
                    tvMontoConvertido.text = "No disponible"
                    tvTasa.text = "No disponible"
                    tvFrasePoder.text = "No se pudo establecer conexión con el servidor"
                }
            }
        }.start()
    }
}