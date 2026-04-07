package com.swqsv.babysongs.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.swqsv.babysongs.BabySongsApplication
import com.swqsv.babysongs.data.model.Album
import com.swqsv.babysongs.data.model.PlayMode
import com.swqsv.babysongs.data.model.Song
import com.swqsv.babysongs.playback.PlaybackUiState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class PlayerViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as BabySongsApplication

    val playbackState: StateFlow<PlaybackUiState> =
        app.playbackController.state.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = app.playbackController.state.value,
        )

    fun playPause() {
        app.playbackController.playPause()
    }

    fun seekTo(ms: Long) {
        app.playbackController.seekTo(ms)
    }

    fun skipToNext() {
        app.playbackController.skipToNext(getApplication())
    }

    fun skipToPrevious() {
        app.playbackController.skipToPrevious(getApplication())
    }

    fun setPlayMode(mode: PlayMode) {
        app.playbackController.setPlayMode(mode)
    }

    fun playAlbumFromSong(album: Album, songs: List<Song>, start: Song, mode: PlayMode) {
        app.playbackController.playAlbumFromSong(getApplication(), album, songs, start, mode)
    }

    fun persistProgress() {
        viewModelScope.launch {
            val c = app.playbackController
            val song = c.state.value.currentSong ?: return@launch
            val album = c.state.value.currentAlbum ?: return@launch
            val pos = c.player.currentPosition.coerceAtLeast(0L)
            val mode = c.state.value.playMode
            withContext(Dispatchers.IO) {
                app.playbackPreferences.save(
                    albumFolderPath = album.folderPath,
                    songFilePath = song.filePath,
                    positionMs = pos,
                    playMode = mode,
                )
            }
        }
    }
}
