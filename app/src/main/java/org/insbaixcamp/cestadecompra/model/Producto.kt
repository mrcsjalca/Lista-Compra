package org.insbaixcamp.cestadecompra.model

data class Producto (
    val id: String = "",
    val nombre: String  = "",
    val categoriaId: String = "",
    val completado: Boolean = false
)