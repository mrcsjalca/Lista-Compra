package org.insbaixcamp.cestadecompra

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import org.insbaixcamp.cestadecompra.ui.LoginScreen
import org.insbaixcamp.cestadecompra.ui.theme.CestadecompraTheme
import com.google.firebase.firestore.FirebaseFirestore

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

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

    // Escucha los cambios de sesión (login, logout, token revocado...)
    DisposableEffect(Unit) {
        val listener = FirebaseAuth.AuthStateListener { auth ->
            usuario = auth.currentUser
        }
        FirebaseAuth.getInstance().addAuthStateListener(listener)
        onDispose { FirebaseAuth.getInstance().removeAuthStateListener(listener) }
    }

    val usuarioActual = usuario
    if (usuarioActual != null) {
        PantallaPrincipal(
            usuario = usuarioActual,
            onLogout = { FirebaseAuth.getInstance().signOut() }
        )
    } else {
        // El listener actualiza el estado, no hace falta hacer nada aquí
        LoginScreen(onLogged = { })
    }
}

@Composable
fun PantallaPrincipal(
    usuario: FirebaseUser,
    onLogout: () -> Unit
) {
    Scaffold(
        modifier = Modifier.fillMaxSize()
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .padding(16.dp)
        ) {
            Text(stringResource(R.string.sesion_iniciada_como, usuario.email.orEmpty()))
            Text(stringResource(R.string.uid_usuario, usuario.uid))

            Button(onClick = onLogout) {
                Text(stringResource(R.string.cerrar_sesion))
            }
        }
    }
}