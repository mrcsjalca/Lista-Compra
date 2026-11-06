package org.insbaixcamp.cestadecompra.ui

import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore

data class SolicitudItem(
    val id: String = "",
    val deEmail: String = "",
    val deNombre: String = "",
    val paraEmail: String = "",
    val tipo: String = "amigo", // "amigo", "lista", o "notificacion"
    val listaId: String = "",
    val listaNombre: String = "",
    val mensaje: String = ""
)

@Composable
fun SolicitudesDialog(
    userEmail: String,
    onCerrar: () -> Unit
) {
    val db = remember { FirebaseFirestore.getInstance() }
    var solicitudes by remember { mutableStateOf(listOf<SolicitudItem>()) }

    DisposableEffect(userEmail) {
        val registro = db.collection("solicitudes")
            .whereEqualTo("paraEmail", userEmail)
            .addSnapshotListener { snap, err ->
                if (err != null) { Log.e("Solicitudes", err.message ?: ""); return@addSnapshotListener }
                solicitudes = snap?.documents?.mapNotNull { d ->
                    d.toObject(SolicitudItem::class.java)?.copy(id = d.id)
                } ?: emptyList()
            }
        onDispose { registro.remove() }
    }

    fun aceptarSolicitud(s: SolicitudItem) {
        val miNombre = FirebaseAuth.getInstance().currentUser?.displayName ?: userEmail

        if (s.tipo == "amigo") {
            // Añadir como amigos mutuamente en la colección 'amigos'
            val docId1 = "${userEmail}_${s.deEmail}"
            val docId2 = "${s.deEmail}_${userEmail}"

            db.collection("amigos").document(docId1)
                .set(mapOf("usuarioEmail" to userEmail, "amigoEmail" to s.deEmail, "fecha" to System.currentTimeMillis()))
            
            db.collection("amigos").document(docId2)
                .set(mapOf("usuarioEmail" to s.deEmail, "amigoEmail" to userEmail, "fecha" to System.currentTimeMillis()))

            // Enviar respuesta de aviso al remitente
            db.collection("solicitudes").add(
                mapOf(
                    "paraEmail" to s.deEmail,
                    "deEmail" to userEmail,
                    "tipo" to "notificacion",
                    "mensaje" to "$miNombre ha aceptado tu solicitud de amistad."
                )
            )
        } else if (s.tipo == "lista" && s.listaId.isNotBlank()) {
            // Añadir usuario a la lista compartida
            db.collection("listas").document(s.listaId)
                .update("miembros", FieldValue.arrayUnion(userEmail))

            // También añadirlos como amigos automáticamente al aceptar compartir lista
            val docId1 = "${userEmail}_${s.deEmail}"
            val docId2 = "${s.deEmail}_${userEmail}"

            db.collection("amigos").document(docId1)
                .set(mapOf("usuarioEmail" to userEmail, "amigoEmail" to s.deEmail, "fecha" to System.currentTimeMillis()))
            
            db.collection("amigos").document(docId2)
                .set(mapOf("usuarioEmail" to s.deEmail, "amigoEmail" to userEmail, "fecha" to System.currentTimeMillis()))

            // Enviar respuesta de aviso al remitente
            db.collection("solicitudes").add(
                mapOf(
                    "paraEmail" to s.deEmail,
                    "deEmail" to userEmail,
                    "tipo" to "notificacion",
                    "mensaje" to "$miNombre ha aceptado la invitación a la lista \"${s.listaNombre}\"."
                )
            )
        }
        // Borrar la solicitud procesada
        db.collection("solicitudes").document(s.id).delete()
    }

    fun rechazarSolicitud(s: SolicitudItem) {
        val miNombre = FirebaseAuth.getInstance().currentUser?.displayName ?: userEmail

        val msg = if (s.tipo == "lista") {
            "$miNombre ha rechazado la invitación a la lista \"${s.listaNombre}\"."
        } else {
            "$miNombre ha rechazado tu solicitud de amistad."
        }

        db.collection("solicitudes").add(
            mapOf(
                "paraEmail" to s.deEmail,
                "deEmail" to userEmail,
                "tipo" to "notificacion",
                "mensaje" to msg
            )
        )

        db.collection("solicitudes").document(s.id).delete()
    }

    fun descartarNotificacion(s: SolicitudItem) {
        db.collection("solicitudes").document(s.id).delete()
    }

    AlertDialog(
        onDismissRequest = onCerrar,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.Notifications,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(end = 8.dp)
                )
                Text("Bandeja de Entrada")
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 400.dp)
            ) {
                if (solicitudes.isEmpty()) {
                    Text(
                        text = "No tienes notificaciones ni solicitudes pendientes.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 16.dp)
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f, fill = false)
                    ) {
                        items(solicitudes, key = { it.id }) { s ->
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
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        when (s.tipo) {
                                            "notificacion" -> {
                                                Text(
                                                    text = "Aviso de respuesta",
                                                    style = MaterialTheme.typography.titleSmall,
                                                    color = MaterialTheme.colorScheme.secondary
                                                )
                                                Text(
                                                    text = s.mensaje,
                                                    style = MaterialTheme.typography.bodySmall
                                                )
                                            }
                                            "amigo" -> {
                                                Text(
                                                    text = "Solicitud de amistad",
                                                    style = MaterialTheme.typography.titleSmall,
                                                    color = MaterialTheme.colorScheme.primary
                                                )
                                                Text(
                                                    text = "${s.deNombre.ifBlank { s.deEmail }} quiere agregarte como amigo.",
                                                    style = MaterialTheme.typography.bodySmall
                                                )
                                            }
                                            else -> {
                                                Text(
                                                    text = "Invitación a lista",
                                                    style = MaterialTheme.typography.titleSmall,
                                                    color = MaterialTheme.colorScheme.primary
                                                )
                                                Text(
                                                    text = "${s.deNombre.ifBlank { s.deEmail }} te ha invitado a la lista \"${s.listaNombre}\".",
                                                    style = MaterialTheme.typography.bodySmall
                                                )
                                            }
                                        }
                                    }

                                    if (s.tipo == "notificacion") {
                                        TextButton(onClick = { descartarNotificacion(s) }) {
                                            Text("OK")
                                        }
                                    } else {
                                        IconButton(onClick = { aceptarSolicitud(s) }) {
                                            Icon(
                                                Icons.Default.Check,
                                                contentDescription = "Aceptar",
                                                tint = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                        IconButton(onClick = { rechazarSolicitud(s) }) {
                                            Icon(
                                                Icons.Default.Close,
                                                contentDescription = "Rechazar",
                                                tint = MaterialTheme.colorScheme.error
                                            )
                                        }
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