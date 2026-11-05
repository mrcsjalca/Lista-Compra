package org.insbaixcamp.cestadecompra.ui

import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.google.firebase.firestore.FirebaseFirestore
import org.insbaixcamp.cestadecompra.model.Categoria
import org.insbaixcamp.cestadecompra.model.Lista

@Composable
fun ListaCompraScreen(lista: Lista, onVolver: () -> Unit) {
    val db = remember { FirebaseFirestore.getInstance() }
    val categoriasRef = remember(lista.id) {
        db.collection("listas").document(lista.id).collection("categorias")
    }
    var categorias by remember(lista.id) { mutableStateOf(listOf<Categoria>()) }
    var nuevoNombre by remember { mutableStateOf("") }
    var categoriaEditando by remember { mutableStateOf<Categoria?>(null) }
    var nombreEditando by remember { mutableStateOf("") }
    var categoriaSeleccionada by remember {mutableStateOf<Categoria?>(null)}


    BackHandler {
        if(categoriaSeleccionada != null) {
            categoriaSeleccionada = null
        } else {
            onVolver()
        }
    }

    DisposableEffect(lista.id) {
        val registro = categoriasRef.addSnapshotListener { snap, error ->
            if (error != null) { Log.e("Categorias", error.message ?: ""); return@addSnapshotListener }
            if (snap != null) {
                categorias = snap.documents
                    .mapNotNull { d -> d.toObject(Categoria::class.java)?.copy(id = d.id) }
                    .sortedBy { it.nombre.lowercase() }
            }
        }
        onDispose { registro.remove() }
    }

    val editando = categoriaEditando
    if (editando != null) {
        AlertDialog(
            onDismissRequest = { categoriaEditando = null },
            title = { Text("Editar categoría") },
            text = { OutlinedTextField(value = nombreEditando, onValueChange = { nombreEditando = it }) },
            confirmButton = {
                TextButton(onClick = {
                    if (nombreEditando.isNotBlank()) {
                        categoriasRef.document(editando.id).update("nombre", nombreEditando.trim())
                        categoriaEditando = null
                    }
                }) { Text("Guardar") }
            },
            dismissButton = { TextButton(onClick = { categoriaEditando = null }) { Text("Cancelar") } }
        )
    }

    if (categoriaSeleccionada == null) {

    Column(Modifier.statusBarsPadding().padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedButton(onClick = onVolver) { Text("Volver") }
            Text(lista.nombre, style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(start = 12.dp))
        }

        Text("Categorías", style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(top = 16.dp))

        Row(Modifier.padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = nuevoNombre,
                onValueChange = { nuevoNombre = it },
                label = { Text("Nueva categoría") },
                modifier = Modifier.weight(1f)
            )
            Button(
                onClick = {
                    if (nuevoNombre.isNotBlank()) {
                        categoriasRef.add(mapOf("nombre" to nuevoNombre.trim()))
                        nuevoNombre = ""
                    }
                },
                modifier = Modifier.padding(start = 8.dp)
            ) { Text("Añadir") }
        }

        LazyColumn {
            items(categorias, key = { it.id }) { cat ->
                Row(Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically) {
                    Text(cat.nombre, Modifier.weight(1f))
                    TextButton(onClick = { nombreEditando = cat.nombre; categoriaEditando = cat }) { Text("Editar") }
                    TextButton(onClick = { categoriasRef.document(cat.id).delete() }) { Text("Eliminar") }
                    Button(
                        onClick = {
                            categoriaSeleccionada = cat
                        }
                    ) {
                        Text("Ver productos")
                    }
                }
            }
        }
    } } else {
        ProductosCategoria(
            lista = lista,
            categoria = categoriaSeleccionada!!,
            onVolver = {
                categoriaSeleccionada = null
            }
        )
    }
}