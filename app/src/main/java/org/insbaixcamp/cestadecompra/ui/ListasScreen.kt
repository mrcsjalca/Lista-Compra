package org.insbaixcamp.cestadecompra.ui

import android.util.Log
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.google.firebase.firestore.FirebaseFirestore
import androidx.compose.foundation.clickable
import androidx.compose.material3.OutlinedButton
import androidx.compose.ui.tooling.preview.Preview

data class Lista(
    val id: String = "",
    val nombre: String = "",
    val propietario: String = "",
    val miembros: List<String> = emptyList()
)


@Composable
fun ListasScreen(
    email : String,
    onAbrirLista: (Lista) -> Unit,
    onSalir: () -> Unit
) {
    //obtenemos la instancia de firestore, y con rembeber hacemos que no se olvide
    val db = remember { FirebaseFirestore.getInstance() }
    var listas by remember { mutableStateOf(listOf<Lista>()) }
    var cambioDeNombre by remember {mutableStateOf("")}

   //El disposableEffect y el onDispose registra el listener y lo quita al salir de la pantalla para no acumular listeners
    DisposableEffect(email){
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

    Column(
        modifier = Modifier.padding(16.dp)
    ) {
        OutlinedTextField(
            value = cambioDeNombre,
            onValueChange = { texto ->
                cambioDeNombre = texto
            },
            label = {
                Text("Nombre de Lista")
            }
        )

        Button(
            onClick = {
                // Comprobamos que el nombre no esté vacío
                val nombre = cambioDeNombre.trim()

                if (nombre.isNotEmpty()) {
                    // Creamos un documento nuevo en Firestore
                    db.collection("listas").add(
                        mapOf(
                            "nombre" to nombre,
                            "propietario" to email,
                            "miembros" to listOf(email)
                        )
                    ).addOnSuccessListener {
                        // Limpiamos el campo si se ha creado correctamente
                        cambioDeNombre = ""
                    }.addOnFailureListener { error ->
                        Log.e("Listas", "Error al crear la lista", error)
                    }
                }
            }
        ) {
            Text("Crear")
        }



        //parecido a un forach pero muestar los elementos en una listas
        LazyColumn {
            items(listas, key = { it.id }) { lista ->
                Text(
                    text = lista.nombre,
                    modifier = Modifier
                        .clickable { onAbrirLista(lista) }
                        .padding(8.dp)
                )
            }
        }

        OutlinedButton(onClick = onSalir){
            Text("Cerra Sesion")
        }
    }
}

