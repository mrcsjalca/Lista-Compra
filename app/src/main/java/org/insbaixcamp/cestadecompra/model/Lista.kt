package org.insbaixcamp.cestadecompra.model

/**
 * Modelo de datos para las listas de compra.
 * Incluye el propietario y la lista de correos de los miembros con acceso.
 */
data class Lista(
    val id: String = "",
    val nombre: String = "",
    val propietario: String = "",
    val propietarioNombre: String = "",
    val miembros: List<String> = emptyList()
) {
    /**
     * Comprueba si la lista pertenece al correo indicado.
     */
    fun esPropia(email: String): Boolean {
        return propietario.trim().lowercase() == email.trim().lowercase()
    }
}
