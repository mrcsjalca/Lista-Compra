package org.insbaixcamp.cestadecompra.model

/**
 * Modelo de datos para las categorías de una lista (ej: Frutas, Lácteos, etc.).
 */
data class Categoria(
    val id: String = "",
    val nombre: String = ""
)

/**
 * Lista de categorías precargadas por defecto al crear una nueva lista de compra.
 */
val CATEGORIAS_POR_DEFECTO = listOf(
    "Frutas y verduras", "Lácteos", "Carne y pescado", "Panadería", "Limpieza", "Otros"
)
