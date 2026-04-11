package com.swqsv.babysongs.ui

/**
 * 桌面长按图标快捷方式：与 [android:name] 为 [EXTRA_SHORTCUT_ACTION] 的 Intent extra 对应。
 */
enum class AppShortcutAction {
    AddCategory,
    AddLibraryFolder,
}

const val EXTRA_SHORTCUT_ACTION = "com.swqsv.babysongs.extra.SHORTCUT_ACTION"

const val SHORTCUT_VALUE_ADD_CATEGORY = "add_category"
const val SHORTCUT_VALUE_ADD_LIBRARY_FOLDER = "add_library_folder"

fun parseShortcutExtra(value: String?): AppShortcutAction? = when (value) {
    SHORTCUT_VALUE_ADD_CATEGORY -> AppShortcutAction.AddCategory
    SHORTCUT_VALUE_ADD_LIBRARY_FOLDER -> AppShortcutAction.AddLibraryFolder
    else -> null
}
