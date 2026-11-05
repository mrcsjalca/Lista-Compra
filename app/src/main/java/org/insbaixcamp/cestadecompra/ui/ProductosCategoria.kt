package org.insbaixcamp.cestadecompra.ui

import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
    val db = remember {  FirebaseFirestore.getInstance() }

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
        //addSnapshotListener para escuchar datos en tiempo real
        val registro = productosRef.addSnapshotListener {snap, error ->
            if (error != null) {
                Log.e("Productos", error.message ?: "")
                return@addSnapshotListener
            }
        // esto sirve para crear datos crudos de firabase firestore a objetos de kotlin
            if (snap != null) {
                productos = snap.documents.mapNotNull { d ->
                    d.toObject(Producto::class.java)
                        ?.copy(id = d.id)
                }
        }
    }
        onDispose { registro.remove() }
    }
    Column(
        Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {

        // Título y botón volver
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedButton(onClick = onVolver) {
                Text("Volver")
            }

            Text(
                categoria.nombre,
                modifier = Modifier.padding(start = 12.dp),
                style = MaterialTheme.typography.titleLarge
            )
        }

        // Campo para introducir productos
        Row(
            Modifier.padding(vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {

            OutlinedTextField(
                value = nuevoProducto,
                onValueChange = { nuevoProducto = it },
                label = { Text("Nuevo producto") },
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
                modifier = Modifier.padding(start = 8.dp)
            ) {
                Text("Añadir")
            }
        }

        // Mostramos los productos
        LazyColumn {

            items(productos, key = { it.id }) { producto ->

                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {

                    // Marcar como completado
                    Checkbox(
                        checked = producto.completado,
                        onCheckedChange = { marcado ->

                            productosRef
                                .document(producto.id)
                                .update("completado", marcado)
                        }
                    )

                    Text(
                        producto.nombre,
                        modifier = Modifier.weight(1f),
                        style = if (producto.completado) {
                            MaterialTheme.typography.bodyMedium.copy(
                                textDecoration = TextDecoration.LineThrough
                            )
                        } else {
                            MaterialTheme.typography.bodyMedium
                        }
                    )

                    // Eliminar producto
                    TextButton(
                        onClick = {
                            productosRef
                                .document(producto.id)
                                .delete()
                        }
                    ) {
                        Text("Eliminar")
                    }
                }
            }
        }
    }
}