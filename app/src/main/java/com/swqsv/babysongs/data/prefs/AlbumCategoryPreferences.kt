package com.swqsv.babysongs.data.prefs

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.swqsv.babysongs.data.model.AlbumCategoryUi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

private val Context.albumCategoryDataStore: DataStore<Preferences> by preferencesDataStore(name = "album_categories")

/**
 * 自定义分类列表，以及「专辑 id → 分类 id」映射（未出现在映射中的专辑视为未分类）。
 */
class AlbumCategoryPreferences(private val context: Context) {

    private object Keys {
        val categoriesJson = stringPreferencesKey("categories_json")
        val albumToCategoryJson = stringPreferencesKey("album_to_category_json")
    }

    val categoriesFlow: Flow<List<AlbumCategoryUi>> = context.albumCategoryDataStore.data.map { prefs ->
        parseCategories(prefs[Keys.categoriesJson].orEmpty())
    }

    /** 仅含已分类专辑；未出现的键表示未分类。 */
    val albumToCategoryIdFlow: Flow<Map<String, String>> = context.albumCategoryDataStore.data.map { prefs ->
        parseAlbumToCategory(prefs[Keys.albumToCategoryJson].orEmpty())
    }

    suspend fun addCategory(name: String): String? {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return null
        val id = UUID.randomUUID().toString()
        context.albumCategoryDataStore.edit { prefs ->
            val list = parseCategories(prefs[Keys.categoriesJson].orEmpty()).toMutableList()
            list.add(AlbumCategoryUi(id = id, name = trimmed))
            prefs[Keys.categoriesJson] = categoriesToJson(list)
        }
        return id
    }

    suspend fun renameCategory(id: String, newName: String) {
        val trimmed = newName.trim()
        if (trimmed.isEmpty()) return
        context.albumCategoryDataStore.edit { prefs ->
            val list = parseCategories(prefs[Keys.categoriesJson].orEmpty()).map {
                if (it.id == id) it.copy(name = trimmed) else it
            }
            prefs[Keys.categoriesJson] = categoriesToJson(list)
        }
    }

    /**
     * 删除分类后，原属于该分类的专辑映射会被移除（变为未分类）。
     */
    suspend fun deleteCategory(id: String) {
        context.albumCategoryDataStore.edit { prefs ->
            val list = parseCategories(prefs[Keys.categoriesJson].orEmpty()).filterNot { it.id == id }
            prefs[Keys.categoriesJson] = categoriesToJson(list)
            val map = parseAlbumToCategory(prefs[Keys.albumToCategoryJson].orEmpty()).toMutableMap()
            map.keys.toList().forEach { albumId ->
                if (map[albumId] == id) map.remove(albumId)
            }
            prefs[Keys.albumToCategoryJson] = albumToCategoryToJson(map)
        }
    }

    suspend fun setAlbumCategory(albumId: String, categoryId: String?) {
        context.albumCategoryDataStore.edit { prefs ->
            val map = parseAlbumToCategory(prefs[Keys.albumToCategoryJson].orEmpty()).toMutableMap()
            if (categoryId == null) {
                map.remove(albumId)
            } else {
                map[albumId] = categoryId
            }
            prefs[Keys.albumToCategoryJson] = albumToCategoryToJson(map)
        }
    }

    private companion object {
        fun parseCategories(json: String): List<AlbumCategoryUi> {
            if (json.isEmpty()) return emptyList()
            return try {
                val arr = JSONArray(json)
                List(arr.length()) { i ->
                    val o = arr.getJSONObject(i)
                    AlbumCategoryUi(
                        id = o.getString("id"),
                        name = o.getString("name"),
                    )
                }
            } catch (_: Exception) {
                emptyList()
            }
        }

        fun categoriesToJson(list: List<AlbumCategoryUi>): String {
            val arr = JSONArray()
            list.forEach { c ->
                arr.put(JSONObject().put("id", c.id).put("name", c.name))
            }
            return arr.toString()
        }

        fun parseAlbumToCategory(json: String): Map<String, String> {
            if (json.isEmpty()) return emptyMap()
            return try {
                val o = JSONObject(json)
                val keys = o.keys()
                val out = mutableMapOf<String, String>()
                while (keys.hasNext()) {
                    val k = keys.next()
                    out[k] = o.getString(k)
                }
                out
            } catch (_: Exception) {
                emptyMap()
            }
        }

        fun albumToCategoryToJson(map: Map<String, String>): String {
            val o = JSONObject()
            map.forEach { (k, v) -> o.put(k, v) }
            return o.toString()
        }
    }
}
