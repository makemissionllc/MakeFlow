package com.makemission.makeflow.widget

import android.content.Context
import androidx.glance.appwidget.updateAll

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

object WidgetUpdateHelper {
    fun updateWidgets(context: Context) {
        CoroutineScope(Dispatchers.Main).launch {
            MakeFlowWidget().updateAll(context)
            QuickActionWidget().updateAll(context)
        }
    }
}
