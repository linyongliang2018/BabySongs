package com.swqsv.babysongs.data.scan

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import com.swqsv.babysongs.data.model.Album
import com.swqsv.babysongs.data.model.Song
import androidx.documentfile.provider.DocumentFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.yield
import java.util.Locale
import java.util.concurrent.atomic.AtomicInteger

private const val FLAT_PREFIX = "flat:"

/**
 * 在 IO 线程扫描用户授权的文档树根 [Uri]（通常为 [Intent.ACTION_OPEN_DOCUMENT_TREE] 结果）。
 * 每个授权根目录对应**一张**专辑；该目录下**递归**包含的 mp3/mp4 均归入此专辑。
 */
class MediaLibraryScanner {

    /**
     * 按 [treeUris] 顺序，为每个根目录生成一张专辑（含子文件夹内音频）。
     */
    suspend fun scanRootsAsAlbums(context: Context, treeUris: List<Uri>): List<Album> = withContext(Dispatchers.IO) {
        treeUris.mapNotNull { scanSingleRootAsOneAlbum(context, it) }
    }

    private suspend fun scanSingleRootAsOneAlbum(context: Context, treeUri: Uri): Album? {
        val root = DocumentFile.fromTreeUri(context, treeUri) ?: return null
        val files = mutableListOf<DocumentFile>()
        val counter = AtomicInteger(0)
        collectAudioDocumentsRecursive(root, files, counter)
        files.sortBy { it.uri.toString().lowercase(Locale.getDefault()) }
        val displayName = root.name ?: "本目录"
        return Album(
            id = flatAlbumId(treeUri),
            name = displayName,
            folderPath = flatFolderPath(treeUri),
            songCount = files.size,
        )
    }

    suspend fun scanSongsForAlbum(context: Context, album: Album): List<Song> = withContext(Dispatchers.IO) {
        if (album.folderPath.startsWith(FLAT_PREFIX)) {
            val treeUri = Uri.parse(album.folderPath.removePrefix(FLAT_PREFIX))
            val root = DocumentFile.fromTreeUri(context, treeUri) ?: return@withContext emptyList()
            val files = mutableListOf<DocumentFile>()
            val counter = AtomicInteger(0)
            collectAudioDocumentsRecursive(root, files, counter)
            files.sortBy { it.uri.toString().lowercase(Locale.getDefault()) }
            val albumName = root.name ?: "本目录"
            files.map { doc -> songFromDocumentFast(doc, albumName) }
        } else {
            val folderUri = Uri.parse(album.folderPath)
            val folder = DocumentFile.fromSingleUri(context, folderUri)
                ?: return@withContext emptyList()
            if (!folder.isDirectory) return@withContext emptyList()
            val albumName = folder.name ?: album.name
            listSongDocuments(folder).map { doc ->
                songFromDocumentFast(doc, albumName)
            }
        }
    }

    /**
     * 补全单首歌曲时长（[MediaMetadataRetriever]）。
     * 应在 IO 线程池上由调用方并发调度；本方法同步阻塞当前线程。
     */
    fun enrichSongDurationSync(context: Context, song: Song): Song {
        if (song.durationMs > 0L) return song
        return song.copy(durationMs = readDurationMsSafe(context, Uri.parse(song.filePath)))
    }

    /**
     * 大目录下 [DocumentFile.listFiles] 耗时较长，每隔若干文件 [yield] 一次，避免长时间占满 IO 线程、
     * 减轻主线程因密集更新导致的卡顿。
     */
    private suspend fun collectAudioDocumentsRecursive(
        folder: DocumentFile,
        out: MutableList<DocumentFile>,
        counter: AtomicInteger,
    ) {
        val children = folder.listFiles() ?: return
        for (child in children) {
            when {
                child.isFile && isSupportedAudio(child.name ?: "") -> {
                    out.add(child)
                    val c = counter.incrementAndGet()
                    if (c % YIELD_EVERY_N_FILES == 0) yield()
                }
                child.isDirectory -> collectAudioDocumentsRecursive(child, out, counter)
            }
        }
    }

    private fun listSongDocuments(folder: DocumentFile): List<DocumentFile> {
        val files = folder.listFiles()?.filter { child ->
            child.isFile && isSupportedAudio(child.name ?: "")
        } ?: emptyList()
        return files.sortedBy { it.name?.lowercase(Locale.getDefault()) ?: "" }
    }

    /** 仅枚举文件信息，不读时长，避免大目录下数百次 [MediaMetadataRetriever] 阻塞首屏。 */
    private fun songFromDocumentFast(doc: DocumentFile, albumName: String): Song {
        val uri = doc.uri
        val fileName = doc.name ?: "audio"
        val title = fileName.substringBeforeLast('.')
        return Song(
            id = uri.toString(),
            title = title,
            filePath = uri.toString(),
            albumName = albumName,
            durationMs = 0L,
            mimeType = guessMimeType(fileName),
        )
    }

    private fun isSupportedAudio(fileName: String): Boolean {
        val lower = fileName.lowercase(Locale.getDefault())
        return lower.endsWith(".mp3") || lower.endsWith(".mp4")
    }

    private fun guessMimeType(fileName: String): String {
        val lower = fileName.lowercase(Locale.getDefault())
        return when {
            lower.endsWith(".mp3") -> "audio/mpeg"
            lower.endsWith(".mp4") -> "video/mp4"
            else -> "application/octet-stream"
        }
    }

    private fun readDurationMsSafe(context: Context, uri: Uri): Long {
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(context, uri)
            val dur = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
            dur?.toLongOrNull() ?: 0L
        } catch (_: Throwable) {
            0L
        } finally {
            try {
                retriever.release()
            } catch (_: Throwable) {
                // ignore
            }
        }
    }

    companion object {
        private const val YIELD_EVERY_N_FILES = 48

        fun flatAlbumId(treeUri: Uri): String = "${FLAT_PREFIX}${treeUri}"

        fun flatFolderPath(treeUri: Uri): String = "${FLAT_PREFIX}${treeUri}"
    }
}
