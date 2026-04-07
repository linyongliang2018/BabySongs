package com.swqsv.babysongs.data.model

/**
 * [id] 使用专辑文件夹绝对路径，保证稳定可持久化。
 */
data class Album(
    val id: String,
    val name: String,
    val folderPath: String,
    val songCount: Int,
)
