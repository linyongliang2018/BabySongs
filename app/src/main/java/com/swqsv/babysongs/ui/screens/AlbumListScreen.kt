package com.swqsv.babysongs.ui.screens

import android.Manifest
import android.content.Intent
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.swqsv.babysongs.R
import com.swqsv.babysongs.data.model.Album
import com.swqsv.babysongs.ui.viewmodel.LibraryViewModel
import com.swqsv.babysongs.ui.viewmodel.LibraryUiState
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlbumListScreen(
    viewModel: LibraryViewModel,
    permissionsGranted: Boolean,
    onOpenAlbum: (Album) -> Unit,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var showHelpDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val openMenuLabel = stringResource(id = R.string.menu_open)

    val treeLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree(),
    ) { uri ->
        if (uri != null) {
            try {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION,
                )
            } catch (_: SecurityException) {
                // 部分机型或 ROM 可能无法持久化，仍尝试扫描本次 URI
            }
            viewModel.onLibraryRootPicked(uri)
            scope.launch { drawerState.close() }
        }
    }

    val launchPickFolder = { treeLauncher.launch(null) }

    LaunchedEffect(permissionsGranted) {
        if (permissionsGranted) {
            viewModel.refreshLibrary()
        }
    }

    if (showHelpDialog) {
        val notPicked = stringResource(id = R.string.root_not_picked)
        val summary = if (state.libraryRoots.isEmpty()) {
            notPicked
        } else {
            state.libraryRoots.joinToString("\n") { "• ${it.displayName}" }
        }
        AlertDialog(
            onDismissRequest = { showHelpDialog = false },
            title = { Text(text = stringResource(id = R.string.how_to_add_dialog_title)) },
            text = {
                Text(
                    text = stringResource(id = R.string.how_to_add_dialog_message, summary),
                    style = MaterialTheme.typography.bodyMedium,
                )
            },
            confirmButton = {
                TextButton(onClick = { showHelpDialog = false }) {
                    Text(text = stringResource(id = R.string.got_it))
                }
            },
        )
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            LibraryDrawerSheet(
                state = state,
                onAddDirectory = launchPickFolder,
                onRemoveRoot = { viewModel.removeLibraryRoot(it) },
                onRescan = { viewModel.refreshLibrary() },
                onHowToAdd = {
                    scope.launch { drawerState.close() }
                    showHelpDialog = true
                },
            )
        },
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(text = stringResource(id = R.string.albums_title)) },
                    navigationIcon = {
                        IconButton(
                            onClick = { scope.launch { drawerState.open() } },
                            modifier = Modifier.semantics { contentDescription = openMenuLabel },
                        ) {
                            Text(text = "☰", style = MaterialTheme.typography.headlineMedium)
                        }
                    },
                )
            },
        ) { padding ->
            when {
                !permissionsGranted -> {
                    Text(
                        text = stringResource(id = R.string.permission_rationale),
                        modifier = Modifier
                            .padding(padding)
                            .padding(24.dp),
                        style = MaterialTheme.typography.bodyLarge,
                    )
                }
                state.isLoading -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(padding),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        CircularProgressIndicator()
                    }
                }
                state.message != null -> {
                    Text(
                        text = state.message ?: "",
                        modifier = Modifier
                            .padding(padding)
                            .padding(24.dp),
                        style = MaterialTheme.typography.bodyLarge,
                    )
                }
                else -> {
                    AlbumListContent(
                        paddingValues = padding,
                        state = state,
                        onOpenAlbum = onOpenAlbum,
                        onOpenHelp = { showHelpDialog = true },
                        onRescan = { viewModel.refreshLibrary() },
                    )
                }
            }
        }
    }
}

@Composable
private fun LibraryDrawerSheet(
    state: LibraryUiState,
    onAddDirectory: () -> Unit,
    onRemoveRoot: (String) -> Unit,
    onRescan: () -> Unit,
    onHowToAdd: () -> Unit,
) {
    ModalDrawerSheet {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                text = stringResource(id = R.string.drawer_title),
                style = MaterialTheme.typography.titleLarge,
            )
            Text(
                text = stringResource(id = R.string.pick_library_folder_body),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Button(
                onClick = onAddDirectory,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(text = stringResource(id = R.string.pick_library_folder_primary))
            }
            if (state.libraryRoots.isNotEmpty()) {
                Text(
                    text = stringResource(id = R.string.roots_summary_label, state.libraryRoots.size),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                state.libraryRoots.forEach { root ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = root.displayName,
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier
                                .weight(1f)
                                .padding(end = 8.dp),
                        )
                        TextButton(onClick = { onRemoveRoot(root.treeUriString) }) {
                            Text(text = stringResource(id = R.string.remove_root))
                        }
                    }
                }
            }
            HorizontalDivider()
            TextButton(onClick = onRescan) {
                Text(text = stringResource(id = R.string.rescan))
            }
            TextButton(onClick = onHowToAdd) {
                Text(text = stringResource(id = R.string.how_to_add_action))
            }
        }
    }
}

@Composable
private fun AlbumListContent(
    paddingValues: PaddingValues,
    state: LibraryUiState,
    onOpenAlbum: (Album) -> Unit,
    onOpenHelp: () -> Unit,
    onRescan: () -> Unit,
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (state.libraryRoots.isEmpty()) {
            item(key = "hint_no_roots") {
                MainEmptyHintNoRootsCard()
            }
        }
        if (state.libraryRoots.isNotEmpty() && state.albums.isEmpty()) {
            item(key = "empty_albums") {
                EmptyAlbumsHint(
                    onOpenHelp = onOpenHelp,
                    onRescan = onRescan,
                )
            }
        }
        items(state.albums, key = { it.id }) { album ->
            AlbumRow(
                album = album,
                onClick = { onOpenAlbum(album) },
            )
        }
    }
}

@Composable
private fun MainEmptyHintNoRootsCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f),
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Text(
            text = stringResource(id = R.string.main_empty_hint_no_roots),
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(20.dp),
        )
    }
}

@Composable
private fun EmptyAlbumsHint(
    onOpenHelp: () -> Unit,
    onRescan: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.35f),
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                text = stringResource(id = R.string.empty_albums_title),
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                text = stringResource(id = R.string.empty_albums_hint),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            TextButton(onClick = onOpenHelp) {
                Text(text = stringResource(id = R.string.how_to_add_dialog_title))
            }
            TextButton(onClick = onRescan) {
                Text(text = stringResource(id = R.string.rescan))
            }
        }
    }
}

@Composable
private fun AlbumRow(
    album: Album,
    onClick: () -> Unit,
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = album.name,
                style = MaterialTheme.typography.titleLarge,
            )
            Text(
                text = stringResource(id = R.string.song_count_format, album.songCount),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

fun storagePermissionList(): Array<String> {
    return if (Build.VERSION.SDK_INT >= 33) {
        arrayOf(
            Manifest.permission.READ_MEDIA_AUDIO,
            Manifest.permission.READ_MEDIA_VIDEO,
            Manifest.permission.POST_NOTIFICATIONS,
        )
    } else {
        arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
    }
}
