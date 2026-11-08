package org.insbaixcamp.cestadecompra.model

/**
 * Modelo de datos para un producto dentro de una categoría.
 */
data class Producto(
    val id: String = "",
    val nombre: String = "",
    val completado: Boolean = false,
    val etiqueta: String = ""
)
