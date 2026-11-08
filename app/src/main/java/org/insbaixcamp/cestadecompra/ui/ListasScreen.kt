package org.insbaixcamp.cestadecompra.ui

import android.Manifest
import android.content.Context
import android.os.Build
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import org.insbaixcamp.cestadecompra.NotificationHelper
import org.insbaixcamp.cestadecompra.model.Lista
import org.insbaixcamp.cestadecompra.model.Solicitud
import org.insbaixcamp.cestadecompra.model.aUsuario

/**
 * Pantalla principal que muestra las listas de compra del usuario:
 * - Listas creadas por el usuario (propias).
 * - Listas compartidas por otros usuarios.
 * - Avisos y notificaciones en tiempo real con opción directa de Aceptar o Rechazar invitaciones.
 */
@Composable
fun ListasScreen(
    email: String,
    onAbrirLista: (Lista) -> Unit,
    onSalir: () -> Unit
) {
    val context = LocalContext.current
    val db = remember { FirebaseFirestore.getInstance() }
    val usuario = remember { FirebaseAuth.getInstance().currentUser?.aUsuario() }

    var listas by remember { mutableStateOf(listOf<Lista>()) }
    var nuevoNombre by remember { mutableStateOf("") }
    var creandoLista by remember { mutableStateOf(false) }

    val emailLimpio = remember(email) { email.trim().lowercase() }
    val propias = listas.filter { it.esPropia(emailLimpio) }
    val compartidas = listas.filter { !it.esPropia(emailLimpio) }

    val categoriasPorDefecto = listOf(
        "Frutas y verduras", "Lácteos", "Carne y pescado", "Panadería", "Limpieza", "Otros"
    )

    // Estado para abrir el diálogo de compartir
    var listaCompartiendoId by remember { mutableStateOf<String?>(null) }
    val compartiendo = listas.find { it.id == listaCompartiendoId }

    // Estado para abrir la bandeja completa de notificaciones
    var mostrarSolicitudes by remember { mutableStateOf(false) }
    var listaSolicitudes by remember { mutableStateOf(listOf<Solicitud>()) }

    // Solicitar permiso de notificaciones en Android 13+
    val launcherPermisoNotif = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            launcherPermisoNotif.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    // Escuchar las solicitudes/invitaciones pendientes dirigidas a este usuario
    DisposableEffect(emailLimpio) {
        var primeraCarga = true
        val registro = db.collection("solicitudes")
            .whereEqualTo("paraEmail", emailLimpio)
            .addSnapshotListener { snap, error ->
                if (error == null && snap != null) {
                    val solicitudesActuales = snap.documents.mapNotNull { d ->
                        d.toObject(Solicitud::class.java)?.copy(id = d.id)
                    }

                    // Si no es la primera carga y hay invitaciones nuevas, lanzar notificación en la barra de estado
                    if (!primeraCarga) {
                        for (dc in snap.documentChanges) {
                            if (dc.type == com.google.firebase.firestore.DocumentChange.Type.ADDED) {
                                val s = dc.document.toObject(Solicitud::class.java)
                                if (s.tipo == "lista") {
                                    NotificationHelper.lanzarNotificacion(
                                        context = context,
                                        deNombre = s.deNombre.ifBlank { s.deEmail },
                                        listaNombre = s.listaNombre,
                                        notifId = dc.document.id.hashCode()
                                    )
                                }
                            }
                        }
                    }
                    primeraCarga = false
                    listaSolicitudes = solicitudesActuales
                }
            }
        onDispose { registro.remove() }
    }

    // Escuchar las listas donde el usuario es miembro
    DisposableEffect(emailLimpio) {
        val registro = db.collection("listas")
            .whereArrayContains("miembros", emailLimpio)
            .addSnapshotListener { snap, error ->
                if (error != null) {
                    Log.e("Listas", error.message ?: "")
                    return@addSnapshotListener
                }
                listas = snap?.documents?.mapNotNull { d ->
                    d.toObject(Lista::class.java)?.copy(id = d.id)
                } ?: emptyList()
            }
        onDispose { registro.remove() }
    }

    // Diálogo para compartir la lista seleccionada
    if (compartiendo != null) {
        CompartirDialog(
            lista = compartiendo,
            emailUsuario = emailLimpio,
            onCerrar = { listaCompartiendoId = null }
        )
    }

    // Diálogo completo de solicitudes/notificaciones
    if (mostrarSolicitudes) {
        SolicitudesDialog(
            userEmail = emailLimpio,
            onCerrar = { mostrarSolicitudes = false }
        )
    }

    // Diálogo de confirmación para eliminar una lista
    var listaPorEliminar by remember { mutableStateOf<Lista?>(null) }
    val porEliminar = listaPorEliminar
    if (porEliminar != null) {
        AlertDialog(
            onDismissRequest = { listaPorEliminar = null },
            icon = { Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
            title = { Text("Eliminar lista") },
            text = {
                Text(
                    if (porEliminar.esPropia(emailLimpio))
                        "¿Estás seguro de eliminar \"${porEliminar.nombre}\"? Se borrarán todas sus categorías y productos para todos los miembros."
                    else
                        "¿Deseas salir de la lista compartida \"${porEliminar.nombre}\"?"
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    if (porEliminar.esPropia(emailLimpio)) {
                        eliminarLista(db, porEliminar)
                    } else {
                        // Salir de la lista compartida
                        db.collection("listas").document(porEliminar.id)
                            .update("miembros", FieldValue.arrayRemove(emailLimpio))
                    }
                    listaPorEliminar = null
                }) {
                    Text("Eliminar", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { listaPorEliminar = null }) {
                    Text("Cancelar")
                }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        // Cabecera superior con usuario, notificaciones y salir
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)),
            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Avatar circular con la inicial
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(42.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        val inicial = (usuario?.nombre ?: emailLimpio).take(1).uppercase()
                        Text(
                            text = inicial,
                            color = MaterialTheme.colorScheme.onPrimary,
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                }

                Spacer(Modifier.width(12.dp))

                // Nombre y correo del usuario
                Column(Modifier.weight(1f)) {
                    Text(
                        text = usuario?.nombre ?: "Mi Cuenta",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = emailLimpio,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Botón de notificaciones con insignia de cantidad
                BadgedBox(
                    badge = {
                        if (listaSolicitudes.isNotEmpty()) {
                            Badge { Text("${listaSolicitudes.size}") }
                        }
                    }
                ) {
                    IconButton(onClick = { mostrarSolicitudes = true }) {
                        Icon(
                            Icons.Default.Notifications,
                            contentDescription = "Bandeja de notificaciones",
                            tint = if (listaSolicitudes.isNotEmpty()) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Botón para cerrar sesión
                IconButton(onClick = onSalir) {
                    Icon(
                        Icons.AutoMirrored.Filled.ExitToApp,
                        contentDescription = "Cerrar sesión",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }

        // Banners de invitaciones pendientes directamente en pantalla para que el usuario no tenga que buscarlas
        val invitacionesPendientes = listaSolicitudes.filter { it.tipo == "lista" }
        if (invitacionesPendientes.isNotEmpty()) {
            invitacionesPendientes.forEach { inv ->
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp)
                ) {
                    Column(Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.Notifications,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = "¡Invitación a lista de compra!",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }

                        Text(
                            text = "${inv.deNombre.ifBlank { inv.deEmail }} te ha invitado a unirte a \"${inv.listaNombre}\". ¿Quieres unirte?",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(top = 4.dp, bottom = 8.dp)
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            OutlinedButton(
                                onClick = {
                                    // Rechazar invitación
                                    db.collection("solicitudes").document(inv.id).delete()
                                },
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Rechazar")
                            }

                            Spacer(Modifier.width(8.dp))

                            Button(
                                onClick = {
                                    // Aceptar invitación: unirse a la lista
                                    db.collection("listas").document(inv.listaId)
                                        .update("miembros", FieldValue.arrayUnion(emailLimpio))
                                        .addOnSuccessListener {
                                            db.collection("solicitudes").document(inv.id).delete()
                                        }
                                },
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                            ) {
                                Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Aceptar")
                            }
                        }
                    }
                }
            }
        }

        // Crear una nueva lista de compra
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = nuevoNombre,
                onValueChange = { nuevoNombre = it },
                label = { Text("Nueva lista") },
                placeholder = { Text("Ej: Compra semanal") },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.weight(1f)
            )

            Spacer(Modifier.width(8.dp))

            Button(
                onClick = {
                    val nombre = nuevoNombre.trim()
                    if (nombre.isNotEmpty() && !creandoLista) {
                        creandoLista = true
                        db.collection("listas").add(
                            mapOf(
                                "nombre" to nombre,
                                "propietario" to emailLimpio,
                                "propietarioNombre" to (usuario?.nombre ?: emailLimpio.substringBefore("@")),
                                "miembros" to listOf(emailLimpio)
                            )
                        ).addOnSuccessListener { ref ->
                            val batch = db.batch()
                            categoriasPorDefecto.forEach { cat ->
                                val categoriaRef = ref.collection("categorias").document()
                                batch.set(categoriaRef, mapOf("nombre" to cat))
                            }
                            batch.commit().addOnCompleteListener {
                                nuevoNombre = ""
                                creandoLista = false
                            }
                        }.addOnFailureListener {
                            creandoLista = false
                            Log.e("Listas", "Error al crear lista", it)
                        }
                    }
                },
                enabled = !creandoLista && nuevoNombre.isNotBlank(),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.height(56.dp)
            ) {
                if (creandoLista) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(Modifier.width(4.dp))
                    Text("Crear")
                }
            }
        }

        // Listas del usuario (Propias y Compartidas)
        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Sección: Mis listas
            item {
                Text(
                    text = "Mis listas",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(top = 4.dp, bottom = 4.dp)
                )
            }

            if (propias.isEmpty()) {
                item {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                    ) {
                        Text(
                            text = "Aún no tienes listas propias. ¡Crea una arriba!",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }
            } else {
                items(propias, key = { it.id }) { lista ->
                    ListaItem(
                        lista = lista,
                        email = emailLimpio,
                        onAbrirLista = onAbrirLista,
                        onEliminar = { listaPorEliminar = it },
                        onCompartir = { listaCompartiendoId = lista.id }
                    )
                }
            }

            // Sección: Compartidas conmigo
            item {
                Text(
                    text = "Compartidas conmigo",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.padding(top = 16.dp, bottom = 4.dp)
                )
            }

            if (compartidas.isEmpty()) {
                item {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                    ) {
                        Text(
                            text = "No tienes listas compartidas por otros.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }
            } else {
                items(compartidas, key = { it.id }) { lista ->
                    ListaItem(
                        lista = lista,
                        email = emailLimpio,
                        onAbrirLista = onAbrirLista,
                        onEliminar = { listaPorEliminar = it },
                        onCompartir = { listaCompartiendoId = lista.id }
                    )
                }
            }
        }
    }
}

/**
 * Tarjeta individual para mostrar una lista de compras.
 */
@Composable
fun ListaItem(
    lista: Lista,
    email: String,
    onAbrirLista: (Lista) -> Unit,
    onEliminar: (Lista) -> Unit,
    onCompartir: () -> Unit
) {
    val esPropia = lista.esPropia(email)

    Card(
        onClick = { onAbrirLista(lista) },
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = lista.nombre,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )

                Text(
                    text = if (esPropia) "Lista propia" else "De: ${lista.propietarioNombre.ifBlank { lista.propietario }}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(Modifier.height(4.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.People,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = "${lista.miembros.size} miembro(s)",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            // Botón Compartir
            IconButton(onClick = onCompartir) {
                Icon(
                    Icons.Default.Share,
                    contentDescription = "Compartir lista",
                    tint = MaterialTheme.colorScheme.primary
                )
            }

            // Botón Eliminar / Salir
            IconButton(onClick = { onEliminar(lista) }) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Eliminar o salir de la lista",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

/**
 * Elimina una lista y todas sus categorías y productos asociados en Firestore.
 */
fun eliminarLista(db: FirebaseFirestore, lista: Lista) {
    val listRef = db.collection("listas").document(lista.id)
    listRef.collection("categorias").get().addOnSuccessListener { catSnap ->
        val batch = db.batch()
        catSnap.documents.forEach { catDoc ->
            catDoc.reference.collection("productos").get().addOnSuccessListener { prodSnap ->
                prodSnap.documents.forEach { prodDoc -> prodDoc.reference.delete() }
            }
            batch.delete(catDoc.reference)
        }
        batch.delete(listRef)
        batch.commit()
    }.addOnFailureListener {
        listRef.delete()
    }
}
