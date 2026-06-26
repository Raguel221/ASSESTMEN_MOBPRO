package com.raguel0087.myapplication1.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.raguel0087.myapplication1.auth.GoogleSignInHelper
import com.raguel0087.myapplication1.model.UserData
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class AuthUiState(
    val currentUser: UserData? = null,
    val isLoading: Boolean = false,
    val error: String? = null
)

class AuthViewModel : ViewModel() {

    private lateinit var googleSignInHelper: GoogleSignInHelper

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    /**
     * Must be called once from MainActivity before any auth operation.
     */
    fun init(context: Context) {
        if (!::googleSignInHelper.isInitialized) {
            googleSignInHelper = GoogleSignInHelper(context)
        }
        val firebaseUser = googleSignInHelper.getCurrentUser()
        if (firebaseUser != null) {
            _uiState.value = AuthUiState(
                currentUser = UserData(
                    uid = firebaseUser.uid,
                    displayName = firebaseUser.displayName ?: "Unknown",
                    email = firebaseUser.email ?: "",
                    photoUrl = firebaseUser.photoUrl?.toString()
                )
            )
        }
    }

    fun signIn(activityContext: Context) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            runCatching {
                googleSignInHelper.signIn(activityContext)
            }.onSuccess { firebaseUser ->
                _uiState.value = AuthUiState(
                    currentUser = UserData(
                        uid = firebaseUser.uid,
                        displayName = firebaseUser.displayName ?: "Unknown",
                        email = firebaseUser.email ?: "",
                        photoUrl = firebaseUser.photoUrl?.toString()
                    ),
                    isLoading = false
                )
            }.onFailure { throwable ->
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = throwable.message ?: "Sign-in failed."
                )
            }
        }
    }

    fun signOut() {
        viewModelScope.launch {
            runCatching { googleSignInHelper.signOut() }
            _uiState.value = AuthUiState(currentUser = null)
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
}
