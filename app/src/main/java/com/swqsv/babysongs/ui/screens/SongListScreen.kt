package com.swqsv.babysongs.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.swqsv.babysongs.R
import com.swqsv.babysongs.data.model.Album
import com.swqsv.babysongs.data.model.Song
import com.swqsv.babysongs.ui.formatDurationMs
import com.swqsv.babysongs.ui.viewmodel.LibraryViewModel
import com.swqsv.babysongs.ui.viewmodel.PlayerViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.yield

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SongListScreen(
    album: Album,
    libraryViewModel: LibraryViewModel,
    playerViewModel: PlayerViewModel,
    onBack: () -> Unit,
    /** 可选：进入「正在播放」全屏（列表仍为默认主界面）。 */
    onOpenFullPlayer: () -> Unit,
) {
    val songs = remember(album.id) { mutableStateListOf<Song>() }
    var loading by remember(album.id) { mutableStateOf(true) }

    val playbackState by playerViewModel.playbackState.collectAsStateWithLifecycle()

    LaunchedEffect(album.id) {
        loading = true
        val cached = libraryViewModel.getCachedSongsOrNull(album)
        if (!cached.isNullOrEmpty()) {
            songs.clear()
            songs.addAll(cached)
            loading = false
        }
        val full = libraryViewModel.scanSongsAndPersist(album)
        songs.clear()
        songs.addAll(full)
        loading = false
        val chunkSize = 16
        val pauseMs = 48L
        if (full.isNotEmpty()) {
            full.chunked(chunkSize).forEachIndexed { chunkIndex, chunk ->
                yield()
                if (chunkIndex > 0) delay(pauseMs)
                val start = chunkIndex * chunkSize
                val enriched = libraryViewModel.enrichSongChunkParallel(chunk)
                enriched.forEachIndexed { i, s ->
                    val idx = start + i
                    if (idx < songs.size) songs[idx] = s
                }
            }
        }
        libraryViewModel.persistSongsCache(album, songs.toList())
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text(text = album.name) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                ),
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(id = R.string.cd_back),
                        )
                    }
                },
                actions = {
                    TextButton(onClick = onOpenFullPlayer) {
                        Text(text = stringResource(id = R.string.open_full_player))
                    }
                },
            )
        },
    ) { padding ->
        when {
            loading -> {
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
            else -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(songs, key = { it.id }) { song ->
                        val isCurrent =
                            playbackState.currentSong?.id == song.id &&
                                playbackState.currentAlbum?.id == album.id
                        SongRow(
                            song = song,
                            isCurrent = isCurrent,
                            onClick = {
                                val mode = playerViewModel.playbackState.value.playMode
                                playerViewModel.playAlbumFromSong(album, songs.toList(), song, mode)
                            },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SongRow(
    song: Song,
    isCurrent: Boolean,
    onClick: () -> Unit,
) {
    val border = if (isCurrent) {
        BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.45f))
    } else {
        BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    }
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(
            containerColor = if (isCurrent) {
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.42f)
            } else {
                MaterialTheme.colorScheme.surface
            },
        ),
        border = border,
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {
            Text(
                text = song.title,
                style = MaterialTheme.typography.titleMedium,
                color = if (isCurrent) {
                    MaterialTheme.colorScheme.onPrimaryContainer
                } else {
                    MaterialTheme.colorScheme.onSurface
                },
            )
            val dur = song.durationMs
            if (dur > 0L) {
                Text(
                    text = formatDurationMs(dur),
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isCurrent) {
                        MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f)
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                )
            }
        }
    }
}
