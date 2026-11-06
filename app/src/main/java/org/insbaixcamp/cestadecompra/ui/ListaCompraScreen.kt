package org.insbaixcamp.cestadecompra.ui

import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.google.firebase.firestore.FirebaseFirestore
import org.insbaixcamp.cestadecompra.model.Categoria
import org.insbaixcamp.cestadecompra.model.Lista
import org.insbaixcamp.cestadecompra.model.Producto

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
    var categoriaSeleccionada by remember { mutableStateOf<Categoria?>(null) }

    // Estado para confirmación de eliminación de categoría con productos
    var categoriaPorEliminar by remember { mutableStateOf<Pair<Categoria, Int>?>(null) }

    BackHandler {
        if (categoriaSeleccionada != null) {
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

    // Diálogo para editar nombre de categoría
    val editando = categoriaEditando
    if (editando != null) {
        AlertDialog(
            onDismissRequest = { categoriaEditando = null },
            title = { Text("Editar categoría") },
            text = {
                OutlinedTextField(
                    value = nombreEditando,
                    onValueChange = { nombreEditando = it },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
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

    // Diálogo de advertencia para borrar categoría con productos
    val porEliminar = categoriaPorEliminar
    if (porEliminar != null) {
        val (cat, numProds) = porEliminar
        AlertDialog(
            onDismissRequest = { categoriaPorEliminar = null },
            title = { Text("Eliminar categoría") },
            text = {
                Text("La categoría \"${cat.nombre}\" contiene $numProds producto(s). ¿Estás seguro de que deseas eliminarla junto con todos sus productos?")
            },
            confirmButton = {
                TextButton(onClick = {
                    eliminarCategoriaConProductos(db, lista.id, cat.id)
                    categoriaPorEliminar = null
                }) { Text("Eliminar", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { categoriaPorEliminar = null }) { Text("Cancelar") }
            }
        )
    }

    if (categoriaSeleccionada == null) {
        Column(Modifier.statusBarsPadding().padding(16.dp)) {
            // Cabecera superior cómoda
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 8.dp)
            ) {
                OutlinedButton(onClick = onVolver) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Volver",
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text("Volver")
                }
                Text(
                    lista.nombre,
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.padding(start = 12.dp)
                )
            }

            Text(
                "Categorías de la lista",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
            )

            // Campo para añadir categoría
            Row(
                modifier = Modifier.padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = nuevoNombre,
                    onValueChange = { nuevoNombre = it },
                    label = { Text("Añadir categoría") },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                Button(
                    onClick = {
                        if (nuevoNombre.isNotBlank()) {
                            categoriasRef.add(mapOf("nombre" to nuevoNombre.trim()))
                            nuevoNombre = ""
                        }
                    },
                    modifier = Modifier
                        .padding(start = 8.dp)
                        .height(54.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(Modifier.width(4.dp))
                    Text("Añadir")
                }
            }

            LazyColumn(modifier = Modifier.weight(1f)) {
                items(categorias, key = { it.id }) { cat ->
                    CategoriaItem(
                        lista = lista,
                        cat = cat,
                        onEditar = { nombreEditando = cat.nombre; categoriaEditando = cat },
                        onSolicitarEliminar = { numProds ->
                            if (numProds > 0) {
                                categoriaPorEliminar = Pair(cat, numProds)
                            } else {
                                categoriasRef.document(cat.id).delete()
                            }
                        },
                        onAbrir = { categoriaSeleccionada = cat }
                    )
                }
            }
        }
    } else {
        ProductosCategoria(
            lista = lista,
            categoria = categoriaSeleccionada!!,
            onVolver = {
                categoriaSeleccionada = null
            }
        )
    }
}

/**
 * Borra una categoría y sus subcolecciones de productos en Firestore
 */
fun eliminarCategoriaConProductos(db: FirebaseFirestore, listaId: String, categoriaId: String) {
    val productosRef = db.collection("listas").document(listaId)
        .collection("categorias").document(categoriaId)
        .collection("productos")

    productosRef.get().addOnSuccessListener { snap ->
        val batch = db.batch()
        snap.documents.forEach { doc ->
            batch.delete(doc.reference)
        }
        val catDoc = db.collection("listas").document(listaId)
            .collection("categorias").document(categoriaId)
        batch.delete(catDoc)
        batch.commit()
    }.addOnFailureListener {
        db.collection("listas").document(listaId)
            .collection("categorias").document(categoriaId).delete()
    }
}

@Composable
fun CategoriaItem(
    lista: Lista,
    cat: Categoria,
    onEditar: () -> Unit,
    onSolicitarEliminar: (Int) -> Unit,
    onAbrir: () -> Unit
) {
    val db = remember { FirebaseFirestore.getInstance() }
    var productos by remember(lista.id, cat.id) { mutableStateOf(listOf<Producto>()) }

    DisposableEffect(lista.id, cat.id) {
        val registro = db.collection("listas").document(lista.id)
            .collection("categorias").document(cat.id)
            .collection("productos")
            .addSnapshotListener { snap, error ->
                if (error != null) { Log.e("Resumen", error.message ?: ""); return@addSnapshotListener }
                productos = snap?.documents
                    ?.mapNotNull { d -> d.toObject(Producto::class.java)?.copy(id = d.id) }
                    ?.sortedBy { it.completado } ?: emptyList()
            }
        onDispose { registro.remove() }
    }

    val gris = MaterialTheme.colorScheme.onSurfaceVariant

    Card(
        onClick = onAbrir,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    cat.nombre,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f)
                )

                IconButton(onClick = onEditar) {
                    Icon(
                        Icons.Default.Edit,
                        contentDescription = "Editar categoría",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                }

                IconButton(onClick = { onSolicitarEliminar(productos.size) }) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Eliminar categoría",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Button(
                    onClick = onAbrir,
                    modifier = Modifier.padding(start = 4.dp)
                ) {
                    Text("Ver (${productos.size})")
                }
            }

            if (productos.isEmpty()) {
                Text(
                    "Sin productos en esta categoría",
                    color = gris,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 4.dp)
                )
            } else {
                Column(Modifier.padding(top = 4.dp)) {
                    productos.take(3).forEach { p ->
                        Text(
                            "• ${p.nombre}",
                            color = gris,
                            style = MaterialTheme.typography.bodySmall.copy(
                                textDecoration = if (p.completado) TextDecoration.LineThrough else TextDecoration.None
                            )
                        )
                    }
                    if (productos.size > 3) {
                        Text(
                            "+${productos.size - 3} producto(s) más",
                            color = MaterialTheme.colorScheme.primary,
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }
                }
            }
        }
    }
}