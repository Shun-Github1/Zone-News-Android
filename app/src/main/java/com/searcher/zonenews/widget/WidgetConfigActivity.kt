package com.searcher.zonenews.widget

import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.searcher.zonenews.R

/**
 * Configuration activity for widgets
 * Currently passes through without configuration, but can be extended for user customization
 */
class WidgetConfigActivity : AppCompatActivity() {

    private var appWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Set the result to CANCELED in case the user backs out
        setResult(RESULT_CANCELED)
        
        // Get the widget id from the intent
        appWidgetId = intent?.extras?.getInt(
            AppWidgetManager.EXTRA_APPWIDGET_ID,
            AppWidgetManager.INVALID_APPWIDGET_ID
        ) ?: AppWidgetManager.INVALID_APPWIDGET_ID
        
        // If no valid widget ID, finish
        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish()
            return
        }
        
        // For now, skip configuration and just confirm widget creation
        // In the future, this could show a configuration UI
        confirmWidget()
    }
    
    private fun confirmWidget() {
        // Trigger an immediate update for the new widget
        val appWidgetManager = AppWidgetManager.getInstance(this)
        
        // Update both widget types
        DetailedNewsWidgetProvider.requestUpdate(this)
        CompactNewsWidgetProvider.requestUpdate(this)
        
        // Create the result intent and set the widget ID
        val resultValue = Intent().apply {
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
        }
        
        setResult(RESULT_OK, resultValue)
        finish()
    }
}
