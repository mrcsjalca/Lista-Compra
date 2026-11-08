package org.insbaixcamp.cestadecompra

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import org.insbaixcamp.cestadecompra.model.Lista
import org.insbaixcamp.cestadecompra.ui.ListaCompraScreen
import org.insbaixcamp.cestadecompra.ui.ListasScreen
import org.insbaixcamp.cestadecompra.ui.LoginScreen
import org.insbaixcamp.cestadecompra.ui.theme.CestadecompraTheme

/**
 * Actividad principal de la aplicación.
 * Gestiona el flujo entre la pantalla de Login, la lista general de compras
 * y la pantalla de detalle de una lista abierta.
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Inicializar canal de notificaciones para las invitaciones
        NotificationHelper.crearCanal(this)

        setContent {
            CestadecompraTheme {
                CestadecompraApp()
            }
        }
    }
}

@Composable
fun CestadecompraApp() {
    var usuario by remember { mutableStateOf(FirebaseAuth.getInstance().currentUser) }
    var listaAbierta by remember { mutableStateOf<Lista?>(null) }

    // Escuchar el estado de autenticación (inicio o cierre de sesión)
    DisposableEffect(Unit) {
        val listener = FirebaseAuth.AuthStateListener { auth ->
            usuario = auth.currentUser
        }
        FirebaseAuth.getInstance().addAuthStateListener(listener)
        onDispose { FirebaseAuth.getInstance().removeAuthStateListener(listener) }
    }

    // Registrar o actualizar datos básicos del usuario en Firestore
    val usuarioActual = usuario
    LaunchedEffect(usuarioActual?.uid) {
        if (usuarioActual != null) {
            val emailLimpio = usuarioActual.email.orEmpty().trim().lowercase()
            if (emailLimpio.isNotEmpty()) {
                FirebaseFirestore.getInstance()
                    .collection("usuarios")
                    .document(emailLimpio)
                    .set(
                        mapOf(
                            "email" to emailLimpio,
                            "nombre" to (usuarioActual.displayName ?: emailLimpio.substringBefore("@")),
                            "uid" to usuarioActual.uid
                        )
                    )
                    .addOnFailureListener { e ->
                        Log.e("Usuarios", "Error registrando usuario en Firestore", e)
                    }
            }
        }
    }

    val lista = listaAbierta
    if (usuarioActual == null) {
        // Si no ha iniciado sesión, mostrar pantalla de bienvenida y login
        LoginScreen(onLogged = { })
    } else if (lista == null) {
        // Pantalla con todas las listas (propias y compartidas)
        ListasScreen(
            email = usuarioActual.email.orEmpty().lowercase(),
            onAbrirLista = { listaAbierta = it },
            onSalir = {
                listaAbierta = null
                FirebaseAuth.getInstance().signOut()
            }
        )
    } else {
        // Pantalla de detalle de la lista seleccionada
        ListaCompraScreen(
            lista = lista,
            onVolver = { listaAbierta = null }
        )
    }
}
