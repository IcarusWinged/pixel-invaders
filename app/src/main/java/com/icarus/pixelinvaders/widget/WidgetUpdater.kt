package com.icarus.pixelinvaders.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.widget.RemoteViews
import com.icarus.pixelinvaders.R
import com.icarus.pixelinvaders.data.SettingsStore
import com.icarus.pixelinvaders.game.GameSession
import com.icarus.pixelinvaders.render.GameRenderer
import com.icarus.pixelinvaders.service.GameLoopService
import kotlin.math.roundToInt

/** Renders the current game frame into every placed widget and wires up the three buttons. */
object WidgetUpdater {
    private var renderer: GameRenderer? = null

    @Synchronized
    private fun renderer(context: Context): GameRenderer {
        renderer?.let { return it }
        val opts = BitmapFactory.Options().apply { inScaled = false }
        val atlas = BitmapFactory.decodeResource(context.resources, R.drawable.spritesheet, opts)
        return GameRenderer(atlas).also { renderer = it }
    }

    fun allIds(context: Context): IntArray {
        val mgr = AppWidgetManager.getInstance(context)
        return mgr.getAppWidgetIds(ComponentName(context, InvadersWidgetProvider::class.java))
    }

    fun updateAll(context: Context) = update(context, allIds(context))

    fun update(context: Context, ids: IntArray) {
        if (ids.isEmpty()) return
        val mgr = AppWidgetManager.getInstance(context)
        val palette = SettingsStore(context).theme.palette
        val r = renderer(context)

        for (id in ids) {
            val bitmap = GameSession.read(context) { r.render(it, palette, widthPx(context, mgr, id)) }
            val views = RemoteViews(context.packageName, R.layout.widget_invaders)
            views.setImageViewBitmap(R.id.game_image, bitmap)
            views.setInt(R.id.widget_root, "setBackgroundColor", palette.bg)
            for (button in intArrayOf(R.id.btn_left, R.id.btn_fire, R.id.btn_right)) {
                views.setInt(button, "setBackgroundColor", palette.buttonBg)
                views.setTextColor(button, palette.buttonText)
            }
            views.setOnClickPendingIntent(R.id.btn_left, action(context, GameLoopService.ACTION_LEFT, 1))
            views.setOnClickPendingIntent(R.id.btn_fire, action(context, GameLoopService.ACTION_FIRE, 2))
            views.setOnClickPendingIntent(R.id.btn_right, action(context, GameLoopService.ACTION_RIGHT, 3))
            mgr.updateAppWidget(id, views)
        }
    }

    /** Widget width in pixels from its current dp size (updates when the user resizes it). */
    private fun widthPx(context: Context, mgr: AppWidgetManager, id: Int): Int {
        val opts = mgr.getAppWidgetOptions(id)
        val wDp = opts.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, DEFAULT_DP)
        return (wDp * context.resources.displayMetrics.density).roundToInt()
    }

    private fun action(context: Context, action: String, requestCode: Int): PendingIntent {
        val intent = Intent(context, GameLoopService::class.java).setAction(action)
        return PendingIntent.getForegroundService(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private const val DEFAULT_DP = 250
}
