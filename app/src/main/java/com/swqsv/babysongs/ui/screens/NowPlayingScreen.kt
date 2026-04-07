package com.swqsv.babysongs.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.swqsv.babysongs.R
import com.swqsv.babysongs.data.model.PlayMode
import com.swqsv.babysongs.ui.formatDurationMs
import com.swqsv.babysongs.ui.viewmodel.PlayerViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NowPlayingScreen(
    playerViewModel: PlayerViewModel,
    onBack: () -> Unit,
) {
    val state by playerViewModel.playbackState.collectAsStateWithLifecycle()

    DisposableEffect(Unit) {
        onDispose {
            playerViewModel.persistProgress()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = stringResource(id = R.string.now_playing_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(id = R.string.cd_back),
                        )
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                text = state.currentSong?.title ?: "—",
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.fillMaxWidth(),
            )
            Text(
                text = state.currentAlbum?.name ?: "—",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(modifier = Modifier.height(8.dp))

            val duration = state.durationMs.coerceAtLeast(0L)
            val safeDuration = if (duration > 0L) duration else 1L
            val progress = (state.positionMs.toFloat() / safeDuration.toFloat()).coerceIn(0f, 1f)

            Slider(
                value = progress,
                onValueChange = { v ->
                    val target = (v * safeDuration).toLong()
                    playerViewModel.seekTo(target)
                },
                modifier = Modifier.fillMaxWidth(),
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(text = formatDurationMs(state.positionMs))
                Text(text = formatDurationMs(duration))
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    val next = cycleMode(state.playMode)
                    playerViewModel.setPlayMode(next)
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(text = playModeLabel(state.playMode))
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(
                    onClick = { playerViewModel.skipToPrevious() },
                    modifier = Modifier.size(52.dp),
                ) {
                    Icon(
                        imageVector = Icons.Filled.SkipPrevious,
                        contentDescription = stringResource(id = R.string.cd_skip_previous),
                        modifier = Modifier.size(32.dp),
                    )
                }
                FilledIconButton(
                    onClick = { playerViewModel.playPause() },
                    modifier = Modifier.size(64.dp),
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    ),
                ) {
                    Icon(
                        imageVector = if (state.isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                        contentDescription = stringResource(id = R.string.cd_play_pause),
                        modifier = Modifier.size(36.dp),
                    )
                }
                IconButton(
                    onClick = { playerViewModel.skipToNext() },
                    modifier = Modifier.size(52.dp),
                ) {
                    Icon(
                        imageVector = Icons.Filled.SkipNext,
                        contentDescription = stringResource(id = R.string.cd_skip_next),
                        modifier = Modifier.size(32.dp),
                    )
                }
            }
        }
    }
}

private fun cycleMode(current: PlayMode): PlayMode {
    return when (current) {
        PlayMode.ORDER -> PlayMode.LIST_LOOP
        PlayMode.LIST_LOOP -> PlayMode.SINGLE_LOOP
        PlayMode.SINGLE_LOOP -> PlayMode.SHUFFLE
        PlayMode.SHUFFLE -> PlayMode.ORDER
    }
}

@Composable
private fun playModeLabel(mode: PlayMode): String {
    val id = when (mode) {
        PlayMode.ORDER -> R.string.mode_order
        PlayMode.LIST_LOOP -> R.string.mode_list_loop
        PlayMode.SINGLE_LOOP -> R.string.mode_single_loop
        PlayMode.SHUFFLE -> R.string.mode_shuffle
    }
    return stringResource(id = id)
}
