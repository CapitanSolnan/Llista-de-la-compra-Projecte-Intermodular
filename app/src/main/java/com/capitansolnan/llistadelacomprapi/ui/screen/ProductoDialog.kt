package com.capitansolnan.llistadelacomprapi.ui.screen

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.capitansolnan.llistadelacomprapi.Producto
import com.capitansolnan.llistadelacomprapi.R

@Composable
fun ProductoDialog(
    producto: Producto?,
    onDismiss: () -> Unit,
    onGuardar: (nombre: String, cantidad: Int) -> Unit
) {
    var nombre by remember { mutableStateOf(producto?.nombre ?: "") }
    var cantidad by remember { mutableStateOf((producto?.cantidad ?: 1).toString()) }

    val cantidadInt = cantidad.toIntOrNull()
    val valido = nombre.isNotBlank() && cantidadInt != null && cantidadInt > 0

    AlertDialog(
        onDismissRequest = onDismiss,

        title = { Text(if (producto == null)  stringResource(R.string.add) else stringResource(R.string.edit)) },
        text = {
            Column {
                OutlinedTextField(
                    value = nombre,
                    onValueChange = { nombre = it },
                    label = { Text(stringResource(R.string.name)) },
                    singleLine = true
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = cantidad,
                    onValueChange = { cantidad = it.filter(Char::isDigit) },
                    label = { Text(stringResource(R.string.cantidad)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onGuardar(nombre, cantidadInt ?: 1) },
                enabled = valido
            ) { Text(stringResource(R.string.save)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancelar)) }
        }
    )
}