package org.insbaixcamp.cestadecompra.ui

import android.util.Log
import android.util.Patterns
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.google.firebase.firestore.FirebaseFirestore

@Composable
fun AmigosDialog(
    userEmail: String,
    userNombre: String,
    onCerrar: () -> Unit
) {
    val db = remember { FirebaseFirestore.getInstance() }
    var nuevoEmail by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var exito by remember { mutableStateOf<String?>(null) }

    var misAmigos by remember { mutableStateOf(listOf<String>()) }

    // Escuchar amigos guardados en la colección 'amigos'
    DisposableEffect(userEmail) {
        val registro = db.collection("amigos")
            .whereEqualTo("usuarioEmail", userEmail)
            .addSnapshotListener { snap, err ->
                if (err != null) { Log.e("Amigos", err.message ?: ""); return@addSnapshotListener }
                misAmigos = snap?.documents?.mapNotNull { it.getString("amigoEmail") } ?: emptyList()
            }
        onDispose { registro.remove() }
    }

    fun enviarSolicitudAmistad() {
        val targetEmail = nuevoEmail.trim().lowercase()
        when {
            !Patterns.EMAIL_ADDRESS.matcher(targetEmail).matches() -> {
                error = "Email no válido"
                exito = null
            }
            targetEmail == userEmail -> {
                error = "No puedes agregarte a ti mismo"
                exito = null
            }
            targetEmail in misAmigos -> {
                error = "Ya está en tu lista de amigos"
                exito = null
            }
            else -> {
                error = null
                // Document ID único para evitar duplicados
                val docId = "${userEmail}_$targetEmail"
                val amigoData = mapOf(
                    "usuarioEmail" to userEmail,
                    "amigoEmail" to targetEmail,
                    "fecha" to System.currentTimeMillis()
                )

                // 1. Guardar amigo en colección 'amigos'
                db.collection("amigos").document(docId)
                    .set(amigoData)
                    .addOnSuccessListener {
                        exito = "Amigo añadido: $targetEmail"
                        nuevoEmail = ""

                        // 2. Notificar al destinatario en 'solicitudes'
                        db.collection("solicitudes").add(
                            mapOf(
                                "paraEmail" to targetEmail,
                                "deEmail" to userEmail,
                                "deNombre" to userNombre,
                                "tipo" to "amigo",
                                "fecha" to System.currentTimeMillis()
                            )
                        ).addOnFailureListener { e ->
                            Log.e("Amigos", "Aviso en solicitudes opcional no enviado", e)
                        }
                    }
                    .addOnFailureListener { e ->
                        Log.e("Amigos", "Error al guardar amigo", e)
                        error = "Error al guardar amigo: ${e.localizedMessage ?: "Comprueba la conexión"}"
                    }
            }
        }
    }

    fun eliminarAmigo(amigoEmail: String) {
        val docId = "${userEmail}_$amigoEmail"
        db.collection("amigos").document(docId).delete()
    }

    AlertDialog(
        onDismissRequest = onCerrar,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.PersonAdd,
                    contentDescription = null,
                    modifier = Modifier.padding(end = 8.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Text("Mis Amigos")
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 400.dp)
            ) {
                OutlinedTextField(
                    value = nuevoEmail,
                    onValueChange = { nuevoEmail = it; error = null; exito = null },
                    label = { Text("Añadir amigo por correo") },
                    singleLine = true,
                    isError = error != null,
                    modifier = Modifier.fillMaxWidth()
                )

                error?.let {
                    Text(
                        text = it,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
                exito?.let {
                    Text(
                        text = it,
                        color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }

                Button(
                    onClick = { enviarSolicitudAmistad() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                ) {
                    Icon(Icons.Default.PersonAdd, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Añadir Amigo")
                }

                HorizontalDivider(Modifier.padding(vertical = 8.dp))

                Text(
                    text = "Lista de Amigos (${misAmigos.size}):",
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                if (misAmigos.isEmpty()) {
                    Text(
                        text = "Aún no has añadido amigos",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f, fill = false)
                    ) {
                        items(misAmigos) { emailAmigo ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                                )
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Default.Person,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Text(
                                        text = emailAmigo,
                                        style = MaterialTheme.typography.bodyMedium,
                                        modifier = Modifier
                                            .weight(1f)
                                            .padding(start = 12.dp)
                                    )
                                    IconButton(onClick = { eliminarAmigo(emailAmigo) }) {
                                        Icon(
                                            Icons.Default.Delete,
                                            contentDescription = "Quitar amigo",
                                            tint = MaterialTheme.colorScheme.error
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onCerrar) {
                Text("Cerrar")
            }
        }
    )
}