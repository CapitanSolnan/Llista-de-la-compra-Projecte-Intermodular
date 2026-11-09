package com.capitansolnan.llistadelacomprapi.ui.screen

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.capitansolnan.llistadelacomprapi.ListaViewModel
import com.capitansolnan.llistadelacomprapi.Producto
import com.capitansolnan.llistadelacomprapi.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ListaScreen(
    viewModel: ListaViewModel = viewModel(),
    onCerrarSesion: () -> Unit
) {
    val context = LocalContext.current
    val productos by viewModel.productos.collectAsStateWithLifecycle()
    val estado by viewModel.estado.collectAsStateWithLifecycle()
    val mensaje by viewModel.mensaje.collectAsStateWithLifecycle()
    val activa = estado.activa

    var mostrarDialogo by remember { mutableStateOf(false) }
    var productoEditando by remember { mutableStateOf<Producto?>(null) }

    var menuListas by remember { mutableStateOf(false) }
    var menuOpciones by remember { mutableStateOf(false) }
    var mostrarNueva by remember { mutableStateOf(false) }
    var mostrarRenombrar by remember { mutableStateOf(false) }
    var mostrarUnirse by remember { mutableStateOf(false) }
    var mostrarEliminar by remember { mutableStateOf(false) }
    var mostrarCompartir by remember { mutableStateOf(false) }

    LaunchedEffect(mensaje) {
        mensaje?.let {
            Toast.makeText(context, it, Toast.LENGTH_LONG).show()
            viewModel.limpiarMensaje()
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Box {
                        Row(
                            modifier = Modifier
                                .clickable { menuListas = true }
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = activa?.nombre ?: stringResource(R.string.titulo_lista),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(Modifier.width(4.dp))
                            Icon(Icons.Default.ArrowDropDown, contentDescription = "Canviar de llista")
                        }
                        DropdownMenu(
                            expanded = menuListas,
                            onDismissRequest = { menuListas = false }
                        ) {
                            estado.listas.forEach { lista ->
                                DropdownMenuItem(
                                    text = { Text(lista.nombre) },
                                    trailingIcon = {
                                        if (lista.id == activa?.id) {
                                            Icon(Icons.Default.Check, contentDescription = null)
                                        }
                                    },
                                    onClick = {
                                        menuListas = false
                                        viewModel.cambiarLista(lista.id)
                                    }
                                )
                            }
                            HorizontalDivider()
                            DropdownMenuItem(
                                text = { Text("Nova llista…") },
                                leadingIcon = { Icon(Icons.Default.Add, contentDescription = null) },
                                onClick = {
                                    menuListas = false
                                    mostrarNueva = true
                                }
                            )
                        }
                    }
                },
                actions = {
                    IconButton(onClick = {
                        if (activa == null) {
                            Toast.makeText(context, "Carregant llista...", Toast.LENGTH_SHORT).show()
                        } else {
                            mostrarCompartir = true
                        }
                    }) {
                        Icon(Icons.Default.Share, contentDescription = "Compartir llista")
                    }
                    Box {
                        IconButton(onClick = { menuOpciones = true }) {
                            Icon(Icons.Default.MoreVert, contentDescription = "Més opcions")
                        }
                        DropdownMenu(
                            expanded = menuOpciones,
                            onDismissRequest = { menuOpciones = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Renombrar llista") },
                                enabled = activa != null,
                                onClick = {
                                    menuOpciones = false
                                    mostrarRenombrar = true
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Unir-se a una llista") },
                                onClick = {
                                    menuOpciones = false
                                    mostrarUnirse = true
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Eliminar llista") },
                                enabled = activa != null,
                                onClick = {
                                    menuOpciones = false
                                    mostrarEliminar = true
                                }
                            )
                            HorizontalDivider()
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.delete)) },
                                onClick = {
                                    menuOpciones = false
                                    viewModel.borrarComprados()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.exit)) },
                                onClick = {
                                    menuOpciones = false
                                    onCerrarSesion()
                                }
                            )
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = {
                    productoEditando = null
                    mostrarDialogo = true
                },
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("Afegir producte") },
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
    ) { padding ->
        if (productos.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.padding(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ShoppingCart,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
                    )
                    Spacer(Modifier.height(16.dp))
                    Text(
                        text = "La llista està buida",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "Prem el botó flotant per afegir el teu primer producte.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(productos, key = { it.id }) { producto ->
                    ProductoItem(
                        producto = producto,
                        onMarcar = { viewModel.marcar(producto) },
                        onEditar = {
                            productoEditando = producto
                            mostrarDialogo = true
                        },
                        onBorrar = { viewModel.borrar(producto) }
                    )
                }
            }
        }
    }

    if (mostrarDialogo) {
        ProductoDialog(
            producto = productoEditando,
            onDismiss = { mostrarDialogo = false },
            onGuardar = { nombre, cantidad ->
                val editando = productoEditando
                if (editando == null) {
                    viewModel.añadir(nombre, cantidad)
                } else {
                    viewModel.editar(editando, nombre, cantidad)
                }
                mostrarDialogo = false
            }
        )
    }

    if (mostrarNueva) {
        NombreListaDialog(
            titulo = "Nova llista",
            onDismiss = { mostrarNueva = false },
            onConfirmar = {
                viewModel.nuevaLista(it)
                mostrarNueva = false
            }
        )
    }

    if (mostrarRenombrar) {
        NombreListaDialog(
            titulo = "Renombrar llista",
            nombreInicial = activa?.nombre ?: "",
            onDismiss = { mostrarRenombrar = false },
            onConfirmar = {
                viewModel.renombrarLista(it)
                mostrarRenombrar = false
            }
        )
    }

    if (mostrarUnirse) {
        UnirseDialog(
            onDismiss = { mostrarUnirse = false },
            onUnirse = { codigo, nombre ->
                viewModel.unirse(codigo, nombre)
                mostrarUnirse = false
            }
        )
    }

    if (mostrarEliminar && activa != null) {
        AlertDialog(
            onDismissRequest = { mostrarEliminar = false },
            title = { Text("Eliminar llista") },
            text = {
                Text(
                    "¿Eliminar \"${activa.nombre}\"? Si la comparteixes amb altres persones, " +
                            "elles la conserven. Si ets l'única, s'esborraran també els seus productes."
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    mostrarEliminar = false
                    viewModel.eliminarLista()
                }) { Text("Eliminar") }
            },
            dismissButton = {
                TextButton(onClick = { mostrarEliminar = false }) {
                    Text(stringResource(R.string.cancelar))
                }
            }
        )
    }

    if (mostrarCompartir && activa != null) {
        CompartirListaDialog(
            lista = activa,
            onDismiss = { mostrarCompartir = false }
        )
    }
}

@Composable
fun CompartirListaDialog(
    lista: ListaInfo,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val clipboardManager = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Compartir llista") },
        text = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Comparteix aquest codi amb qui vulguis perquè es pugui unir a \"${lista.nombre}\":",
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(Modifier.height(16.dp))

                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.medium,
                    color = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = lista.id,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.weight(1f)
                        )
                        Button(
                            onClick = {
                                val clip = ClipData.newPlainText("Codi de la llista", lista.id)
                                clipboardManager.setPrimaryClip(clip)
                                Toast.makeText(context, "Codi copiat al porta-papers!", Toast.LENGTH_SHORT).show()
                            },
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text("Copiar")
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))

                OutlinedButton(
                    onClick = {
                        compartirListaIntent(context, lista)
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Compartir per aplicacions (WhatsApp, etc.)")
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Tancar")
            }
        }
    )
}

private fun compartirListaIntent(context: Context, lista: ListaInfo) {
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(
            Intent.EXTRA_TEXT,
            "Et comparteixo la meva llista de la compra \"${lista.nombre}\". Obre l'app, ves al menú ⋮ → Unir-se a una llista i enganxa aquest codi:\n${lista.id}"
        )
    }
    context.startActivity(Intent.createChooser(intent, "Compartir llista"))
}

@Composable
fun NombreListaDialog(
    titulo: String,
    nombreInicial: String = "",
    onDismiss: () -> Unit,
    onConfirmar: (String) -> Unit
) {
    var nombre by remember { mutableStateOf(nombreInicial) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(titulo) },
        text = {
            OutlinedTextField(
                value = nombre,
                onValueChange = { nombre = it },
                label = { Text("Nom de la llista") },
                singleLine = true
            )
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirmar(nombre) },
                enabled = nombre.isNotBlank()
            ) { Text(stringResource(R.string.save)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancelar)) }
        }
    )
}

@Composable
fun UnirseDialog(
    onDismiss: () -> Unit,
    onUnirse: (codigo: String, nombre: String) -> Unit
) {
    var codigo by remember { mutableStateOf("") }
    var nombre by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Unir-se a una llista") },
        text = {
            Column {
                OutlinedTextField(
                    value = codigo,
                    onValueChange = { codigo = it },
                    label = { Text("Codi de la llista") },
                    singleLine = true
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = nombre,
                    onValueChange = { nombre = it },
                    label = { Text("Nom (opcional)") },
                    singleLine = true
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onUnirse(codigo, nombre) },
                enabled = codigo.isNotBlank()
            ) { Text("Unir-se") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancelar)) }
        }
    )
}

@Composable
fun ProductoItem(
    producto: Producto,
    onMarcar: () -> Unit,
    onEditar: () -> Unit,
    onBorrar: () -> Unit
) {
    val tachado = if (producto.comprado) TextDecoration.LineThrough else null
    val containerColor = if (producto.comprado) {
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    } else {
        MaterialTheme.colorScheme.surfaceContainerLow
    }
    val contentColor = if (producto.comprado) {
        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
    } else {
        MaterialTheme.colorScheme.onSurface
    }

    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        onClick = onEditar,
        colors = CardDefaults.elevatedCardColors(
            containerColor = containerColor
        ),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = producto.comprado,
                onCheckedChange = { onMarcar() }
            )
            Spacer(Modifier.width(8.dp))
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(vertical = 4.dp)
            ) {
                Text(
                    text = producto.nombre,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    textDecoration = tachado,
                    color = contentColor
                )
                Spacer(Modifier.height(2.dp))
                Surface(
                    shape = MaterialTheme.shapes.small,
                    color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = if (producto.comprado) 0.4f else 1f)
                ) {
                    Text(
                        text = "${stringResource(R.string.cantidad)}: ${producto.cantidad}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
            IconButton(onClick = onBorrar) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Borrar",
                    tint = MaterialTheme.colorScheme.error.copy(alpha = 0.8f)
                )
            }
        }
    }
}