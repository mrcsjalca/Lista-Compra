package org.insbaixcamp.cestadecompra.model

data class Producto (
    val id: String = "",
    val nombre: String  = "",
    val completado: Boolean = false,
    val etiqueta: String = ""
)