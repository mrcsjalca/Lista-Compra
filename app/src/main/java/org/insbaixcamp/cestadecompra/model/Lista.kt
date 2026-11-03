package org.insbaixcamp.cestadecompra.model

data class Lista(
    val id: String = "",
    val nombre: String = "",
    val propietario: String = "",
    val propietarioNombre: String = "",
    val miembros: List<String> = emptyList()
) {
    fun esPropia(email: String): Boolean {
        // devuelve un boolean si es el correo del usuario o no
        return propietario == email
    }
}




