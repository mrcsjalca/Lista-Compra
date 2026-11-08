package org.insbaixcamp.cestadecompra.model

import com.google.firebase.auth.FirebaseUser

/**
 * Modelo de datos para representar al usuario en la aplicación.
 */
data class Usuario(
    val uid: String = "",
    val email: String = "",
    val nombre: String = "",
    val fotoUrl: String = ""
)

/**
 * Conversor de conveniencia de un FirebaseUser a nuestro modelo Usuario.
 */
fun FirebaseUser.aUsuario() = Usuario(
    uid = uid,
    email = email.orEmpty().lowercase(),
    nombre = displayName ?: email.orEmpty().substringBefore("@"),
    fotoUrl = photoUrl?.toString().orEmpty()
)
