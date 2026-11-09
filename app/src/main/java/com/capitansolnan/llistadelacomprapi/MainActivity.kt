package com.capitansolnan.llistadelacomprapi

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.capitansolnan.llistadelacomprapi.ui.theme.LlistaDeLaCompraPITheme
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.capitansolnan.llistadelacomprapi.ui.screen.ListaScreen
import com.capitansolnan.llistadelacomprapi.ui.screen.LoginScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            LlistaDeLaCompraPITheme {
                val authViewModel: AuthViewModel = viewModel()
                val usuario by authViewModel.usuario.collectAsStateWithLifecycle()
                val context = LocalContext.current

                androidx.compose.material3.Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = androidx.compose.material3.MaterialTheme.colorScheme.background
                ) {
                    if (usuario == null) {
                        LoginScreen(authViewModel)
                    } else {
                        ListaScreen(onCerrarSesion = { authViewModel.cerrarSesion(context) })
                    }
                }
            }
        }
    }
}
