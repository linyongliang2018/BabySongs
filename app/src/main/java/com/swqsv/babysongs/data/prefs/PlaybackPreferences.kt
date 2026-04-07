package com.swqsv.babysongs.data.prefs

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.swqsv.babysongs.data.model.PlayMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.playbackDataStore: DataStore<Preferences> by preferencesDataStore(name = "playback")

/**
 * 轻量持久化：上次专辑路径、歌曲路径、进度、播放模式。
 */
class PlaybackPreferences(private val context: Context) {

    private object Keys {
        val albumFolderPath = stringPreferencesKey("album_folder_path")
        val songFilePath = stringPreferencesKey("song_file_path")
        val positionMs = longPreferencesKey("position_ms")
        val playModeOrdinal = longPreferencesKey("play_mode_ordinal")
    }

    val snapshot: Flow<PlaybackSnapshot> = context.playbackDataStore.data.map { prefs ->
        PlaybackSnapshot(
            albumFolderPath = prefs[Keys.albumFolderPath],
            songFilePath = prefs[Keys.songFilePath],
            positionMs = prefs[Keys.positionMs] ?: 0L,
            playMode = prefs[Keys.playModeOrdinal]?.toInt()?.let { o ->
                PlayMode.entries.getOrNull(o)
            } ?: PlayMode.ORDER,
        )
    }

    suspend fun save(
        albumFolderPath: String?,
        songFilePath: String?,
        positionMs: Long,
        playMode: PlayMode,
    ) {
        context.playbackDataStore.edit { prefs ->
            if (albumFolderPath != null) {
                prefs[Keys.albumFolderPath] = albumFolderPath
            } else {
                prefs.remove(Keys.albumFolderPath)
            }
            if (songFilePath != null) {
                prefs[Keys.songFilePath] = songFilePath
            } else {
                prefs.remove(Keys.songFilePath)
            }
            prefs[Keys.positionMs] = positionMs
            prefs[Keys.playModeOrdinal] = playMode.ordinal.toLong()
        }
    }

    suspend fun clearProgress() {
        context.playbackDataStore.edit { prefs ->
            prefs.remove(Keys.positionMs)
        }
    }
}

data class PlaybackSnapshot(
    val albumFolderPath: String?,
    val songFilePath: String?,
    val positionMs: Long,
    val playMode: PlayMode,
)
