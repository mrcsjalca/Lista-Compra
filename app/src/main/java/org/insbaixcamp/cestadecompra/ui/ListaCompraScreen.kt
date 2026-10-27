
package org.insbaixcamp.cestadecompra.ui

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.google.firebase.firestore.FirebaseFirestore
import org.insbaixcamp.cestadecompra.model.Categoria

@Composable
fun ListaCompraScreen(
    lista: Lista,
    onVolver: () -> Unit
) {
    val db = remember {
        FirebaseFirestore.getInstance()
    }

    val categoriasRef = remember(lista.id) {
        db.collection("listas")
            .document(lista.id)
            .collection("categorias")
    }

    var categorias by remember(lista.id) {
        mutableStateOf(listOf<Categoria>())
    }

    var nuevoNombre by remember {
        mutableStateOf("")
    }

    var categoriaEditando by remember {
        mutableStateOf<Categoria?>(null)
    }

    var nombreEditando by remember {
        mutableStateOf("")
    }

    BackHandler {
        onVolver()
    }

    DisposableEffect(lista.id) {
        val registro = categoriasRef.addSnapshotListener { snap, error ->
            if (error != null) {
                return@addSnapshotListener
            }

            if (snap != null) {
                categorias = snap.documents.mapNotNull { documento ->
                    documento.toObject(Categoria::class.java)
                }
            }
        }

        onDispose {
            registro.remove()
        }
    }
}
