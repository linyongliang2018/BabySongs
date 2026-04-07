package com.swqsv.babysongs.data.model

/**
 * [id] 使用音频文件绝对路径。
 */
data class Song(
    val id: String,
    val title: String,
    val filePath: String,
    val albumName: String,
    val durationMs: Long,
    val mimeType: String,
)
