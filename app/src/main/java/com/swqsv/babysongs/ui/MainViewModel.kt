package com.swqsv.babysongs.ui

import android.content.Intent
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class MainViewModel : ViewModel() {
    private val _pendingShortcut = MutableStateFlow<AppShortcutAction?>(null)
    val pendingShortcut: StateFlow<AppShortcutAction?> = _pendingShortcut.asStateFlow()

    /** 仅当 Intent 带有快捷方式 extra 时更新，避免覆盖尚未消费的动作。 */
    fun applyShortcutIntent(intent: Intent?) {
        val action = parseShortcutExtra(intent?.getStringExtra(EXTRA_SHORTCUT_ACTION)) ?: return
        _pendingShortcut.value = action
    }

    fun consumePendingShortcut() {
        _pendingShortcut.value = null
    }
}
