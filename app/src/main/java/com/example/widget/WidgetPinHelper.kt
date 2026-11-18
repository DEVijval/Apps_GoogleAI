package com.example.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build

object WidgetPinHelper {
    fun isPinSupported(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return false
        val appWidgetManager = AppWidgetManager.getInstance(context)
        return appWidgetManager.isRequestPinAppWidgetSupported
    }

    fun requestPinWidget(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return false
        val appWidgetManager = AppWidgetManager.getInstance(context)
        if (!appWidgetManager.isRequestPinAppWidgetSupported) return false
        val provider = ComponentName(context, CalmFlowWidgetReceiver::class.java)
        val callbackIntent = Intent(context, CalmFlowWidgetReceiver::class.java)
        val successCallback = PendingIntent.getBroadcast(
            context,
            901,
            callbackIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        return appWidgetManager.requestPinAppWidget(provider, null, successCallback)
    }
}
