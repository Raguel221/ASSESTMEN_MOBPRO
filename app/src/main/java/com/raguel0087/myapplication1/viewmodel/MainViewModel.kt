package com.raguel0087.myapplication1.viewmodel

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.Timestamp
import com.raguel0087.myapplication1.model.BibleVerse
import com.raguel0087.myapplication1.model.UserData
import com.raguel0087.myapplication1.repository.BibleRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

data class MainUiState(
    val verses: List<BibleVerse> = emptyList(),
    val filteredVerses: List<BibleVerse> = emptyList(),
    val isLoading: Boolean = false,
    val isUploading: Boolean = false,
    val error: String? = null,
    val searchQuery: String = "",
    val snackbarMessage: String? = null
)

class MainViewModel : ViewModel() {

    private val repository = BibleRepository()

    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    init {
        observeVerses()
    }

    // --- Realtime listener ---

    private fun observeVerses() {
        _uiState.value = _uiState.value.copy(isLoading = true, error = null)
        repository.getVerses()
            .onEach { result ->
                result.onSuccess { verses ->
                    val query = _uiState.value.searchQuery
                    _uiState.value = _uiState.value.copy(
                        verses = verses,
                        filteredVerses = applyFilter(verses, query),
                        isLoading = false,
                        error = null
                    )
                }.onFailure { error ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = error.message ?: "Failed to load verses."
                    )
                }
            }
            .catch { error ->
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = error.message ?: "Unexpected error."
                )
            }
            .launchIn(viewModelScope)
    }

    // --- Search ---

    fun onSearchQueryChange(query: String) {
        _uiState.value = _uiState.value.copy(
            searchQuery = query,
            filteredVerses = applyFilter(_uiState.value.verses, query)
        )
    }

    private fun applyFilter(verses: List<BibleVerse>, query: String): List<BibleVerse> {
        if (query.isBlank()) return verses
        val lower = query.lowercase()
        return verses.filter { verse ->
            verse.reference.lowercase().contains(lower) ||
                verse.verse.lowercase().contains(lower)
        }
    }

    // --- Refresh ---

    fun refresh() {
        observeVerses()
    }

    // --- Upload Image ---

    /**
     * Uploads image to Firebase Storage and returns the download URL.
     * Sets isUploading state so the UI can show a loading indicator.
     * @return download URL string, or null on failure.
     */
    suspend fun uploadImage(uri: Uri, context: Context): String? {
        _uiState.value = _uiState.value.copy(isUploading = true)
        val result = repository.uploadImage(uri, context)
        _uiState.value = _uiState.value.copy(isUploading = false)
        return result.getOrElse {
            _uiState.value = _uiState.value.copy(
                snackbarMessage = "Upload failed: ${it.message}"
            )
            null
        }
    }

    // --- Add Verse ---

    fun addVerse(
        verse: String,
        reference: String,
        imageUrl: String,
        currentUser: UserData
    ) {
        viewModelScope.launch {
            val newVerse = BibleVerse(
                verse = verse.trim(),
                reference = reference.trim(),
                imageUrl = imageUrl,
                ownerId = currentUser.uid,
                ownerName = currentUser.displayName,
                createdAt = Timestamp.now()
            )
            repository.addVerse(newVerse)
                .onSuccess {
                    _uiState.value = _uiState.value.copy(snackbarMessage = "Verse added successfully!")
                }
                .onFailure { error ->
                    _uiState.value = _uiState.value.copy(
                        snackbarMessage = "Failed to add verse: ${error.message}"
                    )
                }
        }
    }

    // --- Edit Verse ---

    fun updateVerse(
        verseId: String,
        newVerse: String,
        newReference: String,
        newImageUrl: String
    ) {
        viewModelScope.launch {
            val updated = BibleVerse(
                id = verseId,
                verse = newVerse.trim(),
                reference = newReference.trim(),
                imageUrl = newImageUrl
            )
            repository.updateVerse(verseId, updated)
                .onSuccess {
                    _uiState.value = _uiState.value.copy(snackbarMessage = "Verse updated successfully!")
                }
                .onFailure { error ->
                    _uiState.value = _uiState.value.copy(
                        snackbarMessage = "Failed to update verse: ${error.message}"
                    )
                }
        }
    }

    // --- Delete Verse ---

    fun deleteVerse(verseId: String) {
        viewModelScope.launch {
            repository.deleteVerse(verseId)
                .onSuccess {
                    _uiState.value = _uiState.value.copy(snackbarMessage = "Verse deleted.")
                }
                .onFailure { error ->
                    _uiState.value = _uiState.value.copy(
                        snackbarMessage = "Failed to delete verse: ${error.message}"
                    )
                }
        }
    }

    // --- State Helpers ---

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    fun clearSnackbar() {
        _uiState.value = _uiState.value.copy(snackbarMessage = null)
    }

    fun verseCountForUser(uid: String): Int =
        _uiState.value.verses.count { it.ownerId == uid }
}
