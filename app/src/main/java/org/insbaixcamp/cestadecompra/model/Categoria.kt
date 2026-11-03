package org.insbaixcamp.cestadecompra.model

data class Categoria (
    val id: String = "",
    val nombre: String = ""
)
// lo hacemos de esta forma y no con una array para no sobreescribir una array entero
val CATEGORIAS_POR_DEFECTO = listOf(
    "Frutas y verduras", "Lácteos", "Carne y pescado", "Panadería", "Limpieza", "Otros"
)