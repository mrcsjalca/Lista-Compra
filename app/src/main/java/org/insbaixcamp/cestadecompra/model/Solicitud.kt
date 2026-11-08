package org.insbaixcamp.cestadecompra.model

/**
 * Modelo de datos para las invitaciones y notificaciones entre usuarios.
 * Permite enviar una invitación a otra persona para unirse a una lista de la compra.
 */
data class Solicitud(
    val id: String = "",
    val deEmail: String = "",
    val deNombre: String = "",
    val paraEmail: String = "",
    val tipo: String = "lista", // "lista" para invitaciones, "notificacion" para respuestas
    val listaId: String = "",
    val listaNombre: String = "",
    val mensaje: String = "",
    val fecha: Long = 0L
)
