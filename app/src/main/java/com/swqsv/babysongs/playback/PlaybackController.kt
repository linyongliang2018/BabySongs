package com.swqsv.babysongs.playback

import android.app.Application
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import com.swqsv.babysongs.data.model.Album
import com.swqsv.babysongs.data.model.PlayMode
import com.swqsv.babysongs.data.model.Song
import com.swqsv.babysongs.data.prefs.PlaybackPreferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File
import kotlin.random.Random

/**
 * 持有应用级 [ExoPlayer]，与 [BabyPlaybackService] 中的 [MediaSession] 配合。
 * 随机模式使用洗牌队列 + 指针；单曲循环在自然结束时重复当前曲。
 */
class PlaybackController(
    private val application: Application,
    private val preferences: PlaybackPreferences,
) {

    private companion object {
        const val TAG: String = "PlaybackController"
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var positionTicker: Job? = null

    private var orderedSongs: List<Song> = emptyList()
    private var currentAlbum: Album? = null
    private var playMode: PlayMode = PlayMode.ORDER
    /** 非随机模式下当前曲目在专辑列表中的索引。 */
    private var playIndex: Int = 0
    /** 随机模式：对 orderedSongs 下标的洗牌序列。 */
    private var shuffleOrder: List<Int> = emptyList()
    private var shufflePtr: Int = 0

    private val _state = MutableStateFlow(
        PlaybackUiState(
            currentAlbum = null,
            currentSong = null,
            isPlaying = false,
            positionMs = 0L,
            durationMs = 0L,
            playMode = PlayMode.ORDER,
            queue = emptyList(),
        ),
    )
    val state: StateFlow<PlaybackUiState> = _state.asStateFlow()

    private val playerListener = object : Player.Listener {
        override fun onPlaybackStateChanged(playbackState: Int) {
            if (playbackState == Player.STATE_READY) {
                val d = player.duration
                if (d > 0) {
                    _state.update { it.copy(durationMs = d) }
                }
            }
            if (playbackState == Player.STATE_ENDED) {
                onPlaybackNaturallyEnded()
            }
        }

        override fun onIsPlayingChanged(isPlaying: Boolean) {
            _state.update { it.copy(isPlaying = isPlaying) }
            if (isPlaying) {
                startPositionTicker()
            } else {
                stopPositionTicker()
                // ExoPlayer 必须在主线程访问；先读取 position 再在 IO 写入 DataStore
                val song = logicalCurrentSong()
                if (song != null) {
                    val pos = player.currentPosition.coerceAtLeast(0L)
                    scope.launch(Dispatchers.IO) {
                        preferences.save(
                            albumFolderPath = currentAlbum?.folderPath,
                            songFilePath = song.filePath,
                            positionMs = pos,
                            playMode = playMode,
                        )
                    }
                }
            }
        }

        override fun onPlayerError(error: PlaybackException) {
            scope.launch(Dispatchers.Main.immediate) {
                skipToNextAfterError()
            }
        }
    }

    val player: ExoPlayer = ExoPlayer.Builder(application).build().apply {
        setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(C.USAGE_MEDIA)
                .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                .build(),
            true,
        )
        setHandleAudioBecomingNoisy(true)
        addListener(playerListener)
    }

    var mediaSession: MediaSession? = null

    fun playAlbumFromSong(
        context: Context,
        album: Album,
        songs: List<Song>,
        startSong: Song,
        mode: PlayMode,
    ) {
        ensureForegroundService(context)
        orderedSongs = songs
        currentAlbum = album
        playMode = mode
        val startIndex = orderedSongs.indexOfFirst { it.id == startSong.id }.coerceAtLeast(0)
        when (mode) {
            PlayMode.SHUFFLE -> {
                shuffleOrder = shuffleIndices(orderedSongs.size)
                shufflePtr = shuffleOrder.indexOf(startIndex).let { idx ->
                    if (idx >= 0) idx else 0
                }
                playIndex = startIndex
            }
            else -> {
                playIndex = startIndex
            }
        }
        emitFullState()
        loadCurrentSongFromLogicalState(shouldPlay = true)
    }

    fun setPlayMode(mode: PlayMode) {
        if (mode == playMode) return
        val current = logicalCurrentSong() ?: run {
            playMode = mode
            emitFullState()
            return
        }
        when {
            mode == PlayMode.SHUFFLE && playMode != PlayMode.SHUFFLE -> {
                val idx = orderedSongs.indexOfFirst { it.id == current.id }.coerceAtLeast(0)
                shuffleOrder = shuffleIndices(orderedSongs.size)
                shufflePtr = shuffleOrder.indexOf(idx).let { p -> if (p >= 0) p else 0 }
                playIndex = idx
            }
            playMode == PlayMode.SHUFFLE && mode != PlayMode.SHUFFLE -> {
                playIndex = orderedSongs.indexOfFirst { it.id == current.id }.coerceAtLeast(0)
            }
            else -> Unit
        }
        playMode = mode
        emitFullState()
        val pos = player.currentPosition.coerceAtLeast(0L)
        scope.launch(Dispatchers.IO) {
            preferences.save(
                albumFolderPath = currentAlbum?.folderPath,
                songFilePath = current.id,
                positionMs = pos,
                playMode = playMode,
            )
        }
    }

    fun playPause() {
        if (player.isPlaying) {
            player.pause()
        } else {
            if (player.playbackState == Player.STATE_IDLE || player.playbackState == Player.STATE_ENDED) {
                loadCurrentSongFromLogicalState(shouldPlay = true)
            } else {
                player.play()
            }
        }
    }

    fun seekTo(positionMs: Long) {
        player.seekTo(positionMs)
        _state.update { it.copy(positionMs = positionMs) }
    }

    fun skipToNext(context: Context) {
        ensureForegroundService(context)
        when (playMode) {
            PlayMode.ORDER, PlayMode.SINGLE_LOOP -> {
                if (orderedSongs.isEmpty()) return
                if (playIndex >= orderedSongs.lastIndex) {
                    player.pause()
                    return
                }
                playIndex++
                emitFullState()
                loadCurrentSongFromLogicalState(shouldPlay = true)
            }
            PlayMode.LIST_LOOP -> {
                if (orderedSongs.isEmpty()) return
                playIndex = (playIndex + 1) % orderedSongs.size
                emitFullState()
                loadCurrentSongFromLogicalState(shouldPlay = true)
            }
            PlayMode.SHUFFLE -> {
                advanceShuffleForManualNext()
                emitFullState()
                loadCurrentSongFromLogicalState(shouldPlay = true)
            }
        }
    }

    fun skipToPrevious(context: Context) {
        ensureForegroundService(context)
        when (playMode) {
            PlayMode.ORDER, PlayMode.SINGLE_LOOP -> {
                if (orderedSongs.isEmpty()) return
                if (playIndex <= 0) {
                    player.pause()
                    return
                }
                playIndex--
                emitFullState()
                loadCurrentSongFromLogicalState(shouldPlay = true)
            }
            PlayMode.LIST_LOOP -> {
                if (orderedSongs.isEmpty()) return
                playIndex = if (playIndex <= 0) orderedSongs.lastIndex else playIndex - 1
                emitFullState()
                loadCurrentSongFromLogicalState(shouldPlay = true)
            }
            PlayMode.SHUFFLE -> {
                if (shuffleOrder.isEmpty()) return
                shufflePtr = if (shufflePtr <= 0) shuffleOrder.lastIndex else shufflePtr - 1
                emitFullState()
                loadCurrentSongFromLogicalState(shouldPlay = true)
            }
        }
    }

    /**
     * 从持久化恢复模式与曲目；若文件仍存在则定位到 [positionMs]。
     */
    fun restoreFromLibrary(
        album: Album,
        songs: List<Song>,
        song: Song,
        positionMs: Long,
        mode: PlayMode,
    ) {
        orderedSongs = songs
        currentAlbum = album
        playMode = mode
        val idx = orderedSongs.indexOfFirst { it.id == song.id }.coerceAtLeast(0)
        when (mode) {
            PlayMode.SHUFFLE -> {
                shuffleOrder = shuffleIndices(orderedSongs.size)
                shufflePtr = shuffleOrder.indexOf(idx).let { p -> if (p >= 0) p else 0 }
                playIndex = idx
            }
            else -> {
                playIndex = idx
            }
        }
        emitFullState()
        loadCurrentSongFromLogicalState(
            shouldPlay = false,
            resumePositionMs = positionMs.coerceAtLeast(0L),
        )
    }

    fun releaseMediaSessionOnly() {
        mediaSession?.release()
        mediaSession = null
    }

    private fun onPlaybackNaturallyEnded() {
        when (playMode) {
            PlayMode.SINGLE_LOOP -> {
                player.seekTo(0)
                player.prepare()
                player.play()
            }
            PlayMode.ORDER -> {
                if (orderedSongs.isEmpty()) return
                if (playIndex >= orderedSongs.lastIndex) {
                    player.pause()
                    return
                }
                playIndex++
                emitFullState()
                loadCurrentSongFromLogicalState(shouldPlay = true)
            }
            PlayMode.LIST_LOOP -> {
                if (orderedSongs.isEmpty()) return
                playIndex = (playIndex + 1) % orderedSongs.size
                emitFullState()
                loadCurrentSongFromLogicalState(shouldPlay = true)
            }
            PlayMode.SHUFFLE -> {
                if (shuffleOrder.isEmpty()) return
                if (shufflePtr >= shuffleOrder.lastIndex) {
                    shuffleOrder = shuffleIndices(orderedSongs.size)
                    shufflePtr = 0
                } else {
                    shufflePtr++
                }
                emitFullState()
                loadCurrentSongFromLogicalState(shouldPlay = true)
            }
        }
    }

    private fun skipToNextAfterError() {
        if (orderedSongs.isEmpty()) return
        when (playMode) {
            PlayMode.ORDER, PlayMode.SINGLE_LOOP -> {
                if (playIndex >= orderedSongs.lastIndex) {
                    player.pause()
                    return
                }
                playIndex++
            }
            PlayMode.LIST_LOOP -> {
                playIndex = (playIndex + 1) % orderedSongs.size
            }
            PlayMode.SHUFFLE -> {
                advanceShuffleForManualNext()
            }
        }
        emitFullState()
        loadCurrentSongFromLogicalState(shouldPlay = true)
    }

    private fun advanceShuffleForManualNext() {
        if (shuffleOrder.isEmpty()) return
        if (shufflePtr >= shuffleOrder.lastIndex) {
            shuffleOrder = shuffleIndices(orderedSongs.size)
            shufflePtr = 0
        } else {
            shufflePtr++
        }
    }

    private fun logicalCurrentSong(): Song? {
        if (orderedSongs.isEmpty()) return null
        return when (playMode) {
            PlayMode.SHUFFLE -> {
                val idx = shuffleOrder.getOrNull(shufflePtr) ?: return null
                orderedSongs.getOrNull(idx)
            }
            else -> orderedSongs.getOrNull(playIndex)
        }
    }

    private fun displayQueue(): List<Song> {
        if (orderedSongs.isEmpty()) return emptyList()
        return when (playMode) {
            PlayMode.SHUFFLE -> shuffleOrder.mapNotNull { orderedSongs.getOrNull(it) }
            else -> orderedSongs
        }
    }

    private fun emitFullState() {
        val song = logicalCurrentSong()
        _state.update {
            it.copy(
                currentAlbum = currentAlbum,
                currentSong = song,
                playMode = playMode,
                queue = displayQueue(),
                durationMs = song?.durationMs ?: it.durationMs,
            )
        }
    }

    private fun loadCurrentSongFromLogicalState(shouldPlay: Boolean, resumePositionMs: Long? = null) {
        val song = logicalCurrentSong() ?: return
        val uri = uriForSong(song)
        val item = MediaItem.fromUri(uri)
        player.setMediaItem(item)
        player.prepare()
        val pos = resumePositionMs?.coerceAtLeast(0L) ?: 0L
        if (pos > 0L) {
            player.seekTo(pos)
        }
        if (shouldPlay) {
            player.play()
        }
        _state.update {
            it.copy(
                currentSong = song,
                durationMs = song.durationMs,
                positionMs = pos,
            )
        }
        scope.launch(Dispatchers.IO) {
            preferences.save(
                albumFolderPath = currentAlbum?.folderPath,
                songFilePath = song.filePath,
                positionMs = pos,
                playMode = playMode,
            )
        }
    }

    private fun startPositionTicker() {
        stopPositionTicker()
        positionTicker = scope.launch {
            while (isActive) {
                delay(500)
                if (!player.isPlaying) continue
                val pos = player.currentPosition
                val dur = player.duration.takeIf { it > 0 } ?: _state.value.durationMs
                _state.update { it.copy(positionMs = pos, durationMs = dur) }
            }
        }
    }

    private fun stopPositionTicker() {
        positionTicker?.cancel()
        positionTicker = null
    }

    private fun ensureForegroundService(context: Context) {
        val appCtx = context.applicationContext
        val intent = Intent(appCtx, BabyPlaybackService::class.java)
        try {
            ContextCompat.startForegroundService(appCtx, intent)
            Log.i(TAG, "startForegroundService(BabyPlaybackService) ok")
        } catch (e: Exception) {
            Log.e(TAG, "startForegroundService failed: ${e.javaClass.simpleName} ${e.message}", e)
            throw e
        }
    }

    /** 支持系统文档树返回的 [content://] 与旧版文件绝对路径。 */
    private fun uriForSong(song: Song): Uri {
        return if (song.filePath.startsWith("content://")) {
            Uri.parse(song.filePath)
        } else {
            Uri.fromFile(File(song.filePath))
        }
    }

    private fun shuffleIndices(size: Int): List<Int> {
        if (size <= 0) return emptyList()
        val list = MutableList(size) { it }
        val rnd = Random(System.nanoTime())
        for (i in list.lastIndex downTo 1) {
            val j = rnd.nextInt(i + 1)
            val tmp = list[i]
            list[i] = list[j]
            list[j] = tmp
        }
        return list
    }
}
