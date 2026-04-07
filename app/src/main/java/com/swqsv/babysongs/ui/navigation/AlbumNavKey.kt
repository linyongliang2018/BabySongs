package com.swqsv.babysongs.ui.navigation

import android.util.Base64

/**
 * 将 [album.id]（可能含 `content://` 等字符）编码为 Navigation 路径安全片段。
 */
object AlbumNavKey {

    fun encode(albumId: String): String {
        return Base64.encodeToString(
            albumId.toByteArray(Charsets.UTF_8),
            Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING,
        )
    }

    fun decode(segment: String): String? {
        return try {
            val bytes = Base64.decode(
                segment,
                Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING,
            )
            String(bytes, Charsets.UTF_8)
        } catch (_: Throwable) {
            null
        }
    }
}
