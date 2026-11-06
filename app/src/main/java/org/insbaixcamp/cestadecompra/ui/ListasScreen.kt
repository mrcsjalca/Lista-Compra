package org.insbaixcamp.cestadecompra.ui

import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import org.insbaixcamp.cestadecompra.model.Lista
import org.insbaixcamp.cestadecompra.model.aUsuario

/**
 * Codigo para poder cargar la vista de las listas
 * contiene un boton para cerrar sesion
 * un apartado para crear listas
 * las diferentes categorias
 * cargamos las listas con un foreach
 * y vemos las listas que tenemos compartidas
 */
@Composable
fun ListasScreen(
    email: String,
    onAbrirLista: (Lista) -> Unit,
    onSalir: () -> Unit
) {
    val db = remember { FirebaseFirestore.getInstance() }
    val usuario = remember { FirebaseAuth.getInstance().currentUser?.aUsuario() }
    var listas by remember { mutableStateOf(listOf<Lista>()) }
    var nuevoNombre by remember { mutableStateOf("") }

    val propias = listas.filter { it.esPropia(email) }
    val compartidas = listas.filter { !it.esPropia(email) }
    val CATEGORIAS_POR_DEFECTO = listOf(
        "Frutas y verduras", "Lácteos", "Carne y pescado", "Panadería", "Limpieza", "Otros"
    )
    var listaCompartiendoId by remember { mutableStateOf<String?>(null) }
    val compartiendo = listas.find { it.id == listaCompartiendoId }

    if (compartiendo != null) {
        CompartirDialog(
            lista = compartiendo,
            emailUsuario = email,
            onCerrar = { listaCompartiendoId = null }
        )
    }

    // Lógica para borrar la lista de Firestore
    var listaPorEliminar by remember { mutableStateOf<Lista?>(null) }
    val porEliminar = listaPorEliminar
    if (porEliminar != null) {
        AlertDialog(
            onDismissRequest = { listaPorEliminar = null },
            title = { Text("Eliminar lista") },
            text = { Text("Se borrará \"${porEliminar.nombre}\" con sus categorías y productos para todos los miembros.") },
            confirmButton = {
                TextButton(onClick = {
                    eliminarLista(db, porEliminar)
                    listaPorEliminar = null
                }) { Text("Eliminar", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { listaPorEliminar = null }) { Text("Cancelar") } }
        )
    }

    DisposableEffect(email) {
        val registro = db.collection("listas")
            .whereArrayContains("miembros", email)
            .addSnapshotListener { snap, error ->
                if (error != null) { Log.e("Listas", error.message ?: ""); return@addSnapshotListener }
                listas = snap?.documents?.mapNotNull { d ->
                    d.toObject(Lista::class.java)?.copy(id = d.id)
                } ?: emptyList()
            }
        onDispose { registro.remove() }
    }

    Column(Modifier.statusBarsPadding().padding(16.dp)) {
        // Cabecera principal con información del usuario y botón cerrar sesión
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(usuario?.nombre ?: "Usuario", style = MaterialTheme.typography.titleLarge)
                Text(usuario?.email ?: email, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
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

        // Apartado para crear listas
        Row(Modifier.padding(vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = nuevoNombre,
                onValueChange = { nuevoNombre = it },
                label = { Text("Nombre de lista") },
                singleLine = true,
                modifier = Modifier.weight(1f)
            )
            Button(
                onClick = {
                    val nombre = nuevoNombre.trim()
                    if (nombre.isNotEmpty()) {
                        db.collection("listas").add(
                            mapOf(
                                "nombre" to nombre,
                                "propietario" to email,
                                "propietarioNombre" to (usuario?.nombre ?: email),
                                "miembros" to listOf(email)
                            )
                        ).addOnSuccessListener { ref ->
                            val batch = db.batch()
                            CATEGORIAS_POR_DEFECTO.forEach { cat ->
                                val categoriaRef = ref.collection("categorias").document()
                                batch.set(
                                    categoriaRef,
                                    mapOf("nombre" to cat)
                                )
                            }
                            batch.commit()
                                .addOnSuccessListener {
                                    nuevoNombre = ""
                                    Log.d("Listas", "Se han creado las 6 categorías")
                                }
                                .addOnFailureListener {
                                    Log.e("Listas", "Error creando categorías", it)
                                }
                        }
                            .addOnFailureListener { Log.e("Listas", "Error al crear", it) }
                    }
                },
                modifier = Modifier.padding(start = 8.dp)
            ) { Text("Crear") }
        }

        // Cargamos las listas con un LazyColumn (que sustituye el foreach para listas reactivas)
        LazyColumn(Modifier.weight(1f)) {
            item {
                Text(
                    "Mis listas",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }
            if (propias.isEmpty()) {
                item {
                    Text(
                        "Aún no tienes listas",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }
            }
            items(propias, key = { it.id }) { lista ->
                ListaItem(
                    lista = lista,
                    email = email,
                    onAbrirLista = onAbrirLista,
                    onEliminar = { listaPorEliminar = it },
                    onCompartir = { listaCompartiendoId = lista.id }
                )
            }

            item {
                Text(
                    "Compartidas conmigo",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
                )
            }
            if (compartidas.isEmpty()) {
                item {
                    Text(
                        "Nadie ha compartido listas contigo",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }
            }

            items(compartidas, key = { it.id }) { lista ->
                ListaItem(
                    lista = lista,
                    email = email,
                    onAbrirLista = onAbrirLista,
                    onEliminar = { listaPorEliminar = it },
                    onCompartir = { listaCompartiendoId = lista.id }
                )
            }
        }
    }
}

/**
 * Función auxiliar para eliminar una lista y sus subcolecciones de Firestore
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

@Composable
fun ListaItem(
    lista: Lista,
    email: String,
    onAbrirLista: (Lista) -> Unit,
    onEliminar: (Lista) -> Unit,
    onCompartir: () -> Unit
) {
    Card(
        onClick = { onAbrirLista(lista) },
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    lista.nombre,
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    if (lista.esPropia(email)) "Lista propia"
                    else "Compartida por ${lista.propietarioNombre.ifBlank { lista.propietario }}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    "${lista.miembros.size} miembro(s)",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            // Icono para Compartir lista
            IconButton(onClick = onCompartir) {
                Icon(
                    Icons.Default.Share,
                    contentDescription = "Compartir lista",
                    tint = MaterialTheme.colorScheme.primary
                )
            }

            // Icono para Eliminar o salir de la lista
            IconButton(onClick = { onEliminar(lista) }) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Eliminar lista",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}