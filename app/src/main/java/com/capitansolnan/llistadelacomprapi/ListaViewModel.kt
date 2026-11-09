package com.capitansolnan.llistadelacomprapi

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.capitansolnan.llistadelacomprapi.ui.screen.EstadoListas
import com.capitansolnan.llistadelacomprapi.ui.screen.ProductoRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

@OptIn(ExperimentalCoroutinesApi::class)
class ListaViewModel : ViewModel() {

    private val repositorio = ProductoRepository()

    val estado: StateFlow<EstadoListas> = repositorio.estado.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(0),
        initialValue = EstadoListas()
    )

    val productos: StateFlow<List<Producto>> = estado
        .map { it.activa?.id }
        .distinctUntilChanged()
        .flatMapLatest { id ->
            if (id == null) flowOf(emptyList()) else repositorio.productosDe(id)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(0),
            initialValue = emptyList()
        )

    private val _mensaje = MutableStateFlow<String?>(null)
    val mensaje: StateFlow<String?> = _mensaje

    fun limpiarMensaje() {
        _mensaje.value = null
    }

    private fun listaActivaId(): String? = estado.value.activa?.id

    // ---- Productos ----

    fun añadir(nombre: String, cantidad: Int = 1) {
        val id = listaActivaId() ?: return
        if (nombre.isBlank()) return
        repositorio.insertar(id, Producto(nombre = nombre.trim(), cantidad = cantidad))
    }

    fun editar(producto: Producto, nombre: String, cantidad: Int) {
        val id = listaActivaId() ?: return
        if (nombre.isBlank()) return
        repositorio.actualizar(id, producto.copy(nombre = nombre.trim(), cantidad = cantidad))
    }

    fun marcar(producto: Producto) {
        val id = listaActivaId() ?: return
        repositorio.actualizar(id, producto.copy(comprado = !producto.comprado))
    }

    fun borrar(producto: Producto) {
        val id = listaActivaId() ?: return
        repositorio.borrar(id, producto)
    }

    fun borrarComprados() {
        val id = listaActivaId() ?: return
        repositorio.borrarComprados(id, productos.value)
    }

    // ---- Listas ----

    fun cambiarLista(id: String) {
        repositorio.cambiarActiva(id)
    }

    fun nuevaLista(nombre: String) {
        if (nombre.isBlank()) return
        repositorio.nuevaLista(nombre) { ok ->
            if (!ok) _mensaje.value = "No se pudo crear la lista"
        }
    }

    fun renombrarLista(nombre: String) {
        val id = listaActivaId() ?: return
        if (nombre.isBlank()) return
        repositorio.renombrar(id, nombre)
    }

    fun unirse(codigo: String, nombre: String) {
        val nombreFinal = nombre.ifBlank { "Lista compartida" }
        repositorio.unirse(codigo, nombreFinal) { ok ->
            _mensaje.value = if (ok) "Te has unido a la lista" else "Código no válido"
        }
    }

    fun eliminarLista() {
        val id = listaActivaId() ?: return
        repositorio.eliminar(id) { ok ->
            _mensaje.value = if (ok) "Lista eliminada" else "No se pudo eliminar la lista"
        }
    }
}