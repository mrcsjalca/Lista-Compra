package org.insbaixcamp.cestadecompra.ui

import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
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
import org.insbaixcamp.cestadecompra.model.Solicitud

/**
 * Diálogo de notificaciones e invitaciones pendientes del usuario.
 * Permite aceptar o rechazar solicitudes para unirse a listas compartidas.
 */
@Composable
fun SolicitudesDialog(
    userEmail: String,
    onCerrar: () -> Unit
) {
    val db = remember { FirebaseFirestore.getInstance() }
    var solicitudes by remember { mutableStateOf(listOf<Solicitud>()) }

    // Escuchar las invitaciones dirigidas al usuario actual
    DisposableEffect(userEmail) {
        val registro = db.collection("solicitudes")
            .whereEqualTo("paraEmail", userEmail.trim().lowercase())
            .addSnapshotListener { snap, err ->
                if (err != null) {
                    Log.e("Solicitudes", "Error al leer solicitudes", err)
                    return@addSnapshotListener
                }
                solicitudes = snap?.documents?.mapNotNull { d ->
                    d.toObject(Solicitud::class.java)?.copy(id = d.id)
                } ?: emptyList()
            }
        onDispose { registro.remove() }
    }

    // Aceptar la invitación: añade al usuario a los miembros de la lista
    fun aceptarInvitacion(s: Solicitud) {
        val miNombre = FirebaseAuth.getInstance().currentUser?.displayName ?: userEmail.substringBefore("@")

        if (s.listaId.isNotBlank()) {
            db.collection("listas").document(s.listaId)
                .update("miembros", FieldValue.arrayUnion(userEmail.trim().lowercase()))
                .addOnSuccessListener {
                    // Notificar al creador que la invitación fue aceptada
                    db.collection("solicitudes").add(
                        mapOf(
                            "paraEmail" to s.deEmail,
                            "deEmail" to userEmail,
                            "tipo" to "notificacion",
                            "mensaje" to "$miNombre ha aceptado unirse a tu lista \"${s.listaNombre}\".",
                            "fecha" to System.currentTimeMillis()
                        )
                    )
                }
        }
        // Eliminar la invitación procesada
        db.collection("solicitudes").document(s.id).delete()
    }

    // Rechazar la invitación: no añade al usuario y borra la solicitud
    fun rechazarInvitacion(s: Solicitud) {
        val miNombre = FirebaseAuth.getInstance().currentUser?.displayName ?: userEmail.substringBefore("@")

        // Enviar aviso opcional de rechazo
        db.collection("solicitudes").add(
            mapOf(
                "paraEmail" to s.deEmail,
                "deEmail" to userEmail,
                "tipo" to "notificacion",
                "mensaje" to "$miNombre ha declinado la invitación a la lista \"${s.listaNombre}\".",
                "fecha" to System.currentTimeMillis()
            )
        )
        // Eliminar la invitación
        db.collection("solicitudes").document(s.id).delete()
    }

    // Descartar avisos o notificaciones simples
    fun descartarNotificacion(s: Solicitud) {
        db.collection("solicitudes").document(s.id).delete()
    }

    AlertDialog(
        onDismissRequest = onCerrar,
        icon = {
            Icon(
                Icons.Default.Notifications,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
        },
        title = {
            Text(
                "Bandeja de notificaciones",
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 420.dp)
            ) {
                if (solicitudes.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No tienes invitaciones pendientes.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f, fill = false)
                    ) {
                        items(solicitudes, key = { it.id }) { s ->
                            Card(
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp)
                                ) {
                                    if (s.tipo == "notificacion") {
                                        // Notificación informativa
                                        Text(
                                            text = "Aviso",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MaterialTheme.colorScheme.secondary
                                        )
                                        Text(
                                            text = s.mensaje,
                                            style = MaterialTheme.typography.bodyMedium,
                                            modifier = Modifier.padding(vertical = 4.dp)
                                        )
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.End
                                        ) {
                                            TextButton(onClick = { descartarNotificacion(s) }) {
                                                Text("Entendido")
                                            }
                                        }
                                    } else {
                                        // Invitación para unirse a una lista
                                        Text(
                                            text = "Invitación a lista",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        Text(
                                            text = "${s.deNombre.ifBlank { s.deEmail }} te ha invitado a unirte a \"${s.listaNombre}\".",
                                            style = MaterialTheme.typography.bodyMedium,
                                            modifier = Modifier.padding(vertical = 4.dp)
                                        )
                                        Text(
                                            text = "¿Deseas unirte a esta lista para colaborar?",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )

                                        Spacer(Modifier.height(8.dp))

                                        // Botones de acción: Aceptar / Rechazar
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.End,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            OutlinedButton(
                                                onClick = { rechazarInvitacion(s) },
                                                shape = RoundedCornerShape(8.dp),
                                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                                            ) {
                                                Icon(
                                                    Icons.Default.Close,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(16.dp),
                                                    tint = MaterialTheme.colorScheme.error
                                                )
                                                Spacer(Modifier.width(4.dp))
                                                Text("Rechazar", color = MaterialTheme.colorScheme.error)
                                            }

                                            Spacer(Modifier.width(8.dp))

                                            Button(
                                                onClick = { aceptarInvitacion(s) },
                                                shape = RoundedCornerShape(8.dp),
                                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                                            ) {
                                                Icon(
                                                    Icons.Default.Check,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                                Spacer(Modifier.width(4.dp))
                                                Text("Aceptar")
                                            }
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
