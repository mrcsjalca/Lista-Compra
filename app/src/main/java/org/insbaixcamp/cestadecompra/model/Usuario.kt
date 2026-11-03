package org.insbaixcamp.cestadecompra.model

import com.google.firebase.auth.FirebaseUser

data class Usuario(
    val uid: String = "",
    val email: String = "",
    val nombre: String = "",
    val fotoUrl: String = ""
)

fun FirebaseUser.aUsuario() = Usuario(
    uid = uid,
    email = email.orEmpty().lowercase(),
    nombre = displayName ?: email.orEmpty().substringBefore("@"),
    fotoUrl = photoUrl?.toString().orEmpty()
)
