package com.swqsv.babysongs.playback

import android.content.Intent
import android.util.Log
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.swqsv.babysongs.BabySongsApplication

/**
 * 前台媒体服务：绑定 [MediaSession]，与系统通知栏/耳机线控对接。
 * [ExoPlayer] 由 [PlaybackController] 在 Application 中创建，此处只负责会话与生命周期。
 *
 * **必须在** [startForegroundService] 后极短时间内调用 [startForeground]，否则系统抛出
 * [android.app.ForegroundServiceDidNotStartInTimeException] 导致进程崩溃；因此 [onCreate] 在
 * [super.onCreate] 之后**立刻**调用 [PlaybackForeground.promoteToForeground]（早于 [MediaSession] 构建）。
 */
class BabyPlaybackService : MediaSessionService() {

    private var foregroundReady: Boolean = false

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "onCreate")
        ensureForeground("onCreate")
        val app = application as BabySongsApplication
        val controller = app.playbackController
        if (controller.mediaSession == null) {
            controller.mediaSession = MediaSession.Builder(this, controller.player).build()
            Log.d(TAG, "MediaSession created")
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "onStartCommand startId=$startId flags=$flags action=${intent?.action}")
        ensureForeground("onStartCommand")
        return super.onStartCommand(intent, flags, startId)
    }

    /**
     * 幂等：部分 ROM 时序下 [onStartCommand] 可能先于其它逻辑再次进入，重复 [startForeground] 安全。
     */
    private fun ensureForeground(reason: String) {
        if (foregroundReady) {
            Log.v(TAG, "ensureForeground: already active, skip ($reason)")
            return
        }
        try {
            PlaybackForeground.promoteToForeground(this)
            foregroundReady = true
            Log.i(TAG, "ensureForeground ok ($reason)")
        } catch (e: Exception) {
            Log.e(TAG, "ensureForeground FAILED ($reason): ${e.javaClass.simpleName} ${e.message}", e)
            throw e
        }
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? {
        return (application as BabySongsApplication).playbackController.mediaSession
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        Log.d(TAG, "onTaskRemoved")
        val controller = (application as BabySongsApplication).playbackController
        if (!controller.player.playWhenReady) {
            stopSelf()
        }
    }

    override fun onDestroy() {
        Log.d(TAG, "onDestroy")
        foregroundReady = false
        val app = application as BabySongsApplication
        app.playbackController.releaseMediaSessionOnly()
        super.onDestroy()
    }

    private companion object {
        const val TAG: String = "BabyPlaybackSvc"
    }
}
