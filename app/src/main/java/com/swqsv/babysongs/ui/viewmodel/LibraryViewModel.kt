package com.swqsv.babysongs.ui.viewmodel

import android.app.Application
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.swqsv.babysongs.BabySongsApplication
import com.swqsv.babysongs.data.model.Album
import com.swqsv.babysongs.data.model.Song
import com.swqsv.babysongs.data.prefs.PlaybackSnapshot
import com.swqsv.babysongs.data.scan.MediaLibraryScanner
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class LibraryRootInfo(
    val displayName: String,
    val treeUriString: String,
)

data class LibraryUiState(
    val albums: List<Album> = emptyList(),
    val isLoading: Boolean = false,
    val message: String? = null,
    /** 已添加文档根（顺序与添加顺序一致），用于展示与移除 */
    val libraryRoots: List<LibraryRootInfo> = emptyList(),
)

class LibraryViewModel(application: Application) : AndroidViewModel(application) {

    private val scanner = MediaLibraryScanner()
    private val app = application as BabySongsApplication

    /**
     * 限制并发的元数据读取线程数，兼顾骁龙等多核与存储带宽，避免同时打开过多 [MediaMetadataRetriever]。
     */
    private val songDurationDispatcher = Dispatchers.IO.limitedParallelism(4)

    private var hasRestoredFromSnapshot: Boolean = false

    private val _uiState = MutableStateFlow(LibraryUiState())
    val uiState: StateFlow<LibraryUiState> = _uiState.asStateFlow()

    /**
     * 用户通过系统文件夹选择器选定目录后调用；追加 URI 并重新扫描。
     */
    fun onLibraryRootPicked(treeUri: Uri) {
        viewModelScope.launch {
            app.libraryRootPreferences.addRootTreeUri(treeUri.toString())
            hasRestoredFromSnapshot = false
            refreshLibrary()
        }
    }

    fun removeLibraryRoot(treeUriString: String) {
        viewModelScope.launch {
            val uri = runCatching { Uri.parse(treeUriString) }.getOrNull()
            if (uri != null) {
                app.songIndexCache.delete(MediaLibraryScanner.flatAlbumId(uri))
            }
            app.libraryRootPreferences.removeRootTreeUri(treeUriString)
            hasRestoredFromSnapshot = false
            refreshLibrary()
        }
    }

    fun refreshLibrary() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, message = null)
            try {
                app.libraryRootPreferences.migrateLegacyIfNeeded()
                val uriStrings = app.libraryRootPreferences.rootTreeUriStrings.first()
                if (uriStrings.isEmpty()) {
                    _uiState.value = LibraryUiState(
                        albums = emptyList(),
                        isLoading = false,
                        message = null,
                        libraryRoots = emptyList(),
                    )
                    return@launch
                }
                val treeUris = uriStrings.map { Uri.parse(it) }
                val (libraryRoots, fastAlbums) = withContext(Dispatchers.IO) {
                    val roots = uriStrings.zip(treeUris).map { (uriStr, uri) ->
                        val name = DocumentFile.fromTreeUri(getApplication(), uri)?.name
                            ?: uri.lastPathSegment
                            ?: uriStr
                        LibraryRootInfo(displayName = name, treeUriString = uriStr)
                    }
                    val albums = treeUris.map { uri ->
                        val id = MediaLibraryScanner.flatAlbumId(uri)
                        val cached = app.songIndexCache.loadSongs(id)
                        val count = cached?.size ?: 0
                        val name = DocumentFile.fromTreeUri(getApplication(), uri)?.name
                            ?: uri.lastPathSegment
                            ?: "本目录"
                        Album(
                            id = id,
                            name = name,
                            folderPath = MediaLibraryScanner.flatFolderPath(uri),
                            songCount = count,
                        )
                    }
                    roots to albums
                }
                _uiState.value = LibraryUiState(
                    albums = fastAlbums,
                    isLoading = false,
                    message = null,
                    libraryRoots = libraryRoots,
                )
                viewModelScope.launch {
                    tryRestoreFromSnapshot(fastAlbums)
                }
                viewModelScope.launch(Dispatchers.IO) {
                    try {
                        val scanned = scanner.scanRootsAsAlbums(getApplication(), treeUris)
                        withContext(Dispatchers.Main.immediate) {
                            _uiState.value = _uiState.value.copy(albums = scanned)
                        }
                    } catch (_: Exception) {
                    }
                }
            } catch (e: Exception) {
                _uiState.value = LibraryUiState(
                    albums = emptyList(),
                    isLoading = false,
                    message = e.message ?: "扫描失败",
                    libraryRoots = emptyList(),
                )
            }
        }
    }

    suspend fun getCachedSongsOrNull(album: Album): List<Song>? {
        return withContext(Dispatchers.IO) {
            app.songIndexCache.loadSongs(album.id)
        }
    }

    /**
     * 全量扫描文档树并写入缓存；用于「重新扫描」或与缓存合并后的后台刷新。
     */
    suspend fun scanSongsAndPersist(album: Album): List<Song> {
        val list = scanner.scanSongsForAlbum(getApplication(), album)
        withContext(Dispatchers.IO) {
            app.songIndexCache.saveSongs(album.id, list)
        }
        return list
    }

    suspend fun persistSongsCache(album: Album, songs: List<Song>) {
        withContext(Dispatchers.IO) {
            app.songIndexCache.saveSongs(album.id, songs)
        }
    }

    suspend fun getSongsForAlbum(album: Album): List<Song> {
        return scanner.scanSongsForAlbum(getApplication(), album)
    }

    /**
     * 在一批歌曲内并发补全时长（受 [songDurationDispatcher] 限制），保持返回顺序与 [songs] 一致。
     */
    suspend fun enrichSongChunkParallel(songs: List<Song>): List<Song> = coroutineScope {
        songs.map { song ->
            async(songDurationDispatcher) {
                scanner.enrichSongDurationSync(getApplication(), song)
            }
        }.awaitAll()
    }

    private suspend fun tryRestoreFromSnapshot(albums: List<Album>) {
        if (hasRestoredFromSnapshot) return
        val snapshot: PlaybackSnapshot = app.playbackPreferences.snapshot.first()
        val albumPath = snapshot.albumFolderPath ?: return
        val songPath = snapshot.songFilePath ?: return
        val album = albums.firstOrNull { it.folderPath == albumPath } ?: return
        val songs = withContext(Dispatchers.IO) {
            app.songIndexCache.loadSongs(album.id)
                ?: scanner.scanSongsForAlbum(getApplication(), album).also { list ->
                    app.songIndexCache.saveSongs(album.id, list)
                }
        }
        val song = songs.firstOrNull { it.filePath == songPath } ?: return
        withContext(Dispatchers.Main.immediate) {
            app.playbackController.restoreFromLibrary(
                album = album,
                songs = songs,
                song = song,
                positionMs = snapshot.positionMs,
                mode = snapshot.playMode,
            )
        }
        hasRestoredFromSnapshot = true
    }
}
