package org.insbaixcamp.cestadecompra.ui

import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.google.firebase.firestore.FirebaseFirestore
import org.insbaixcamp.cestadecompra.model.Categoria
import org.insbaixcamp.cestadecompra.model.Lista
import org.insbaixcamp.cestadecompra.model.Producto

/**
 * Pantalla que muestra las categorías pertenecientes a una lista de la compra específica.
 * Permite añadir nuevas categorías, editarlas, eliminarlas y acceder al detalle de productos.
 */
@Composable
fun ListaCompraScreen(
    lista: Lista,
    onVolver: () -> Unit
) {
    val db = remember { FirebaseFirestore.getInstance() }
    val categoriasRef = remember(lista.id) {
        db.collection("listas").document(lista.id).collection("categorias")
    }

    var categorias by remember(lista.id) { mutableStateOf(listOf<Categoria>()) }
    var nuevoNombre by remember { mutableStateOf("") }
    var categoriaEditando by remember { mutableStateOf<Categoria?>(null) }
    var nombreEditando by remember { mutableStateOf("") }
    var categoriaSeleccionada by remember { mutableStateOf<Categoria?>(null) }

    // Estado para confirmar el borrado de una categoría que contenga productos
    var categoriaPorEliminar by remember { mutableStateOf<Pair<Categoria, Int>?>(null) }

    // Manejar el botón físico o gesto de volver atrás en Android
    BackHandler {
        if (categoriaSeleccionada != null) {
            categoriaSeleccionada = null
        } else {
            onVolver()
        }
    }

    // Escuchar las categorías de la lista en Firestore en tiempo real
    DisposableEffect(lista.id) {
        val registro = categoriasRef.addSnapshotListener { snap, error ->
            if (error != null) {
                Log.e("Categorias", error.message ?: "")
                return@addSnapshotListener
            }
            if (snap != null) {
                categorias = snap.documents
                    .mapNotNull { d -> d.toObject(Categoria::class.java)?.copy(id = d.id) }
                    .sortedBy { it.nombre.lowercase() }
            }
        }
        onDispose { registro.remove() }
    }

    // Diálogo para editar el nombre de una categoría existente
    val editando = categoriaEditando
    if (editando != null) {
        AlertDialog(
            onDismissRequest = { categoriaEditando = null },
            title = { Text("Renombrar categoría") },
            text = {
                OutlinedTextField(
                    value = nombreEditando,
                    onValueChange = { nombreEditando = it },
                    label = { Text("Nombre de la categoría") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val n = nombreEditando.trim()
                        if (n.isNotBlank()) {
                            categoriasRef.document(editando.id).update("nombre", n)
                            categoriaEditando = null
                        }
                    },
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Guardar")
                }
            },
            dismissButton = {
                TextButton(onClick = { categoriaEditando = null }) {
                    Text("Cancelar")
                }
            }
        )
    }

    // Diálogo de confirmación para eliminar una categoría que contiene productos
    val porEliminar = categoriaPorEliminar
    if (porEliminar != null) {
        val (cat, numProds) = porEliminar
        AlertDialog(
            onDismissRequest = { categoriaPorEliminar = null },
            icon = { Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
            title = { Text("Eliminar categoría") },
            text = {
                Text("La categoría \"${cat.nombre}\" contiene $numProds producto(s). ¿Estás seguro de que deseas eliminarla junto con todos sus productos?")
            },
            confirmButton = {
                TextButton(onClick = {
                    eliminarCategoriaConProductos(db, lista.id, cat.id)
                    categoriaPorEliminar = null
                }) {
                    Text("Eliminar", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { categoriaPorEliminar = null }) {
                    Text("Cancelar")
                }
            }
        )
    }

    if (categoriaSeleccionada == null) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            // Barra superior de navegación y título
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp)
            ) {
                IconButton(onClick = onVolver) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Volver",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }

                Spacer(Modifier.width(4.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = lista.nombre,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Categorías",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            // Formulario para añadir una nueva categoría
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = nuevoNombre,
                    onValueChange = { nuevoNombre = it },
                    label = { Text("Nueva categoría") },
                    placeholder = { Text("Ej: Bebidas") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f)
                )

                Spacer(Modifier.width(8.dp))

                Button(
                    onClick = {
                        val nombre = nuevoNombre.trim()
                        if (nombre.isNotBlank()) {
                            categoriasRef.add(mapOf("nombre" to nombre))
                            nuevoNombre = ""
                        }
                    },
                    enabled = nuevoNombre.isNotBlank(),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.height(56.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(Modifier.width(4.dp))
                    Text("Añadir")
                }
            }

            // Listado de categorías
            if (categorias.isEmpty()) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
                ) {
                    Text(
                        text = "No hay categorías en esta lista. Escribe arriba para añadir una.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(categorias, key = { it.id }) { cat ->
                        CategoriaItem(
                            lista = lista,
                            cat = cat,
                            onEditar = {
                                nombreEditando = cat.nombre
                                categoriaEditando = cat
                            },
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
        }
    } else {
        // Pantalla de productos dentro de la categoría seleccionada
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
 * Borra una categoría junto con todos los productos que contiene en Firestore.
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

/**
 * Tarjeta individual para mostrar una categoría con su resumen de productos.
 */
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
                if (error != null) {
                    Log.e("Resumen", error.message ?: "")
                    return@addSnapshotListener
                }
                productos = snap?.documents
                    ?.mapNotNull { d -> d.toObject(Producto::class.java)?.copy(id = d.id) }
                    ?.sortedBy { it.completado } ?: emptyList()
            }
        onDispose { registro.remove() }
    }

    val textoGris = MaterialTheme.colorScheme.onSurfaceVariant

    Card(
        onClick = onAbrir,
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = cat.nombre,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f)
                )

                // Botón editar nombre
                IconButton(onClick = onEditar, modifier = Modifier.size(36.dp)) {
                    Icon(
                        Icons.Default.Edit,
                        contentDescription = "Editar categoría",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                }

                // Botón eliminar categoría
                IconButton(onClick = { onSolicitarEliminar(productos.size) }, modifier = Modifier.size(36.dp)) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Eliminar categoría",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(18.dp)
                    )
                }

                Spacer(Modifier.width(4.dp))

                Button(
                    onClick = onAbrir,
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text("Ver (${productos.size})")
                }
            }

            // Vista previa de los primeros productos
            if (productos.isEmpty()) {
                Text(
                    text = "Sin productos todavía",
                    color = textoGris,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 4.dp)
                )
            } else {
                Column(Modifier.padding(top = 6.dp)) {
                    productos.take(3).forEach { p ->
                        Text(
                            text = "• ${p.nombre}",
                            color = textoGris,
                            style = MaterialTheme.typography.bodySmall.copy(
                                textDecoration = if (p.completado) TextDecoration.LineThrough else TextDecoration.None
                            )
                        )
                    }
                    if (productos.size > 3) {
                        Text(
                            text = "+${productos.size - 3} producto(s) más",
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
