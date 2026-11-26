package com.icarus.pixelinvaders.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.os.Bundle
import com.icarus.pixelinvaders.service.GameLoopService

/** Entry point for the home screen widget: redraws on add, resize and system updates. */
class InvadersWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        WidgetUpdater.update(context, ids)
    }

    override fun onAppWidgetOptionsChanged(
        context: Context,
        manager: AppWidgetManager,
        id: Int,
        newOptions: Bundle,
    ) {
        WidgetUpdater.update(context, intArrayOf(id))
    }

    override fun onDisabled(context: Context) {
        context.stopService(Intent(context, GameLoopService::class.java))
    }
}
