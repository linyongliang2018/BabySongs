package com.swqsv.babysongs.data.cache

import android.content.Context
import com.swqsv.babysongs.data.model.Song
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.security.MessageDigest

/**
 * 将专辑内歌曲列表缓存在应用私有目录，避免每次进入列表都完整遍历 SAF 文档树。
 * 「重新扫描」或进入列表后的后台全量扫描会覆盖为最新结果。
 */
class SongIndexCache(context: Context) {

    private val dir = File(context.filesDir, "song_index").apply { mkdirs() }

    fun loadSongs(albumId: String): List<Song>? {
        val f = fileFor(albumId)
        if (!f.exists() || f.length() == 0L) return null
        return try {
            val json = f.readText(Charsets.UTF_8)
            val root = JSONObject(json)
            val arr = root.optJSONArray(JSON_SONGS) ?: return null
            val version = root.optInt(JSON_KEY_VERSION, 0)
            if (version != CACHE_VERSION) return null
            List(arr.length()) { i ->
                val o = arr.getJSONObject(i)
                Song(
                    id = o.getString("id"),
                    title = o.getString("title"),
                    filePath = o.getString("filePath"),
                    albumName = o.getString("albumName"),
                    durationMs = o.optLong("durationMs", 0L),
                    mimeType = o.optString("mimeType", "application/octet-stream"),
                )
            }
        } catch (_: Exception) {
            null
        }
    }

    fun saveSongs(albumId: String, songs: List<Song>) {
        try {
            val arr = JSONArray()
            songs.forEach { s ->
                arr.put(
                    JSONObject().apply {
                        put("id", s.id)
                        put("title", s.title)
                        put("filePath", s.filePath)
                        put("albumName", s.albumName)
                        put("durationMs", s.durationMs)
                        put("mimeType", s.mimeType)
                    },
                )
            }
            val root = JSONObject().apply {
                put(JSON_KEY_VERSION, CACHE_VERSION)
                put(JSON_SONGS, arr)
            }
            val f = fileFor(albumId)
            f.writeText(root.toString(), Charsets.UTF_8)
        } catch (_: Exception) {
            // 缓存失败不影响主流程
        }
    }

    fun delete(albumId: String) {
        try {
            fileFor(albumId).delete()
        } catch (_: Exception) {
        }
    }

    private fun fileFor(albumId: String): File {
        val name = sha256Hex(albumId) + ".json"
        return File(dir, name)
    }

    private fun sha256Hex(input: String): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(input.toByteArray(Charsets.UTF_8))
        return digest.joinToString("") { b -> "%02x".format(b) }
    }

    private companion object {
        const val CACHE_VERSION = 1
        const val JSON_KEY_VERSION = "version"
        const val JSON_SONGS = "songs"
    }
}
