package com.swqsv.babysongs.ui.widget

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.swqsv.babysongs.BabySongsApplication

class PlaybackActionReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action ?: return
        val app = context.applicationContext as? BabySongsApplication ?: return
        val controller = app.playbackController
        when (action) {
            WidgetActions.ACTION_PLAY_PAUSE -> controller.playPause()
            WidgetActions.ACTION_NEXT -> controller.skipToNext(context.applicationContext)
            WidgetActions.ACTION_PREVIOUS -> controller.skipToPrevious(context.applicationContext)
            else -> return
        }
        BabySongsWidgetProvider.requestUpdate(context.applicationContext)
    }
}
