package com.icarus.pixelinvaders.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.Handler
import android.os.HandlerThread
import android.os.IBinder
import android.os.SystemClock
import com.icarus.pixelinvaders.R
import com.icarus.pixelinvaders.game.GameSession
import com.icarus.pixelinvaders.game.Phase
import com.icarus.pixelinvaders.widget.WidgetUpdater

/**
 * Drives the game at ~20 fps while it is being played, redrawing the widget each frame. Button
 * taps arrive as intents. The service stops itself after a stretch without action so it never
 * drains the battery in the background.
 */
class GameLoopService : Service() {

    private var thread: HandlerThread? = null
    private var handler: Handler? = null
    private var lastFrameMs = 0L
    private var idleMs = 0L

    private val tick = object : Runnable {
        override fun run() {
            val now = SystemClock.elapsedRealtime()
            val dt = (now - lastFrameMs) / 1000f
            lastFrameMs = now

            val phase = GameSession.step(this@GameLoopService, dt)
            WidgetUpdater.updateAll(this@GameLoopService)

            idleMs = if (phase == Phase.RUNNING) 0L else idleMs + FRAME_MS
            if (idleMs >= IDLE_LIMIT_MS) {
                stopSelfCleanly()
            } else {
                handler?.postDelayed(this, FRAME_MS)
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startInForeground()
        when (intent?.action) {
            ACTION_LEFT -> GameSession.moveLeft(this)
            ACTION_RIGHT -> GameSession.moveRight(this)
            ACTION_FIRE -> GameSession.fire(this)
        }
        idleMs = 0L
        ensureLoop()
        return START_NOT_STICKY
    }

    private fun ensureLoop() {
        if (thread != null) return
        val t = HandlerThread("invaders-loop").also { it.start() }
        thread = t
        handler = Handler(t.looper).also {
            lastFrameMs = SystemClock.elapsedRealtime()
            it.post(tick)
        }
    }

    private fun startInForeground() {
        val nm = getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(
            NotificationChannel(CHANNEL_ID, getString(R.string.channel_name), NotificationManager.IMPORTANCE_MIN),
        )
        val notification = Notification.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(getString(R.string.notification_text))
            .setOngoing(true)
            .build()
        startForeground(NOTIFICATION_ID, notification)
    }

    private fun stopSelfCleanly() {
        handler?.removeCallbacks(tick)
        stopForeground(true)
        stopSelf()
    }

    override fun onDestroy() {
        handler?.removeCallbacksAndMessages(null)
        thread?.quitSafely()
        thread = null
        handler = null
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        const val ACTION_LEFT = "com.icarus.pixelinvaders.action.LEFT"
        const val ACTION_RIGHT = "com.icarus.pixelinvaders.action.RIGHT"
        const val ACTION_FIRE = "com.icarus.pixelinvaders.action.FIRE"
        private const val CHANNEL_ID = "invaders_loop"
        private const val NOTIFICATION_ID = 1
        private const val FRAME_MS = 50L
        private const val IDLE_LIMIT_MS = 15_000L
    }
}
