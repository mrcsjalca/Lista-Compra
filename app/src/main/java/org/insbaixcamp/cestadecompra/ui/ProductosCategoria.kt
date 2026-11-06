package org.insbaixcamp.cestadecompra.ui

import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.google.firebase.firestore.FirebaseFirestore
import org.insbaixcamp.cestadecompra.model.Lista
import org.insbaixcamp.cestadecompra.model.Categoria
import org.insbaixcamp.cestadecompra.model.Producto

@Composable
fun ProductosCategoria(
    lista: Lista,
    categoria: Categoria,
    onVolver: () -> Unit
){
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
        Modifier
            .statusBarsPadding()
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Título y botón volver
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
                categoria.nombre,
                modifier = Modifier.padding(start = 12.dp),
                style = MaterialTheme.typography.titleLarge
            )
        }

        // Campo para añadir nuevo producto
        Row(
            modifier = Modifier.padding(vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = nuevoProducto,
                onValueChange = { nuevoProducto = it },
                label = { Text("Nuevo producto") },
                singleLine = true,
                modifier = Modifier.weight(1f)
            )

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
                            Log.e("Productos", "Error al añadir", it)
                        }
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

        // Lista de productos cómodos
        if (productos.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "No hay productos en esta categoría.\n¡Escribe uno arriba para añadirlo!",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(modifier = Modifier.weight(1f)) {
                items(productos, key = { it.id }) { producto ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (producto.completado) {
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                            } else {
                                MaterialTheme.colorScheme.surfaceVariant
                            }
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = producto.completado,
                                onCheckedChange = { marcado ->
                                    productosRef
                                        .document(producto.id)
                                        .update("completado", marcado)
                                }
                            )

                            Text(
                                text = producto.nombre,
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(start = 8.dp),
                                style = if (producto.completado) {
                                    MaterialTheme.typography.bodyLarge.copy(
                                        textDecoration = TextDecoration.LineThrough,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                    )
                                } else {
                                    MaterialTheme.typography.bodyLarge
                                }
                            )

                            IconButton(
                                onClick = {
                                    productosRef
                                        .document(producto.id)
                                        .delete()
                                }
                            ) {
                                Icon(
                                    Icons.Default.Delete,
                                    contentDescription = "Eliminar producto",
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