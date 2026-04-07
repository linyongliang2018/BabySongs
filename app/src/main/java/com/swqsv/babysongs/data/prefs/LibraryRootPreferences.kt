package com.swqsv.babysongs.data.prefs

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.json.JSONArray

private val Context.libraryRootDataStore: DataStore<Preferences> by preferencesDataStore(name = "library_root")

/**
 * 用户通过系统文档树选择的「儿歌根目录」[content Uri](https://developer.android.com/guide/topics/providers/document-provider)，可多个，顺序与添加顺序一致。
 */
class LibraryRootPreferences(private val context: Context) {

    private object Keys {
        /** JSON 字符串数组，每项为 tree URI 字符串 */
        val rootTreeUriListJson = stringPreferencesKey("root_tree_uri_list")
        /** 旧版单条 */
        val rootTreeUriLegacy = stringPreferencesKey("root_tree_uri")
        /** 中间版本 string set（若存在则迁移到 JSON 列表） */
        val rootTreeUrisSet = androidx.datastore.preferences.core.stringSetPreferencesKey("root_tree_uris")
    }

    val rootTreeUriStrings: Flow<List<String>> = context.libraryRootDataStore.data.map { prefs ->
        parseUriList(prefs)
    }

    private fun parseUriList(prefs: Preferences): List<String> {
        val json = prefs[Keys.rootTreeUriListJson]
        if (json != null) {
            return jsonStringToList(json)
        }
        val set = prefs[Keys.rootTreeUrisSet]
        if (!set.isNullOrEmpty()) {
            return set.toList()
        }
        val legacy = prefs[Keys.rootTreeUriLegacy]
        return if (legacy != null) listOf(legacy) else emptyList()
    }

    /**
     * 将旧版单键或 string set 并入 JSON 列表并移除旧键；应在读取库列表前调用一次。
     */
    suspend fun migrateLegacyIfNeeded() {
        context.libraryRootDataStore.edit { prefs ->
            val fromJson = jsonStringToList(prefs[Keys.rootTreeUriListJson].orEmpty())
            val legacy = prefs[Keys.rootTreeUriLegacy]
            val set = prefs[Keys.rootTreeUrisSet]
            if (legacy == null && set.isNullOrEmpty()) {
                return@edit
            }
            val ordered = linkedSetOf<String>()
            ordered.addAll(fromJson)
            legacy?.let { ordered.add(it) }
            set?.forEach { ordered.add(it) }
            prefs.remove(Keys.rootTreeUriLegacy)
            prefs.remove(Keys.rootTreeUrisSet)
            if (ordered.isEmpty()) {
                prefs.remove(Keys.rootTreeUriListJson)
            } else {
                prefs[Keys.rootTreeUriListJson] = listToJsonString(ordered.toList())
            }
        }
    }

    suspend fun addRootTreeUri(uriString: String) {
        context.libraryRootDataStore.edit { prefs ->
            val list = jsonStringToList(prefs[Keys.rootTreeUriListJson].orEmpty()).toMutableList()
            if (uriString !in list) {
                list.add(uriString)
            }
            prefs[Keys.rootTreeUriListJson] = listToJsonString(list)
            prefs.remove(Keys.rootTreeUriLegacy)
            prefs.remove(Keys.rootTreeUrisSet)
        }
    }

    suspend fun removeRootTreeUri(uriString: String) {
        context.libraryRootDataStore.edit { prefs ->
            val list = jsonStringToList(prefs[Keys.rootTreeUriListJson].orEmpty()).filterNot { it == uriString }
            if (list.isEmpty()) {
                prefs.remove(Keys.rootTreeUriListJson)
            } else {
                prefs[Keys.rootTreeUriListJson] = listToJsonString(list)
            }
        }
    }

    private companion object {
        fun listToJsonString(uris: List<String>): String {
            val arr = JSONArray()
            uris.forEach { arr.put(it) }
            return arr.toString()
        }

        fun jsonStringToList(json: String): List<String> {
            if (json.isEmpty()) return emptyList()
            return try {
                val arr = JSONArray(json)
                List(arr.length()) { arr.getString(it) }
            } catch (_: Exception) {
                emptyList()
            }
        }
    }
}
