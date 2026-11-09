package com.capitansolnan.llistadelacomprapi

data class Producto(
    val id: String = "",
    val nombre: String = "",
    val cantidad: Int = 1,
    val comprado: Boolean = false
)