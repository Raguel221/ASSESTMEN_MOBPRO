package com.raguel0087.myapplication1.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.raguel0087.myapplication1.R
import com.raguel0087.myapplication1.model.BibleVerse
import com.raguel0087.myapplication1.viewmodel.AuthViewModel
import com.raguel0087.myapplication1.viewmodel.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    mainViewModel: MainViewModel,
    authViewModel: AuthViewModel,
    onLogout: () -> Unit
) {
    val uiState by mainViewModel.uiState.collectAsStateWithLifecycle()
    val authState by authViewModel.uiState.collectAsStateWithLifecycle()
    val currentUser = authState.currentUser

    val snackbarHostState = remember { SnackbarHostState() }
    var showAddDialog by remember { mutableStateOf(false) }
    var showProfile by remember { mutableStateOf(false) }
    var verseToEdit by remember { mutableStateOf<BibleVerse?>(null) }
    var verseToDelete by remember { mutableStateOf<BibleVerse?>(null) }
    
    // State untuk toggle tampilan
    var showList by remember { mutableStateOf(true) }

    LaunchedEffect(uiState.snackbarMessage) {
        uiState.snackbarMessage?.let { message ->
            snackbarHostState.showSnackbar(message)
            mainViewModel.clearSnackbar()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Bible Verses", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                ),
                actions = {
                    // Tombol Toggle List/Grid
                    IconButton(onClick = { showList = !showList }) {
                        Icon(
                            painter = painterResource(
                                if (showList) R.drawable.baseline_grid_view_24
                                else R.drawable.baseline_view_list_24
                            ),
                            contentDescription = stringResource(
                                if (showList) R.string.tampilan_grid
                                else R.string.tampilan_list
                            ),
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }

                    IconButton(onClick = { mainViewModel.refresh() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                    }
                    IconButton(onClick = { showProfile = true }) {
                        if (!currentUser?.photoUrl.isNullOrBlank()) {
                            AsyncImage(
                                model = currentUser?.photoUrl,
                                contentDescription = "Profile",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.size(32.dp).clip(CircleShape)
                            )
                        } else {
                            Icon(Icons.Default.AccountCircle, contentDescription = "Profile")
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add verse", tint = MaterialTheme.colorScheme.onPrimary)
            }
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { paddingValues ->
        Column(
            modifier = Modifier.fillMaxSize().padding(paddingValues)
        ) {
            OutlinedTextField(
                value = uiState.searchQuery,
                onValueChange = mainViewModel::onSearchQueryChange,
                placeholder = { Text("Search by reference or verse…") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                shape = RoundedCornerShape(24.dp),
                singleLine = true
            )

            Box(modifier = Modifier.fillMaxSize()) {
                when {
                    uiState.isLoading -> CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                    uiState.error != null -> Text(uiState.error!!, modifier = Modifier.align(Alignment.Center))
                    uiState.filteredVerses.isEmpty() -> Text("No verses found", modifier = Modifier.align(Alignment.Center))
                    else -> {
                        if (showList) {
                            LazyColumn(
                                contentPadding = PaddingValues(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxSize()
                            ) {
                                if (uiState.isUploading) {
                                    item { LinearProgressIndicator(modifier = Modifier.fillMaxWidth()) }
                                }
                                items(uiState.filteredVerses) { verse ->
                                    VerseListItem(
                                        verse = verse,
                                        isOwner = verse.ownerId == currentUser?.uid,
                                        onEdit = { verseToEdit = verse },
                                        onDelete = { verseToDelete = verse }
                                    )
                                }
                            }
                        } else {
                            LazyVerticalGrid(
                                columns = GridCells.Fixed(2),
                                contentPadding = PaddingValues(8.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxSize()
                            ) {
                                if (uiState.isUploading) {
                                    item(span = { GridItemSpan(2) }) { LinearProgressIndicator(modifier = Modifier.fillMaxWidth()) }
                                }
                                items(uiState.filteredVerses) { verse ->
                                    VerseGridItem(
                                        verse = verse,
                                        isOwner = verse.ownerId == currentUser?.uid,
                                        onEdit = { verseToEdit = verse },
                                        onDelete = { verseToDelete = verse }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Dialogs
    if (showAddDialog && currentUser != null) {
        AddVerseDialog(
            isUploading = uiState.isUploading,
            currentUser = currentUser,
            mainViewModel = mainViewModel,
            onDismiss = { showAddDialog = false }
        )
    }
    verseToEdit?.let { verse ->
        EditVerseDialog(verse, uiState.isUploading, mainViewModel) { verseToEdit = null }
    }
    verseToDelete?.let { verse ->
        DeleteConfirmationDialog(verse, { mainViewModel.deleteVerse(verse.id); verseToDelete = null }) { verseToDelete = null }
    }
    if (showProfile && currentUser != null) {
        ProfileScreen(
            user = currentUser,
            verseCount = mainViewModel.verseCountForUser(currentUser.uid),
            onLogout = { authViewModel.signOut(); onLogout() },
            onDismiss = { showProfile = false }
        )
    }
}

@Composable
fun VerseListItem(verse: BibleVerse, isOwner: Boolean, onEdit: () -> Unit, onDelete: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable { },
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Row(modifier = Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
            if (verse.imageUrl != null) {
                AsyncImage(
                    model = verse.imageUrl,
                    contentDescription = null,
                    modifier = Modifier.size(80.dp).clip(RoundedCornerShape(8.dp)),
                    contentScale = ContentScale.Crop
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(verse.verse, style = MaterialTheme.typography.bodyMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text(verse.reference, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
            }
            if (isOwner) {
                Column {
                    IconButton(onClick = onEdit) { Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(20.dp)) }
                    IconButton(onClick = onDelete) { Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(20.dp)) }
                }
            }
        }
    }
}

@Composable
fun VerseGridItem(verse: BibleVerse, isOwner: Boolean, onEdit: () -> Unit, onDelete: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable { },
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Column {
            Box {
                if (verse.imageUrl != null) {
                    AsyncImage(
                        model = verse.imageUrl,
                        contentDescription = null,
                        modifier = Modifier.fillMaxWidth().height(120.dp),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Box(modifier = Modifier.fillMaxWidth().height(120.dp).background(Color.Gray.copy(alpha = 0.3f)), contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.Image, contentDescription = null)
                    }
                }
                if (isOwner) {
                    Row(modifier = Modifier.align(Alignment.TopEnd).padding(4.dp)) {
                        IconButton(onClick = onEdit, modifier = Modifier.size(24.dp).background(Color.White.copy(alpha = 0.6f), CircleShape)) {
                            Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                        IconButton(onClick = onDelete, modifier = Modifier.size(24.dp).background(Color.White.copy(alpha = 0.6f), CircleShape)) {
                            Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }
            Column(modifier = Modifier.padding(8.dp)) {
                Text(verse.verse, style = MaterialTheme.typography.bodySmall, maxLines = 3, overflow = TextOverflow.Ellipsis, fontStyle = FontStyle.Italic)
                Spacer(modifier = Modifier.height(4.dp))
                Text(verse.reference, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                Text("— ${verse.ownerName}", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
            }
        }
    }
}
