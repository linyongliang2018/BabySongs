package com.swqsv.babysongs.ui.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.RemoteViews
import com.swqsv.babysongs.R
import com.swqsv.babysongs.playback.PlaybackUiState
import com.swqsv.babysongs.ui.MainActivity

object WidgetUpdater {

    private enum class WidgetTier {
        SMALL,
        MEDIUM,
        LARGE,
    }

    fun updateAll(context: Context, state: PlaybackUiState? = null) {
        val appContext = context.applicationContext
        val manager = AppWidgetManager.getInstance(appContext)
        val provider = ComponentName(appContext, BabySongsWidgetProvider::class.java)
        val appWidgetIds = manager.getAppWidgetIds(provider)
        if (appWidgetIds.isEmpty()) return

        val uiState = state ?: currentPlaybackState(appContext)
        for (id in appWidgetIds) {
            val views = buildRemoteViews(appContext, id, uiState, manager)
            manager.updateAppWidget(id, views)
        }
    }

    fun updateById(context: Context, appWidgetId: Int) {
        val appContext = context.applicationContext
        val manager = AppWidgetManager.getInstance(appContext)
        val uiState = currentPlaybackState(appContext)
        val views = buildRemoteViews(appContext, appWidgetId, uiState, manager)
        manager.updateAppWidget(appWidgetId, views)
    }

    private fun currentPlaybackState(context: Context): PlaybackUiState? {
        val app = context.applicationContext as? com.swqsv.babysongs.BabySongsApplication ?: return null
        return app.playbackController.state.value
    }

    private fun tierFor(manager: AppWidgetManager, appWidgetId: Int): WidgetTier {
        val options: Bundle = manager.getAppWidgetOptions(appWidgetId)
        val minW = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 0)
        val minH = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 0)
        val maxW = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH, 0)
        val maxH = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT, 0)
        val w = approxDp(minW, maxW)
        val h = approxDp(minH, maxH)
        if (w <= 0 && h <= 0) return WidgetTier.MEDIUM
        val smallByWidth = w in 1 until 280
        val smallByHeight = h in 1 until 100
        val largeByWidth = w >= 380
        val largeByHeight = h >= 200
        return when {
            largeByWidth || largeByHeight -> WidgetTier.LARGE
            smallByWidth || smallByHeight -> WidgetTier.SMALL
            else -> WidgetTier.MEDIUM
        }
    }

    /** 用 min/max 近似当前档位（不同桌面给出的语义略有差异）。 */
    private fun approxDp(minV: Int, maxV: Int): Int {
        return when {
            minV > 0 && maxV > 0 -> (minV + maxV) / 2
            minV > 0 -> minV
            maxV > 0 -> maxV
            else -> 0
        }
    }

    private fun layoutIdFor(tier: WidgetTier): Int = when (tier) {
        WidgetTier.SMALL -> R.layout.widget_baby_songs_small
        WidgetTier.MEDIUM -> R.layout.widget_baby_songs_medium
        WidgetTier.LARGE -> R.layout.widget_baby_songs_large
    }

    private fun buildRemoteViews(
        context: Context,
        appWidgetId: Int,
        state: PlaybackUiState?,
        manager: AppWidgetManager,
    ): RemoteViews {
        val tier = tierFor(manager, appWidgetId)
        val layoutId = layoutIdFor(tier)
        val views = RemoteViews(context.packageName, layoutId)
        val songTitle = state?.currentSong?.title ?: context.getString(R.string.widget_idle_title)
        val albumTitle = state?.currentAlbum?.name ?: context.getString(R.string.widget_idle_subtitle)
        views.setTextViewText(R.id.widget_song_title, songTitle)
        views.setTextViewText(R.id.widget_album_title, albumTitle)
        val icon = if (state?.isPlaying == true) {
            R.drawable.ic_widget_pause
        } else {
            R.drawable.ic_widget_play
        }
        views.setImageViewResource(R.id.widget_btn_play_pause, icon)

        val rc = requestCodeBase(appWidgetId)
        views.setOnClickPendingIntent(R.id.widget_root, openAppPendingIntent(context, rc + 0))
        views.setOnClickPendingIntent(
            R.id.widget_btn_play_pause,
            actionPendingIntent(context, WidgetActions.ACTION_PLAY_PAUSE, rc + 1),
        )
        views.setOnClickPendingIntent(
            R.id.widget_btn_next,
            actionPendingIntent(context, WidgetActions.ACTION_NEXT, rc + 2),
        )
        views.setOnClickPendingIntent(
            R.id.widget_btn_prev,
            actionPendingIntent(context, WidgetActions.ACTION_PREVIOUS, rc + 3),
        )
        return views
    }

    private fun requestCodeBase(appWidgetId: Int): Int = 10_000 + appWidgetId * 10

    private fun openAppPendingIntent(context: Context, requestCode: Int): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        return PendingIntent.getActivity(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private fun actionPendingIntent(context: Context, action: String, requestCode: Int): PendingIntent {
        val intent = Intent(context, PlaybackActionReceiver::class.java).apply {
            this.action = action
        }
        return PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }
}
