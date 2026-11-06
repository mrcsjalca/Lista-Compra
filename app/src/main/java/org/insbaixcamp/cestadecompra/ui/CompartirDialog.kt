package org.insbaixcamp.cestadecompra.ui

import android.util.Log
import android.util.Patterns
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import org.insbaixcamp.cestadecompra.model.Lista

@Composable
fun CompartirDialog(
    lista: Lista,
    emailUsuario: String,
    onCerrar: () -> Unit
) {
    val db = remember { FirebaseFirestore.getInstance() }
    var emailInput by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var exito by remember { mutableStateOf<String?>(null) }
    var buscando by remember { mutableStateOf(false) }

    // Obtener colaboradores frecuentes a partir de las listas que comparte el usuario
    var colaboradoresFrecuentes by remember(emailUsuario) { mutableStateOf(listOf<String>()) }

    DisposableEffect(emailUsuario) {
        val registro = db.collection("listas")
            .whereArrayContains("miembros", emailUsuario)
            .addSnapshotListener { snap, err ->
                if (err == null && snap != null) {
                    val todosMiembros = snap.documents
                        .flatMap { doc ->
                            (doc.get("miembros") as? List<*>)?.mapNotNull { it as? String } ?: emptyList()
                        }
                        .filter { it.lowercase() != emailUsuario.lowercase() }
                        .distinct()
                    colaboradoresFrecuentes = todosMiembros
                }
            }
        onDispose { registro.remove() }
    }

    fun compartirLista(targetEmail: String) {
        val e = targetEmail.trim().lowercase()
        when {
            !Patterns.EMAIL_ADDRESS.matcher(e).matches() -> {
                error = "Email no válido"
                exito = null
            }
            e == emailUsuario -> {
                error = "Ya eres el propietario de esta lista"
                exito = null
            }
            e in lista.miembros -> {
                error = "Ya es miembro de esta lista"
                exito = null
            }
            else -> {
                error = null
                buscando = true

                // Comprobar primeramente si la persona está registrada en la app
                db.collection("usuarios").document(e).get()
                    .addOnSuccessListener { doc ->
                        buscando = false
                        if (doc.exists()) {
                            // Usuario registrado -> compartir lista
                            db.collection("listas").document(lista.id)
                                .update("miembros", FieldValue.arrayUnion(e))
                                .addOnSuccessListener {
                                    exito = "Lista compartida con $e"
                                    emailInput = ""
                                }
                                .addOnFailureListener { err ->
                                    Log.e("Compartir", "Error al añadir miembro", err)
                                    error = "No se pudo compartir la lista"
                                }
                        } else {
                            // Usuario no registrado en la app
                            error = "El usuario '$e' no está registrado en la app. Debe iniciar sesión en la app primero."
                            exito = null
                        }
                    }
                    .addOnFailureListener { err ->
                        buscando = false
                        Log.e("Compartir", "Error verificando usuario, añadiendo directamente", err)
                        db.collection("listas").document(lista.id)
                            .update("miembros", FieldValue.arrayUnion(e))
                            .addOnSuccessListener {
                                exito = "Lista compartida con $e"
                                emailInput = ""
                            }
                    }
            }
        }
    }

    AlertDialog(
        onDismissRequest = onCerrar,
        title = { Text("Compartir \"${lista.nombre}\"") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 420.dp)
            ) {
                // Mensajes de error, éxito o búsqueda
                if (buscando) {
                    Text(
                        text = "Verificando si el usuario está registrado...",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }
                error?.let {
                    Text(
                        text = it,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }
                exito?.let {
                    Text(
                        text = it,
                        color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }

                // Campo para ingresar un correo electrónico
                Text(
                    text = "Añadir persona por correo electrónico:",
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                ) {
                    OutlinedTextField(
                        value = emailInput,
                        onValueChange = { emailInput = it; error = null; exito = null },
                        label = { Text("Email de la persona") },
                        singleLine = true,
                        isError = error != null,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(
                        onClick = { compartirLista(emailInput) },
                        enabled = !buscando,
                        modifier = Modifier.padding(start = 4.dp)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Compartir lista")
                    }
                }

                HorizontalDivider(Modifier.padding(vertical = 4.dp))

                // Colaboradores Frecuentes
                Text(
                    text = "Mis colaboradores frecuentes:",
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.padding(vertical = 4.dp)
                )

                if (colaboradoresFrecuentes.isEmpty()) {
                    Text(
                        text = "Aún no has compartido listas con otros usuarios",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f, fill = false)
                    ) {
                        items(colaboradoresFrecuentes) { emailColaborador ->
                            val yaEsMiembro = emailColaborador in lista.miembros
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.Person,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = emailColaborador,
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier
                                        .weight(1f)
                                        .padding(start = 8.dp)
                                )
                                if (yaEsMiembro) {
                                    Icon(
                                        Icons.Default.Check,
                                        contentDescription = "Ya es miembro",
                                        tint = MaterialTheme.colorScheme.secondary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                } else {
                                    IconButton(
                                        onClick = { compartirLista(emailColaborador) },
                                        enabled = !buscando
                                    ) {
                                        Icon(
                                            Icons.AutoMirrored.Filled.Send,
                                            contentDescription = "Añadir a la lista",
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                HorizontalDivider(Modifier.padding(vertical = 8.dp))

                // Miembros Actuales
                Text(
                    text = "Miembros actuales (${lista.miembros.size}):",
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
                lista.miembros.forEach { m ->
                    Text(
                        text = "• $m",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = 4.dp)
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