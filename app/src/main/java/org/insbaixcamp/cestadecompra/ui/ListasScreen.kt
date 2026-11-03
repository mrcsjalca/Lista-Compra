package org.insbaixcamp.cestadecompra.ui

import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.room.util.copy
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import org.insbaixcamp.cestadecompra.model.Lista
import org.insbaixcamp.cestadecompra.model.aUsuario

/**
 * codigo para poder cargar la vista de las listas
 * contiene un boton para cerra sesion
 * un apartado para crear listas
 * las diferentes categorias
 * cargamos las listas con un foreach
 * y vemos las listas que tenemso compartidas
 */

@Composable
fun ListasScreen(
    email: String,
    onAbrirLista: (Lista) -> Unit,
    onSalir: () -> Unit
) {
    //obtenemos la instancia de firestore, y con rembeber hacemos que no se olvide
    val db = remember { FirebaseFirestore.getInstance() }
    val usuario = remember { FirebaseAuth.getInstance().currentUser?.aUsuario() }
    var listas by remember { mutableStateOf(listOf<Lista>()) }
    var nuevoNombre by remember { mutableStateOf("") }

    // filtramos las listas por si son nuestras o de otra persona, esto lo hacemos con un boolean metido dentro de la clase lista
    val propias = listas.filter { it.esPropia(email) }
    val compartidas = listas.filter { !it.esPropia(email) }

    //El disposableEffect y el onDispose registra el listener y lo quita al salir de la pantalla para no acumular listeners

    DisposableEffect(email) {
        val registro = db.collection("listas")
            //filtra la lista para saber las cestas del usuario
            .whereArrayContains("miembros", email)
            //addSnapshot va revisando los cambios que se producen al cambiar de documento
            .addSnapshotListener { snap, error ->
                //de esta forma si ocurre un error, con el log.e muestra el registro en el android studio
                if (error != null) { Log.e("Listas", error.message ?: ""); return@addSnapshotListener }
                // este codigo convierte los datos que vienen de firebase a kotlin
                listas = snap?.documents?.mapNotNull { d ->
                    d.toObject(Lista::class.java)?.copy(id = d.id)
                } ?: emptyList()
            }
        onDispose { registro.remove() }
    }

    Column(Modifier.statusBarsPadding().padding(16.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(usuario?.nombre ?: "Usuario", style = MaterialTheme.typography.titleLarge)
                Text(usuario?.email ?: email, style = MaterialTheme.typography.bodySmall)
            }
            TextButton(onClick = onSalir) { Text("Cerrar sesión") }
        }

        Row(Modifier.padding(vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = nuevoNombre,
                onValueChange = { nuevoNombre = it },
                label = { Text("Nombre de lista") },
                modifier = Modifier.weight(1f)
            )
            Button(
                onClick = {
                    // Comprobamos que el nombre no esté vacío
                    val nombre = nuevoNombre.trim()
                    if (nombre.isNotEmpty()) {
                        // Creamos un documento nuevo en Firestore
                        db.collection("listas").add(
                            mapOf(
                                "nombre" to nombre,
                                "propietario" to email,
                                "propietarioNombre" to (usuario?.nombre ?: email),
                                "miembros" to listOf(email)
                            )
                        ).addOnSuccessListener { nuevoNombre = "" }
                            // Limpiamos el campo si se ha creado correctamente
                            .addOnFailureListener { Log.e("Listas", "Error al crear", it) }
                    }
                },
                modifier = Modifier.padding(start = 8.dp)
            ) { Text("Crear") }
        }

        //parecido a un forach pero muestar los elementos en una listas
        LazyColumn(Modifier.weight(1f)) {
            item { Text("Mis listas", style = MaterialTheme.typography.titleMedium) }
            if (propias.isEmpty()) item { Text("Aún no tienes listas") }
            items(propias, key = { it.id }) { ListaItem(it, email, onAbrirLista) }

            item { Text("Compartidas conmigo", style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(top = 16.dp)) }
            if (compartidas.isEmpty()) item { Text("Nadie ha compartido listas contigo") }
            items(compartidas, key = { it.id }) { ListaItem(it, email, onAbrirLista) }
        }
    }
}

@Composable
fun ListaItem(lista: Lista, email: String, onAbrir: (Lista) -> Unit) {
    Card(
        onClick = { onAbrir(lista) },
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(lista.nombre, style = MaterialTheme.typography.titleMedium)
            Text(
                if (lista.esPropia(email)) "Tuya"
                else "Compartida por ${lista.propietarioNombre.ifBlank { lista.propietario }}"
            )
            Text("${lista.miembros.size} miembro(s)")
        }
    }
}