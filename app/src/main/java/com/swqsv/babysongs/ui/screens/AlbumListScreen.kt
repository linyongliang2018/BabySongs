package com.swqsv.babysongs.ui.screens

import android.Manifest
import android.content.Intent
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.swqsv.babysongs.R
import com.swqsv.babysongs.data.model.Album
import com.swqsv.babysongs.data.model.AlbumCategoryUi
import com.swqsv.babysongs.ui.viewmodel.AlbumsHomePhase
import com.swqsv.babysongs.ui.viewmodel.LibraryCategoryTab
import com.swqsv.babysongs.ui.components.MintGlassCategoryTile
import com.swqsv.babysongs.ui.components.MintGlassEmphasis
import com.swqsv.babysongs.ui.components.MintGlassSplitCard
import com.swqsv.babysongs.ui.viewmodel.LibraryViewModel
import com.swqsv.babysongs.ui.viewmodel.LibraryUiState
import com.swqsv.babysongs.ui.viewmodel.filteredAlbums
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun AlbumListScreen(
    viewModel: LibraryViewModel,
    permissionsGranted: Boolean,
    onOpenAlbum: (Album) -> Unit,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var showHelpDialog by remember { mutableStateOf(false) }
    var showManageCategoriesDialog by remember { mutableStateOf(false) }
    var categoryPickerAlbum by remember { mutableStateOf<Album?>(null) }
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

    if (showManageCategoriesDialog) {
        ManageCategoriesDialog(
            state = state,
            onDismiss = { showManageCategoriesDialog = false },
            onAdd = { viewModel.addCategory(it) },
            onRename = { id, name -> viewModel.renameCategory(id, name) },
            onDelete = { viewModel.deleteCategory(it) },
        )
    }

    categoryPickerAlbum?.let { album ->
        SetAlbumCategoryDialog(
            album = album,
            state = state,
            onDismiss = { categoryPickerAlbum = null },
            onConfirm = { categoryId ->
                viewModel.setAlbumCategory(album.id, categoryId)
                categoryPickerAlbum = null
            },
        )
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
                onManageCategories = {
                    scope.launch { drawerState.close() }
                    showManageCategoriesDialog = true
                },
            )
        },
    ) {
        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            topBar = {
                val backToCategoryLabel = stringResource(id = R.string.category_back_to_picker)
                val viewingAlbums = state.libraryRoots.isNotEmpty() &&
                    state.homePhase == AlbumsHomePhase.ViewingAlbums
                val categoryHome = state.libraryRoots.isNotEmpty() &&
                    state.homePhase == AlbumsHomePhase.PickingCategory
                TopAppBar(
                    title = {
                        Text(
                            text = if (viewingAlbums) {
                                stringResource(id = R.string.albums_title)
                            } else {
                                stringResource(id = R.string.category_home_title)
                            },
                        )
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = if (categoryHome) {
                            MaterialTheme.colorScheme.surfaceContainerLow
                        } else {
                            MaterialTheme.colorScheme.surface
                        },
                        scrolledContainerColor = if (categoryHome) {
                            MaterialTheme.colorScheme.surfaceContainerLow
                        } else {
                            MaterialTheme.colorScheme.surface
                        },
                    ),
                    navigationIcon = {
                        if (viewingAlbums) {
                            IconButton(
                                onClick = { viewModel.backToCategoryPicker() },
                                modifier = Modifier.semantics { contentDescription = backToCategoryLabel },
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = backToCategoryLabel,
                                )
                            }
                        } else {
                            IconButton(
                                onClick = { scope.launch { drawerState.open() } },
                                modifier = Modifier.semantics { contentDescription = openMenuLabel },
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Menu,
                                    contentDescription = openMenuLabel,
                                )
                            }
                        }
                    },
                    actions = {
                        if (viewingAlbums) {
                            IconButton(
                                onClick = { scope.launch { drawerState.open() } },
                                modifier = Modifier.semantics { contentDescription = openMenuLabel },
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Menu,
                                    contentDescription = openMenuLabel,
                                )
                            }
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
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
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
                        onCategoryTileClick = { viewModel.openAlbumListForCategory(it) },
                        onOpenAlbum = onOpenAlbum,
                        onAlbumLongPress = { categoryPickerAlbum = it },
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
    onManageCategories: () -> Unit,
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
            TextButton(onClick = onManageCategories) {
                Text(text = stringResource(id = R.string.category_manage))
            }
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
    onCategoryTileClick: (LibraryCategoryTab) -> Unit,
    onOpenAlbum: (Album) -> Unit,
    onAlbumLongPress: (Album) -> Unit,
    onOpenHelp: () -> Unit,
    onRescan: () -> Unit,
) {
    val filtered = state.filteredAlbums()
    when {
        state.libraryRoots.isEmpty() -> {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
            ) {
                item(key = "hint_no_roots") {
                    MainEmptyHintNoRootsCard()
                }
            }
        }
        state.homePhase == AlbumsHomePhase.PickingCategory -> {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .background(brush = categoryPageBackgroundBrush()),
            ) {
                CategoryGridSection(
                    modifier = Modifier.fillMaxSize(),
                    state = state,
                    onCategoryTileClick = onCategoryTileClick,
                )
            }
        }
        else -> {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                if (state.albums.isEmpty()) {
                    item(key = "empty_albums") {
                        EmptyAlbumsHint(
                            onOpenHelp = onOpenHelp,
                            onRescan = onRescan,
                        )
                    }
                }
                if (state.albums.isNotEmpty() && filtered.isEmpty()) {
                    item(key = "empty_filter") {
                        Text(
                            text = stringResource(id = R.string.category_empty_in_filter),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 24.dp),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                items(filtered, key = { it.id }) { album ->
                    AlbumCard(
                        album = album,
                        state = state,
                        onClick = { onOpenAlbum(album) },
                        onLongClick = { onAlbumLongPress(album) },
                    )
                }
            }
        }
    }
}

private data class CategoryTileModel(
    val tab: LibraryCategoryTab,
    val title: String,
    val albumCount: Int,
)

private fun CategoryTileModel.stableKey(): String = when (val t = tab) {
    LibraryCategoryTab.All -> "all"
    LibraryCategoryTab.Uncategorized -> "unc"
    is LibraryCategoryTab.Custom -> "c_${t.id}"
}

@Composable
private fun categoryPageBackgroundBrush(): Brush {
    val s = MaterialTheme.colorScheme
    return Brush.verticalGradient(
        colors = listOf(
            s.primaryContainer.copy(alpha = 0.85f),
            s.background,
            s.primary.copy(alpha = 0.04f),
        ),
    )
}

@Composable
private fun CategoryGridSection(
    modifier: Modifier = Modifier,
    state: LibraryUiState,
    onCategoryTileClick: (LibraryCategoryTab) -> Unit,
) {
    val labelAll = stringResource(id = R.string.category_tab_all)
    val labelUncategorized = stringResource(id = R.string.category_tab_uncategorized)
    val albums = state.albums
    val assigned = state.albumToCategoryId
    val tiles = remember(state.albums, state.albumToCategoryId, state.categories, labelAll, labelUncategorized) {
        buildList {
            add(
                CategoryTileModel(
                    tab = LibraryCategoryTab.All,
                    title = labelAll,
                    albumCount = albums.size,
                ),
            )
            add(
                CategoryTileModel(
                    tab = LibraryCategoryTab.Uncategorized,
                    title = labelUncategorized,
                    albumCount = albums.count { assigned[it.id] == null },
                ),
            )
            state.categories.forEach { c ->
                add(
                    CategoryTileModel(
                        tab = LibraryCategoryTab.Custom(c.id),
                        title = c.name,
                        albumCount = albums.count { assigned[it.id] == c.id },
                    ),
                )
            }
        }
    }
    val selected = state.selectedCategoryTab
    val showSel = state.showGridSelectionHighlight
    val gutter = 5.dp
    val edge = 6.dp
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = modifier,
        contentPadding = PaddingValues(horizontal = edge, vertical = edge),
        horizontalArrangement = Arrangement.spacedBy(gutter),
        verticalArrangement = Arrangement.spacedBy(gutter),
    ) {
        items(
            items = tiles,
            key = { it.stableKey() },
        ) { tile ->
            MintGlassCategoryTile(
                modifier = Modifier.aspectRatio(1f),
                selected = showSel && selected == tile.tab,
                onClick = { onCategoryTileClick(tile.tab) },
            ) {
                Text(
                    text = tile.title,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    maxLines = 2,
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = stringResource(id = R.string.category_album_count_format, tile.albumCount),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.78f),
                )
            }
        }
    }
}

@Composable
private fun MainEmptyHintNoRootsCard() {
    MintGlassSplitCard(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = stringResource(id = R.string.main_empty_hint_no_roots),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Composable
private fun EmptyAlbumsHint(
    onOpenHelp: () -> Unit,
    onRescan: () -> Unit,
) {
    MintGlassSplitCard(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = stringResource(id = R.string.empty_albums_title),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
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

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun AlbumCard(
    album: Album,
    state: LibraryUiState,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
) {
    val showCategoryLine = state.selectedCategoryTab is LibraryCategoryTab.All
    val categoryLine = categoryLineText(album = album, state = state)
    val scheme = MaterialTheme.colorScheme
    MintGlassSplitCard(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick,
            ),
        emphasis = MintGlassEmphasis.Default,
    ) {
        Text(
            text = album.name,
            style = MaterialTheme.typography.titleMedium,
            color = scheme.onSurface,
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = stringResource(id = R.string.song_count_format, album.songCount),
            style = MaterialTheme.typography.bodyMedium,
            color = scheme.onSurfaceVariant.copy(alpha = 0.9f),
        )
        if (showCategoryLine) {
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = categoryLine,
                style = MaterialTheme.typography.labelMedium,
                color = scheme.primary.copy(alpha = 0.88f),
            )
        }
    }
}

@Composable
private fun categoryLineText(album: Album, state: LibraryUiState): String {
    val id = state.albumToCategoryId[album.id]
    val label = if (id == null) {
        stringResource(id = R.string.category_badge_uncategorized)
    } else {
        state.categories.firstOrNull { it.id == id }?.name
            ?: stringResource(id = R.string.category_badge_uncategorized)
    }
    return stringResource(id = R.string.category_album_line_format, label)
}

@Composable
private fun ManageCategoriesDialog(
    state: LibraryUiState,
    onDismiss: () -> Unit,
    onAdd: (String) -> Unit,
    onRename: (String, String) -> Unit,
    onDelete: (String) -> Unit,
) {
    var showAdd by remember { mutableStateOf(false) }
    var renameTarget by remember { mutableStateOf<AlbumCategoryUi?>(null) }
    var deleteTarget by remember { mutableStateOf<AlbumCategoryUi?>(null) }

    when {
        showAdd -> {
            CategoryNameInputDialog(
                title = stringResource(id = R.string.category_add),
                initial = "",
                onDismiss = { showAdd = false },
                onConfirm = { name ->
                    onAdd(name)
                    showAdd = false
                },
            )
        }
        renameTarget != null -> {
            val cat = renameTarget!!
            CategoryNameInputDialog(
                title = stringResource(id = R.string.category_rename),
                initial = cat.name,
                onDismiss = { renameTarget = null },
                onConfirm = { name ->
                    onRename(cat.id, name)
                    renameTarget = null
                },
            )
        }
        deleteTarget != null -> {
            val cat = deleteTarget!!
            AlertDialog(
                onDismissRequest = { deleteTarget = null },
                title = { Text(text = stringResource(id = R.string.category_delete)) },
                text = {
                    Text(
                        text = stringResource(id = R.string.category_delete_confirm_message, cat.name),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            onDelete(cat.id)
                            deleteTarget = null
                        },
                    ) {
                        Text(text = stringResource(id = R.string.confirm))
                    }
                },
                dismissButton = {
                    TextButton(onClick = { deleteTarget = null }) {
                        Text(text = stringResource(id = R.string.cancel))
                    }
                },
            )
        }
        else -> {
            AlertDialog(
                onDismissRequest = onDismiss,
                title = { Text(text = stringResource(id = R.string.category_manage_title)) },
                text = {
                    Column(
                        modifier = Modifier.verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        if (state.categories.isEmpty()) {
                            Text(
                                text = stringResource(id = R.string.category_manage_empty_hint),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        state.categories.forEach { c ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    text = c.name,
                                    style = MaterialTheme.typography.bodyLarge,
                                    modifier = Modifier.weight(1f),
                                )
                                TextButton(onClick = { renameTarget = c }) {
                                    Text(text = stringResource(id = R.string.category_rename))
                                }
                                TextButton(onClick = { deleteTarget = c }) {
                                    Text(text = stringResource(id = R.string.category_delete))
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showAdd = true }) {
                        Text(text = stringResource(id = R.string.category_add))
                    }
                },
                dismissButton = {
                    TextButton(onClick = onDismiss) {
                        Text(text = stringResource(id = R.string.category_manage_done))
                    }
                },
            )
        }
    }
}

@Composable
private fun CategoryNameInputDialog(
    title: String,
    initial: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    var text by remember(initial) { mutableStateOf(initial) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = title) },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                singleLine = true,
                label = { Text(text = stringResource(id = R.string.category_name_hint)) },
                modifier = Modifier.fillMaxWidth(),
            )
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(text) },
                enabled = text.trim().isNotEmpty(),
            ) {
                Text(text = stringResource(id = R.string.confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(id = R.string.cancel))
            }
        },
    )
}

@Composable
private fun SetAlbumCategoryDialog(
    album: Album,
    state: LibraryUiState,
    onDismiss: () -> Unit,
    onConfirm: (String?) -> Unit,
) {
    var selectedId by remember(album.id) {
        mutableStateOf<String?>(state.albumToCategoryId[album.id])
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(text = stringResource(id = R.string.category_set_for_album))
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(0.dp)) {
                Text(
                    text = album.name,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    RadioButton(
                        selected = selectedId == null,
                        onClick = { selectedId = null },
                    )
                    Text(
                        text = stringResource(id = R.string.category_uncategorized_option),
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(start = 8.dp),
                    )
                }
                state.categories.forEach { c ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(
                            selected = selectedId == c.id,
                            onClick = { selectedId = c.id },
                        )
                        Text(
                            text = c.name,
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.padding(start = 8.dp),
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(selectedId) }) {
                Text(text = stringResource(id = R.string.confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(id = R.string.cancel))
            }
        },
    )
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
