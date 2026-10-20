package org.insbaixcamp.cestadecompra.ui

import org.insbaixcamp.cestadecompra.loginConGoogle

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

@Composable
fun LoginScreen(onLogged: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var error by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Button(onClick = {
            scope.launch {
                val ok = loginConGoogle(context)
                if (ok) onLogged() else error = true
            }
        }) {
            Text("Iniciar sesión con Google")
        }
        if (error) {
            Text("No se pudo iniciar sesión", modifier = Modifier.padding(top = 12.dp))
        }
    }
}