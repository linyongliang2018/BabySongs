package com.swqsv.babysongs.playback

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import com.swqsv.babysongs.R
import com.swqsv.babysongs.ui.MainActivity

/**
 * 前台播放通知：必须在 [startForegroundService] 之后极短时间内调用 [startForeground]，否则会触发
 * [android.app.ForegroundServiceDidNotStartInTimeException] 导致进程崩溃。
 */
object PlaybackForeground {

    private const val TAG = "BabyPlaybackFg"

    const val NOTIFICATION_ID: Int = 1001
    const val CHANNEL_ID: String = "playback_media"

    fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val nm = context.getSystemService(NotificationManager::class.java) ?: run {
            Log.w(TAG, "ensureChannel: NotificationManager null")
            return
        }
        if (nm.getNotificationChannel(CHANNEL_ID) != null) {
            Log.d(TAG, "ensureChannel: channel already exists")
            return
        }
        val ch = NotificationChannel(
            CHANNEL_ID,
            context.getString(R.string.notification_channel_playback_name),
            NotificationManager.IMPORTANCE_LOW,
        ).apply {
            description = context.getString(R.string.notification_channel_playback_desc)
            setShowBadge(false)
        }
        nm.createNotificationChannel(ch)
        Log.i(TAG, "ensureChannel: created channel=$CHANNEL_ID")
    }

    /**
     * 立即将服务提升为前台；应在 [Service.onCreate] 中尽早调用（在其它耗时逻辑之前）。
     */
    fun promoteToForeground(service: Service) {
        Log.d(TAG, "promoteToForeground begin")
        ensureChannel(service)
        val pendingIntent = PendingIntent.getActivity(
            service,
            0,
            Intent(service, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val notification: android.app.Notification = NotificationCompat.Builder(service, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(service.getString(R.string.notification_playback_title))
            .setContentText(service.getString(R.string.notification_playback_text))
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_TRANSPORT)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .build()

        val fgType = ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
        try {
            // ServiceCompat 会按系统版本选择带 type / 不带 type 的 startForeground
            ServiceCompat.startForeground(service, NOTIFICATION_ID, notification, fgType)
            Log.i(TAG, "startForeground ok notificationId=$NOTIFICATION_ID fgType=mediaPlayback")
        } catch (e: SecurityException) {
            Log.e(
                TAG,
                "startForeground SecurityException (检查 POST_NOTIFICATIONS 等权限): ${e.message}",
                e,
            )
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "startForeground failed: ${e.javaClass.simpleName} ${e.message}", e)
            throw e
        }
    }
}
