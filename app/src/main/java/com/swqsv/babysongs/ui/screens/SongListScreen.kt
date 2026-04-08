package com.swqsv.babysongs.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.swqsv.babysongs.ui.components.MintGlassEmphasis
import com.swqsv.babysongs.ui.components.MintGlassSplitCard
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
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
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
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    items(songs, key = { it.id }) { song ->
                        val cur = playbackState.currentSong
                        val sameAlbum =
                            playbackState.currentAlbum?.folderPath == album.folderPath ||
                                playbackState.currentAlbum?.id == album.id
                        val isCurrent = cur != null && sameAlbum &&
                            (cur.id == song.id || cur.filePath == song.filePath)
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
    val scheme = MaterialTheme.colorScheme
    MintGlassSplitCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        emphasis = if (isCurrent) MintGlassEmphasis.Playing else MintGlassEmphasis.Default,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (isCurrent) {
                Box(
                    modifier = Modifier
                        .width(4.dp)
                        .height(46.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(scheme.primary),
                )
                Spacer(modifier = Modifier.width(12.dp))
            }
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = song.title,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = if (isCurrent) FontWeight.SemiBold else FontWeight.Normal,
                        ),
                        color = if (isCurrent) scheme.onPrimaryContainer else scheme.onSurface,
                        modifier = Modifier.weight(1f),
                    )
                    if (isCurrent) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = scheme.primary.copy(alpha = 0.22f),
                        ) {
                            Text(
                                text = stringResource(id = R.string.song_now_playing_badge),
                                style = MaterialTheme.typography.labelSmall,
                                color = scheme.primary,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            )
                        }
                    }
                }
                val dur = song.durationMs
                if (dur > 0L) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = formatDurationMs(dur),
                        style = MaterialTheme.typography.bodySmall,
                        color = if (isCurrent) {
                            scheme.onPrimaryContainer.copy(alpha = 0.85f)
                        } else {
                            scheme.onSurfaceVariant.copy(alpha = 0.92f)
                        },
                    )
                }
            }
        }
    }
}
