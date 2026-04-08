package com.swqsv.babysongs.ui.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent

class BabySongsWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray,
    ) {
        WidgetUpdater.updateAll(context)
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == WidgetActions.ACTION_REFRESH) {
            WidgetUpdater.updateAll(context)
        }
    }

    override fun onAppWidgetOptionsChanged(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        newOptions: android.os.Bundle,
    ) {
        super.onAppWidgetOptionsChanged(context, appWidgetManager, appWidgetId, newOptions)
        WidgetUpdater.updateById(context, appWidgetId)
    }

    companion object {
        fun requestUpdate(context: Context) {
            val intent = Intent(context, BabySongsWidgetProvider::class.java).apply {
                action = WidgetActions.ACTION_REFRESH
            }
            context.sendBroadcast(intent)
        }

        fun providerComponent(context: Context): ComponentName {
            return ComponentName(context, BabySongsWidgetProvider::class.java)
        }
    }
}
