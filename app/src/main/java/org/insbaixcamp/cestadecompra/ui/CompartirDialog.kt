package org.insbaixcamp.cestadecompra.ui

import android.util.Log
import android.util.Patterns
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import org.insbaixcamp.cestadecompra.model.Lista

/**
 * Diálogo para compartir una lista de compras con otros usuarios mediante su correo electrónico.
 * Al compartir, se genera una invitación en la colección 'solicitudes' para que el destinatario
 * reciba una notificación y decida si aceptar o rechazar unirse a la lista.
 */
@Composable
fun CompartirDialog(
    lista: Lista,
    emailUsuario: String,
    onCerrar: () -> Unit
) {
    val db = remember { FirebaseFirestore.getInstance() }
    var emailInput by remember { mutableStateOf("") }
    var mensajeError by remember { mutableStateOf<String?>(null) }
    var mensajeExito by remember { mutableStateOf<String?>(null) }
    var enviando by remember { mutableStateOf(false) }

    // Colaboradores frecuentes encontrados en otras listas compartidas del usuario
    var colaboradoresFrecuentes by remember(emailUsuario) { mutableStateOf(listOf<String>()) }

    // Cargar colaboradores frecuentes de manera reactiva
    DisposableEffect(emailUsuario) {
        val registro = db.collection("listas")
            .whereArrayContains("miembros", emailUsuario)
            .addSnapshotListener { snap, err ->
                if (err == null && snap != null) {
                    val miembros = snap.documents
                        .flatMap { doc ->
                            (doc.get("miembros") as? List<*>)?.mapNotNull { it as? String } ?: emptyList()
                        }
                        .filter { it.trim().lowercase() != emailUsuario.trim().lowercase() }
                        .distinct()
                    colaboradoresFrecuentes = miembros
                }
            }
        onDispose { registro.remove() }
    }

    // Crea la invitación con ID fijo "listaId_email" (las reglas de Firestore comprueban ese ID)
    fun crearInvitacion(e: String) {
        val usuarioActual = FirebaseAuth.getInstance().currentUser
        val nombreRemitente = usuarioActual?.displayName?.takeIf { it.isNotBlank() }
            ?: emailUsuario.substringBefore("@")

        val nuevaInvitacion = mapOf(
            "paraEmail" to e,
            "deEmail" to emailUsuario.trim().lowercase(),
            "deNombre" to nombreRemitente,
            "tipo" to "lista",
            "listaId" to lista.id,
            "listaNombre" to lista.nombre,
            "fecha" to System.currentTimeMillis()
        )

        db.collection("solicitudes").document("${lista.id}_$e").set(nuevaInvitacion)
            .addOnSuccessListener {
                enviando = false
                mensajeExito = "Invitación enviada a $e"
                mensajeError = null
                emailInput = ""
            }
            .addOnFailureListener { error ->
                enviando = false
                Log.e("Compartir", "Error al enviar invitación", error)
                mensajeError = "Error al enviar la invitación. Revisa tu conexión."
                mensajeExito = null
            }
    }

    // Función para validar y enviar la invitación al correo indicado
    fun enviarInvitacion(correoDestino: String) {
        val e = correoDestino.trim().lowercase()

        when {
            e.isBlank() -> {
                mensajeError = "Introduce un correo electrónico"
                mensajeExito = null
            }
            !Patterns.EMAIL_ADDRESS.matcher(e).matches() -> {
                mensajeError = "El formato de correo no es válido"
                mensajeExito = null
            }
            e == emailUsuario.trim().lowercase() -> {
                mensajeError = "Ya eres el propietario de esta lista"
                mensajeExito = null
            }
            lista.miembros.any { it.trim().lowercase() == e } -> {
                mensajeError = "Este usuario ya forma parte de la lista"
                mensajeExito = null
            }
            else -> {
                mensajeError = null
                enviando = true

                // Comprobar si ya existe una invitación pendiente (incluye deEmail por las reglas)
                db.collection("solicitudes")
                    .whereEqualTo("paraEmail", e)
                    .whereEqualTo("listaId", lista.id)
                    .whereEqualTo("deEmail", emailUsuario.trim().lowercase())
                    .get()
                    .addOnSuccessListener { querySnap ->
                        if (!querySnap.isEmpty) {
                            enviando = false
                            mensajeError = "Ya hay una invitación pendiente para este correo"
                            mensajeExito = null
                        } else {
                            crearInvitacion(e)
                        }
                    }
                    .addOnFailureListener {
                        // Si falla la comprobación, enviamos directamente la invitación
                        crearInvitacion(e)
                    }
            }
        }
    }

    AlertDialog(
        onDismissRequest = onCerrar,
        icon = {
            Icon(
                imageVector = Icons.Default.Share,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
        },
        title = {
            Text(
                text = "Compartir \"${lista.nombre}\"",
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 420.dp)
            ) {
                Text(
                    text = "Invita a otra persona para que pueda ver y editar los productos de esta lista.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                // Mensajes informativos de error o éxito
                if (mensajeError != null) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                    ) {
                        Text(
                            text = mensajeError.orEmpty(),
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(8.dp)
                        )
                    }
                }

                if (mensajeExito != null) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                    ) {
                        Text(
                            text = mensajeExito.orEmpty(),
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(8.dp)
                        )
                    }
                }

                // Campo para introducir el correo del destinatario
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                ) {
                    OutlinedTextField(
                        value = emailInput,
                        onValueChange = {
                            emailInput = it
                            mensajeError = null
                            mensajeExito = null
                        },
                        label = { Text("Correo electrónico") },
                        placeholder = { Text("ejemplo@gmail.com") },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    )

                    Spacer(Modifier.width(8.dp))

                    Button(
                        onClick = { enviarInvitacion(emailInput) },
                        enabled = !enviando && emailInput.isNotBlank(),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp)
                    ) {
                        if (enviando) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        } else {
                            Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Enviar")
                        }
                    }
                }

                // Sugerencias de colaboradores habituales
                if (colaboradoresFrecuentes.isNotEmpty()) {
                    Text(
                        text = "Colaboradores frecuentes:",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(top = 4.dp, bottom = 4.dp)
                    )

                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f, fill = false)
                    ) {
                        items(colaboradoresFrecuentes) { colabEmail ->
                            val yaEsta = lista.miembros.any { it.trim().lowercase() == colabEmail.trim().lowercase() }
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 2.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Default.Person,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp),
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                    Text(
                                        text = colabEmail,
                                        style = MaterialTheme.typography.bodySmall,
                                        modifier = Modifier
                                            .weight(1f)
                                            .padding(horizontal = 8.dp)
                                    )
                                    if (yaEsta) {
                                        Icon(
                                            Icons.Default.Check,
                                            contentDescription = "Ya está en la lista",
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    } else {
                                        IconButton(
                                            onClick = { enviarInvitacion(colabEmail) },
                                            enabled = !enviando,
                                            modifier = Modifier.size(28.dp)
                                        ) {
                                            Icon(
                                                Icons.AutoMirrored.Filled.Send,
                                                contentDescription = "Invitar",
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                HorizontalDivider(Modifier.padding(vertical = 8.dp))

                // Lista de miembros actuales de esta lista
                Text(
                    text = "Miembros en esta lista (${lista.miembros.size}):",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 4.dp)
                )

                lista.miembros.forEach { miembro ->
                    val esCreador = miembro.trim().lowercase() == lista.propietario.trim().lowercase()
                    Text(
                        text = "• $miembro" + if (esCreador) " (Creador)" else "",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 1.dp, horizontal = 4.dp)
                    )
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