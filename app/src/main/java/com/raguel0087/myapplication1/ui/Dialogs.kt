package com.raguel0087.myapplication1.ui

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.raguel0087.myapplication1.model.BibleVerse
import com.raguel0087.myapplication1.model.UserData
import com.raguel0087.myapplication1.viewmodel.MainViewModel
import kotlinx.coroutines.launch

// ---------------------------------------------------------------------------
//  Add Verse Dialog
// ---------------------------------------------------------------------------

@Composable
fun AddVerseDialog(
    isUploading: Boolean,
    currentUser: UserData,
    mainViewModel: MainViewModel,
    onDismiss: () -> Unit
) {
    var reference by remember { mutableStateOf("") }
    var verse by remember { mutableStateOf("") }
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    var uploadedImageUrl by remember { mutableStateOf<String?>(null) }
    var referenceError by remember { mutableStateOf(false) }
    var verseError by remember { mutableStateOf(false) }
    var imageError by remember { mutableStateOf(false) }

    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current

    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        uri?.let {
            selectedImageUri = it
            uploadedImageUrl = null // reset until uploaded
            coroutineScope.launch {
                val url = mainViewModel.uploadImage(it, context)
                uploadedImageUrl = url
            }
        }
    }

    AlertDialog(
        onDismissRequest = { if (!isUploading) onDismiss() },
        title = { Text("Add Bible Verse", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                // Image picker area
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .border(
                            width = if (imageError) 2.dp else 1.dp,
                            color = if (imageError) MaterialTheme.colorScheme.error
                            else MaterialTheme.colorScheme.outline,
                            shape = RoundedCornerShape(12.dp)
                        )
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .clickable(enabled = !isUploading) {
                            imagePicker.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                    contentAlignment = Alignment.Center
                ) {
                    if (selectedImageUri != null) {
                        AsyncImage(
                            model = selectedImageUri,
                            contentDescription = "Selected verse image",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxWidth()
                        )
                        if (isUploading) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(180.dp)
                                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.6f)),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator()
                            }
                        }
                    } else {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.AddPhotoAlternate,
                                contentDescription = null,
                                modifier = Modifier.size(40.dp),
                                tint = if (imageError) MaterialTheme.colorScheme.error
                                else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Tap to select an image",
                                style = MaterialTheme.typography.bodySmall,
                                color = if (imageError) MaterialTheme.colorScheme.error
                                else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                if (imageError) {
                    Text(
                        text = "Please select an image first",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 4.dp, start = 4.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = reference,
                    onValueChange = {
                        reference = it
                        referenceError = false
                    },
                    label = { Text("Reference (e.g. John 3:16)") },
                    isError = referenceError,
                    supportingText = if (referenceError) {
                        { Text("Reference cannot be empty") }
                    } else null,
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = verse,
                    onValueChange = {
                        verse = it
                        verseError = false
                    },
                    label = { Text("Verse text") },
                    isError = verseError,
                    supportingText = if (verseError) {
                        { Text("Verse text cannot be empty") }
                    } else null,
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3,
                    maxLines = 6
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    referenceError = reference.isBlank()
                    verseError = verse.isBlank()
                    imageError = uploadedImageUrl == null
                    if (!referenceError && !verseError && !imageError) {
                        mainViewModel.addVerse(
                            verse = verse,
                            reference = reference,
                            imageUrl = uploadedImageUrl!!,
                            currentUser = currentUser
                        )
                        onDismiss()
                    }
                },
                enabled = !isUploading
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(
                onClick = { if (!isUploading) onDismiss() }
            ) {
                Text("Cancel")
            }
        }
    )
}

// ---------------------------------------------------------------------------
//  Edit Verse Dialog
// ---------------------------------------------------------------------------

@Composable
fun EditVerseDialog(
    verse: BibleVerse,
    isUploading: Boolean,
    mainViewModel: MainViewModel,
    onDismiss: () -> Unit
) {
    var reference by remember { mutableStateOf(verse.reference) }
    var verseText by remember { mutableStateOf(verse.verse) }
    var currentImageUrl by remember { mutableStateOf(verse.imageUrl) }
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    var referenceError by remember { mutableStateOf(false) }
    var verseError by remember { mutableStateOf(false) }

    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current

    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        uri?.let {
            selectedImageUri = it
            coroutineScope.launch {
                val url = mainViewModel.uploadImage(it, context)
                if (url != null) {
                    currentImageUrl = url
                }
            }
        }
    }

    AlertDialog(
        onDismissRequest = { if (!isUploading) onDismiss() },
        title = { Text("Edit Verse", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                // Image preview with option to change
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .clickable(enabled = !isUploading) {
                            imagePicker.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                    contentAlignment = Alignment.Center
                ) {
                    AsyncImage(
                        model = selectedImageUri ?: currentImageUrl,
                        contentDescription = "Verse image",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (isUploading) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(180.dp)
                                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.6f)),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(180.dp)
                                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.3f)),
                            contentAlignment = Alignment.BottomCenter
                        ) {
                            Text(
                                text = "Tap to change image",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.padding(8.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = reference,
                    onValueChange = {
                        reference = it
                        referenceError = false
                    },
                    label = { Text("Reference") },
                    isError = referenceError,
                    supportingText = if (referenceError) {
                        { Text("Reference cannot be empty") }
                    } else null,
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = verseText,
                    onValueChange = {
                        verseText = it
                        verseError = false
                    },
                    label = { Text("Verse text") },
                    isError = verseError,
                    supportingText = if (verseError) {
                        { Text("Verse text cannot be empty") }
                    } else null,
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3,
                    maxLines = 6
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    referenceError = reference.isBlank()
                    verseError = verseText.isBlank()
                    if (!referenceError && !verseError) {
                        mainViewModel.updateVerse(
                            verseId = verse.id,
                            newVerse = verseText,
                            newReference = reference,
                            newImageUrl = currentImageUrl
                        )
                        onDismiss()
                    }
                },
                enabled = !isUploading
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = { if (!isUploading) onDismiss() }) {
                Text("Cancel")
            }
        }
    )
}

// ---------------------------------------------------------------------------
//  Delete Confirmation Dialog
// ---------------------------------------------------------------------------

@Composable
fun DeleteConfirmationDialog(
    verse: BibleVerse,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error
            )
        },
        title = { Text("Delete Verse?") },
        text = {
            Text(
                text = "\"${verse.reference}\" will be permanently deleted. This action cannot be undone."
            )
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error
                )
            ) {
                Text("Delete")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
