package com.capitansolnan.llistadelacomprapi.ui.screen

import android.util.Log
import com.capitansolnan.llistadelacomprapi.Producto
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

data class ListaInfo(val id: String, val nombre: String)

data class EstadoListas(
    val listas: List<ListaInfo> = emptyList(),
    val activa: ListaInfo? = null
)

class ProductoRepository {

    private val db = FirebaseDatabase.getInstance()
    private val tag = "ProductoRepository"

    private fun uid() = FirebaseAuth.getInstance().currentUser?.uid

    val estado: Flow<EstadoListas> = callbackFlow {
        val u = uid()
        if (u == null) {
            trySend(EstadoListas())
            awaitClose { }
            return@callbackFlow
        }
        val ref = db.getReference("usuarios").child(u)
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val listas = snapshot.child("listas").children.mapNotNull { s ->
                    val nombre = s.getValue(String::class.java) ?: return@mapNotNull null
                    val id = s.key ?: return@mapNotNull null
                    ListaInfo(id, nombre)
                }.sortedBy { it.nombre.lowercase() }

                if (listas.isEmpty()) {
                    val legado = snapshot.child("listaId").getValue(String::class.java)
                    if (legado != null) adoptarLegado(u, legado) else crearLista(u, "Mi lista")
                    trySend(EstadoListas())
                    return
                }
                val activaId = snapshot.child("listaActiva").getValue(String::class.java)
                val activa = listas.firstOrNull { it.id == activaId } ?: listas.first()
                trySend(EstadoListas(listas, activa))
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e(tag, "Error al llegir les llistes: ${error.message}")
                trySend(EstadoListas())
            }
        }
        ref.addValueEventListener(listener)
        awaitClose { ref.removeEventListener(listener) }
    }

    private fun adoptarLegado(u: String, listaId: String) {
        val cambios = mapOf<String, Any?>(
            "usuarios/$u/listas/$listaId" to "Mi lista",
            "usuarios/$u/listaActiva" to listaId,
            "usuarios/$u/listaId" to null
        )
        db.reference.updateChildren(cambios)
            .addOnFailureListener { Log.e(tag, "Error al adoptar la llista anterior", it) }
    }

    private fun crearLista(u: String, nombre: String, onResultado: ((Boolean) -> Unit)? = null) {
        val id = db.getReference("listas").push().key
        if (id == null) {
            onResultado?.invoke(false)
            return
        }
        val cambios = mapOf(
            "listas/$id/info/creador" to u,
            "listas/$id/miembros/$u" to true,
            "usuarios/$u/listas/$id" to nombre,
            "usuarios/$u/listaActiva" to id
        )
        db.reference.updateChildren(cambios).addOnCompleteListener {
            if (!it.isSuccessful) Log.e(tag, "Error al crear la llista", it.exception)
            onResultado?.invoke(it.isSuccessful)
        }
    }

    fun nuevaLista(nombre: String, onResultado: (Boolean) -> Unit) {
        val u = uid()
        if (u == null) onResultado(false) else crearLista(u, nombre.trim(), onResultado)
    }

    fun cambiarActiva(listaId: String) {
        val u = uid() ?: return
        db.getReference("usuarios").child(u).child("listaActiva").setValue(listaId)
    }

    fun renombrar(listaId: String, nombre: String) {
        val u = uid() ?: return
        db.getReference("usuarios").child(u).child("listas").child(listaId).setValue(nombre.trim())
    }

    fun productosDe(listaId: String): Flow<List<Producto>> = callbackFlow {
        val ref = db.getReference("listas").child(listaId).child("productos")
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val lista = snapshot.children
                    .mapNotNull { it.getValue(Producto::class.java)?.copy(id = it.key ?: "") }
                    .sortedWith(compareBy({ it.comprado }, { it.nombre.lowercase() }))
                trySend(lista)
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e(tag, "Error al llegir productes: ${error.message}")
                trySend(emptyList())
            }
        }
        ref.addValueEventListener(listener)
        awaitClose { ref.removeEventListener(listener) }
    }

    private fun productos(listaId: String) =
        db.getReference("listas").child(listaId).child("productos")

    fun insertar(listaId: String, producto: Producto) {
        val ref = productos(listaId)
        val clave = ref.push().key ?: return
        ref.child(clave).setValue(producto.copy(id = clave))
            .addOnFailureListener { Log.e(tag, "Error al inserir", it) }
    }

    fun actualizar(listaId: String, producto: Producto) {
        productos(listaId).child(producto.id).setValue(producto)
            .addOnFailureListener { Log.e(tag, "Error al actualitzar", it) }
    }

    fun borrar(listaId: String, producto: Producto) {
        productos(listaId).child(producto.id).removeValue()
            .addOnFailureListener { Log.e(tag, "Error al esborrar", it) }
    }

    fun borrarComprados(listaId: String, lista: List<Producto>) {
        val ref = productos(listaId)
        lista.filter { it.comprado }.forEach { ref.child(it.id).removeValue() }
    }

    fun unirse(codigo: String, nombre: String, onResultado: (Boolean) -> Unit) {
        val u = uid()
        val id = codigo.trim()
        if (u == null || id.isEmpty() || id.contains(Regex("""[.#$\[\]/]"""))) {
            onResultado(false)
            return
        }
        val cambios = mapOf(
            "listas/$id/miembros/$u" to true,
            "usuarios/$u/listas/$id" to nombre.trim(),
            "usuarios/$u/listaActiva" to id
        )
        db.reference.updateChildren(cambios)
            .addOnCompleteListener { onResultado(it.isSuccessful) }
    }

    fun eliminar(listaId: String, onResultado: (Boolean) -> Unit) {
        val u = uid()
        if (u == null) {
            onResultado(false)
            return
        }
        db.getReference("listas").child(listaId).child("miembros").get()
            .addOnSuccessListener { snap ->
                val soloYo = snap.childrenCount <= 1
                val cambios = mutableMapOf<String, Any?>("usuarios/$u/listas/$listaId" to null)
                if (soloYo) cambios["listas/$listaId"] = null
                else cambios["listas/$listaId/miembros/$u"] = null
                db.reference.updateChildren(cambios)
                    .addOnCompleteListener { onResultado(it.isSuccessful) }
            }
            .addOnFailureListener {
                db.getReference("usuarios").child(u).child("listas").child(listaId).removeValue()
                onResultado(true)
            }
    }
}
