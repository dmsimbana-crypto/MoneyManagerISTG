package com.GrupoD.moneymanageristg

data class ExchangeRateResponse(
    val base: String,
    val rates: Map<String, Double>
)