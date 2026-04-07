package com.swqsv.babysongs.playback

import com.swqsv.babysongs.data.model.Album
import com.swqsv.babysongs.data.model.PlayMode
import com.swqsv.babysongs.data.model.Song

/**
 * UI 与 MediaSession 共用的播放快照（StateFlow）。
 */
data class PlaybackUiState(
    val currentAlbum: Album?,
    val currentSong: Song?,
    val isPlaying: Boolean,
    val positionMs: Long,
    val durationMs: Long,
    val playMode: PlayMode,
    /** 当前模式下用于展示的队列（随机模式为洗牌后顺序）。 */
    val queue: List<Song>,
)
