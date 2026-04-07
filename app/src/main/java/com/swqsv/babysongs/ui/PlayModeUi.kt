package com.swqsv.babysongs.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.swqsv.babysongs.R
import com.swqsv.babysongs.data.model.PlayMode

fun cyclePlayMode(current: PlayMode): PlayMode {
    return when (current) {
        PlayMode.ORDER -> PlayMode.LIST_LOOP
        PlayMode.LIST_LOOP -> PlayMode.SINGLE_LOOP
        PlayMode.SINGLE_LOOP -> PlayMode.SHUFFLE
        PlayMode.SHUFFLE -> PlayMode.ORDER
    }
}

@Composable
fun playModeLabel(mode: PlayMode): String {
    val id = when (mode) {
        PlayMode.ORDER -> R.string.mode_order
        PlayMode.LIST_LOOP -> R.string.mode_list_loop
        PlayMode.SINGLE_LOOP -> R.string.mode_single_loop
        PlayMode.SHUFFLE -> R.string.mode_shuffle
    }
    return stringResource(id = id)
}
