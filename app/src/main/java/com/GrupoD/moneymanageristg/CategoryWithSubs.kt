package com.GrupoD.moneymanageristg

data class CategoryWithSubs(
    val id: Long,
    val nombre: String,
    val subcategorias: List<Subcategory>
)