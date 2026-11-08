package org.insbaixcamp.cestadecompra.ui

import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
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
 * Pantalla de detalle para gestionar los productos dentro de una categoría específica.
 * Permite marcar productos como completados (tachados), añadir nuevos y eliminarlos.
 */
@Composable
fun ProductosCategoria(
    lista: Lista,
    categoria: Categoria,
    onVolver: () -> Unit
) {
    val db = remember { FirebaseFirestore.getInstance() }

    val productosRef = remember(lista.id, categoria.id) {
        db.collection("listas")
            .document(lista.id)
            .collection("categorias")
            .document(categoria.id)
            .collection("productos")
    }

    var productos by remember(lista.id, categoria.id) {
        mutableStateOf(listOf<Producto>())
    }

    var nuevoProducto by remember {
        mutableStateOf("")
    }

    // Escuchar los productos de esta categoría en tiempo real
    DisposableEffect(lista.id, categoria.id) {
        val registro = productosRef.addSnapshotListener { snap, error ->
            if (error != null) {
                Log.e("Productos", error.message ?: "")
                return@addSnapshotListener
            }
            if (snap != null) {
                productos = snap.documents.mapNotNull { d ->
                    d.toObject(Producto::class.java)?.copy(id = d.id)
                }
            }
        }
        onDispose { registro.remove() }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        // Barra superior con botón volver y nombre de categoría
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp)
        ) {
            IconButton(onClick = onVolver) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Volver a categorías",
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }

            Spacer(Modifier.width(4.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = categoria.nombre,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${lista.nombre} • ${productos.count { !it.completado }} pendientes",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Campo para añadir un nuevo producto
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = nuevoProducto,
                onValueChange = { nuevoProducto = it },
                label = { Text("Nuevo producto") },
                placeholder = { Text("Ej: Manzanas") },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.weight(1f)
            )

            Spacer(Modifier.width(8.dp))

            Button(
                onClick = {
                    val nombre = nuevoProducto.trim()
                    if (nombre.isNotEmpty()) {
                        productosRef.add(
                            mapOf(
                                "nombre" to nombre,
                                "completado" to false
                            )
                        ).addOnSuccessListener {
                            nuevoProducto = ""
                        }.addOnFailureListener {
                            Log.e("Productos", "Error al añadir producto", it)
                        }
                    }
                },
                enabled = nuevoProducto.isNotBlank(),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.height(56.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(Modifier.width(4.dp))
                Text("Añadir")
            }
        }

        // Listado de productos
        if (productos.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                    modifier = Modifier.fillMaxWidth().padding(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "No hay productos en esta categoría.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "¡Escribe uno arriba para añadirlo!",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(productos, key = { it.id }) { producto ->
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (producto.completado) {
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                            } else {
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f)
                            }
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Checkbox para marcar como comprado
                            Checkbox(
                                checked = producto.completado,
                                onCheckedChange = { marcado ->
                                    productosRef.document(producto.id).update("completado", marcado)
                                }
                            )

                            // Nombre del producto (tachado si está completado)
                            Text(
                                text = producto.nombre,
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(horizontal = 8.dp),
                                style = if (producto.completado) {
                                    MaterialTheme.typography.bodyLarge.copy(
                                        textDecoration = TextDecoration.LineThrough,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                    )
                                } else {
                                    MaterialTheme.typography.bodyLarge.copy(
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            )

                            // Botón para borrar producto
                            IconButton(
                                onClick = { productosRef.document(producto.id).delete() },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    Icons.Default.Delete,
                                    contentDescription = "Eliminar producto",
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
