package com.GrupoD.moneymanageristg

data class Transaction(
    val id: Long = 0,
    val concepto: String,
    val categoria: String,
    val subcategoria: String? = null,
    val monto: Double,
    val fecha: String,
    val tipo: String = "Egreso", // "Ingreso", "Egreso", "Traspaso"
    val medioPago: String? = null,
    val cuenta: String? = null
)