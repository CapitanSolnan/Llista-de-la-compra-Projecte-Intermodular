package com.capitansolnan.llistadelacomprapi

import android.content.Context
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialException
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class AuthViewModel : ViewModel() {

    private val auth = FirebaseAuth.getInstance()

    private val _usuario = MutableStateFlow<FirebaseUser?>(auth.currentUser)
    val usuario: StateFlow<FirebaseUser?> = _usuario

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    fun iniciarSesion(context: Context, webClientId: String) {
        viewModelScope.launch {
            try {
                val opcion = GetSignInWithGoogleOption.Builder(webClientId).build()
                val peticion = GetCredentialRequest.Builder()
                    .addCredentialOption(opcion)
                    .build()

                val resultado = CredentialManager.create(context)
                    .getCredential(context, peticion)
                val credencial = resultado.credential

                if (credencial is CustomCredential &&
                    credencial.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
                ) {
                    val google = GoogleIdTokenCredential.createFrom(credencial.data)
                    val firebase = GoogleAuthProvider.getCredential(google.idToken, null)
                    auth.signInWithCredential(firebase).addOnCompleteListener { tarea ->
                        if (tarea.isSuccessful) {
                            _usuario.value = auth.currentUser
                            _error.value = null
                        } else {
                            _error.value = tarea.exception?.message ?: "Error al iniciar sesión"
                        }
                    }
                } else {
                    _error.value = "Tipo de credencial no reconocido"
                }
            } catch (e: GetCredentialException) {
                _error.value = e.message ?: "No se pudo iniciar sesión"
            }
        }
    }

    fun cerrarSesion(context: Context) {
        auth.signOut()
        _usuario.value = null
        viewModelScope.launch {
            CredentialManager.create(context).clearCredentialState(ClearCredentialStateRequest())
        }
    }
}