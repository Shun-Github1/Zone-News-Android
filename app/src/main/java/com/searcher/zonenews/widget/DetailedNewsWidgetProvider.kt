package com.searcher.zonenews.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.util.Log
import android.view.View
import android.widget.RemoteViews
import com.searcher.zonenews.utils.AppIconManager
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.AppWidgetTarget
import com.bumptech.glide.request.transition.Transition
import com.searcher.zonenews.R
import com.searcher.zonenews.entry.HomeDataListEntry
import com.searcher.zonenews.ui.MainActivity
import com.searcher.zonenews.utils.CalculateUtil
import com.searcher.zonenews.utils.Utils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlin.math.abs

/**
 * Detailed News Widget Provider
 * Shows all news card elements: region, topic, heading, bias text, articles/time, sentiment bar, and image
 */
class DetailedNewsWidgetProvider : AppWidgetProvider() {

    companion object {
        private const val TAG = "DetailedNewsWidget"
        const val ACTION_REFRESH = "com.searcher.zonenews.widget.ACTION_REFRESH_DETAILED"
        const val ACTION_NEXT = "com.searcher.zonenews.widget.ACTION_NEXT_DETAILED"
        const val ACTION_PREV = "com.searcher.zonenews.widget.ACTION_PREV_DETAILED"
        
        /**
         * Request widget update from any context
         */
        fun requestUpdate(context: Context) {
            val intent = Intent(context, DetailedNewsWidgetProvider::class.java).apply {
                action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
                val ids = AppWidgetManager.getInstance(context)
                    .getAppWidgetIds(ComponentName(context, DetailedNewsWidgetProvider::class.java))
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
            }
            context.sendBroadcast(intent)
        }
    }

    private val job = SupervisorJob()
    private val scope = CoroutineScope(Dispatchers.IO + job)

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        Log.d(TAG, "onUpdate called for ${appWidgetIds.size} widgets")
        for (appWidgetId in appWidgetIds) {
            updateWidget(context, appWidgetManager, appWidgetId)
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        
        when (intent.action) {
            WidgetDataProvider.ACTION_WIDGET_REFRESH -> {
                Log.d(TAG, "Refresh action received")
                
                // Show loading state immediately
                val appWidgetManager = AppWidgetManager.getInstance(context)
                val componentName = ComponentName(context, DetailedNewsWidgetProvider::class.java)
                val appWidgetIds = appWidgetManager.getAppWidgetIds(componentName)
                
                for (appWidgetId in appWidgetIds) {
                    showLoadingState(context, appWidgetManager, appWidgetId)
                }
                
                val pendingResult = goAsync()
                scope.launch {
                    try {
                        WidgetDataProvider.refreshWidgetData(context)
                    } catch (e: Exception) {
                        Log.e(TAG, "Refresh error", e)
                    } finally {
                        pendingResult.finish()
                    }
                }
            }
            ACTION_NEXT -> {
                val appWidgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
                if (appWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
                    // Show loading state
                    showLoadingState(context, AppWidgetManager.getInstance(context), appWidgetId)
                    
                    scope.launch {
                        val article = WidgetDataProvider.getNextArticle(context, appWidgetId)
                        if (article != null) {
                            updateWidgetWithData(context, AppWidgetManager.getInstance(context), appWidgetId, article)
                        } else {
                             val current = WidgetDataProvider.getCurrentArticle(context, appWidgetId)
                             if (current != null) {
                                  updateWidgetWithData(context, AppWidgetManager.getInstance(context), appWidgetId, current)
                             } else {
                                  showErrorState(context, AppWidgetManager.getInstance(context), appWidgetId)
                             }
                        }
                    }
                }
            }
            ACTION_PREV -> {
                val appWidgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
                if (appWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
                    // Show loading state
                    showLoadingState(context, AppWidgetManager.getInstance(context), appWidgetId)
                    
                    scope.launch {
                        val article = WidgetDataProvider.getPreviousArticle(context, appWidgetId)
                        if (article != null) {
                            updateWidgetWithData(context, AppWidgetManager.getInstance(context), appWidgetId, article)
                        } else {
                             val current = WidgetDataProvider.getCurrentArticle(context, appWidgetId)
                             if (current != null) {
                                  updateWidgetWithData(context, AppWidgetManager.getInstance(context), appWidgetId, current)
                             } else {
                                  showErrorState(context, AppWidgetManager.getInstance(context), appWidgetId)
                             }
                        }
                    }
                }
            }
        }
    }

    override fun onEnabled(context: Context) {
        super.onEnabled(context)
        Log.d(TAG, "Widget enabled")
    }

    override fun onDisabled(context: Context) {
        super.onDisabled(context)
        Log.d(TAG, "Widget disabled")
        job.cancel()
    }
    
    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        super.onDeleted(context, appWidgetIds)
        // Clean up persistent data for deleted widgets
        for (widgetId in appWidgetIds) {
            WidgetDataProvider.clearWidgetData(context, widgetId)
        }
    }

    private fun updateWidget(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int
    ) {
        // Show loading state
        showLoadingState(context, appWidgetManager, appWidgetId)
        
        // Fetch data and update widget
        scope.launch {
            try {
                val article = WidgetDataProvider.getCurrentArticle(context, appWidgetId)
                if (article != null && !article.articleID.isNullOrEmpty()) {
                    updateWidgetWithData(context, appWidgetManager, appWidgetId, article)
                } else {
                    // Check if we have a stored article ID to at least enable deep link
                    val storedId = WidgetDataProvider.getStoredArticleId(context, appWidgetId)
                    if (!storedId.isNullOrEmpty()) {
                        Log.d(TAG, "Using stored article ID: $storedId")
                        showRefreshStateWithDeepLink(context, appWidgetManager, appWidgetId, storedId)
                    } else {
                        showErrorState(context, appWidgetManager, appWidgetId)
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error updating widget", e)
                showErrorState(context, appWidgetManager, appWidgetId)
            }
        }
    }

    private fun showLoadingState(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int
    ) {
        val views = RemoteViews(context.packageName, R.layout.widget_detailed)
        views.setViewVisibility(R.id.widget_loading_container, View.VISIBLE)
        views.setViewVisibility(R.id.widget_error_container, View.GONE)
        appWidgetManager.updateAppWidget(appWidgetId, views)
    }

    private fun showErrorState(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int
    ) {
        val views = RemoteViews(context.packageName, R.layout.widget_detailed)
        views.setViewVisibility(R.id.widget_loading_container, View.GONE)
        views.setViewVisibility(R.id.widget_error_container, View.VISIBLE)
        
        // Set tap to refresh intent
        val refreshIntent = Intent(context, DetailedNewsWidgetProvider::class.java).apply {
            action = WidgetDataProvider.ACTION_WIDGET_REFRESH
        }
        val refreshPendingIntent = PendingIntent.getBroadcast(
            context, 0, refreshIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(R.id.widget_container, refreshPendingIntent)
        
        // Use localized text
        val localizedContext = WidgetDataProvider.getLocalizedContext(context)
        views.setTextViewText(R.id.widget_error_text, localizedContext.getString(R.string.widget_tap_to_refresh))
        
        views.setOnClickPendingIntent(R.id.widget_container, refreshPendingIntent)
        
        // Hide navigation and page tracker
        views.setViewVisibility(R.id.widget_btn_prev, View.GONE)
        views.setViewVisibility(R.id.widget_btn_next, View.GONE)
        views.setViewVisibility(R.id.widget_btn_refresh, View.GONE)
        views.setViewVisibility(R.id.widget_page_tracker, View.GONE)
        
        appWidgetManager.updateAppWidget(appWidgetId, views)
    }
    
    /**
     * Show a refresh-needed state but still allow deep link with stored article ID
     */
    private fun showRefreshStateWithDeepLink(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        articleId: String
    ) {
        val views = RemoteViews(context.packageName, R.layout.widget_detailed)
        views.setViewVisibility(R.id.widget_loading_container, View.GONE)
        views.setViewVisibility(R.id.widget_error_container, View.VISIBLE)
        val localizedContext = WidgetDataProvider.getLocalizedContext(context)
        views.setTextViewText(R.id.widget_error_text, localizedContext.getString(R.string.widget_tap_to_refresh))
        
        // Use explicit intent to MainActivity with article ID as extra
        // This is more reliable than implicit deep links when app is killed
        val articleIntent = Intent(context, MainActivity::class.java).apply {
            putExtra("widget_article_id", articleId)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val articlePendingIntent = PendingIntent.getActivity(
            context, appWidgetId, articleIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(R.id.widget_content_wrapper, articlePendingIntent)
        
        // Set dynamic app icon
        views.setImageViewResource(R.id.widget_logo, AppIconManager.getCurrentIconResourceId(context))
        
        // Hide navigation and page tracker
        views.setViewVisibility(R.id.widget_btn_prev, View.GONE)
        views.setViewVisibility(R.id.widget_btn_next, View.GONE)
        views.setViewVisibility(R.id.widget_btn_refresh, View.GONE)
        views.setViewVisibility(R.id.widget_page_tracker, View.GONE)
        
        appWidgetManager.updateAppWidget(appWidgetId, views)
    }

    private fun updateWidgetWithData(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        article: HomeDataListEntry.DataDTO.ArticlesDTO
    ) {
        val views = RemoteViews(context.packageName, R.layout.widget_detailed)
        
        // Hide loading and error states
        views.setViewVisibility(R.id.widget_loading_container, View.GONE)
        // Hide loading and error states
        views.setViewVisibility(R.id.widget_loading_container, View.GONE)
        views.setViewVisibility(R.id.widget_error_container, View.GONE)
        
        // Ensure navigation and page tracker are visible
        views.setViewVisibility(R.id.widget_btn_prev, View.VISIBLE)
        views.setViewVisibility(R.id.widget_btn_next, View.VISIBLE)
        views.setViewVisibility(R.id.widget_btn_refresh, View.VISIBLE)
        views.setViewVisibility(R.id.widget_page_tracker, View.VISIBLE)
        
        // Set text content
        views.setTextViewText(R.id.widget_region, article.region ?: "")
        views.setTextViewText(R.id.widget_topic, article.sector ?: "")
        views.setTextViewText(R.id.widget_headline, article.title ?: "")
        
        // Use localized context for multilingual support
        val localizedContext = WidgetDataProvider.getLocalizedContext(context)
        
        // Set sentiment/bias text
        val sentimentScore = article.metrics?.sentiment ?: 0.0
        val sentimentText = localizedContext.getString(CalculateUtil.getSentimentLabelResId(sentimentScore))
        views.setTextViewText(R.id.widget_bias_text, sentimentText)
        
        // precise sentiment color
        val colorName = CalculateUtil.getSentimentColorName(sentimentScore)
        val colorResId = context.resources.getIdentifier(colorName, "color", context.packageName)
        if (colorResId != 0) {
            views.setTextColor(R.id.widget_bias_text, context.resources.getColor(colorResId, context.theme))
        }
        
        // Set articles count and time
        val articlesCount = localizedContext.getString(R.string.reports_count, article.nSources ?: 0)
        views.setTextViewText(R.id.widget_articles_count, articlesCount)
        views.setTextViewText(R.id.widget_time, Utils.formatBackendDate(localizedContext, article.date))
        
        // Set sentiment bar - calculate width percentage
        updateSentimentBar(views, sentimentScore)
        
        // Update page tracker
        val pageStatus = WidgetDataProvider.getPageStatus(context, appWidgetId)
        if (pageStatus != null) {
            views.setTextViewText(R.id.widget_page_tracker, "${pageStatus.first}/${pageStatus.second}")
        } else {
             views.setTextViewText(R.id.widget_page_tracker, "")
        }
        
        // Save article ID persistently for this widget
        WidgetDataProvider.saveWidgetArticleId(context, appWidgetId, article.articleID, article.title)
        
        // Use explicit intent to MainActivity with article ID as extra
        // This is more reliable than implicit deep links when app process is killed
        val articleId = article.articleID
        if (!articleId.isNullOrEmpty()) {
            val articleIntent = Intent(context, MainActivity::class.java).apply {
                putExtra("widget_article_id", articleId)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val articlePendingIntent = PendingIntent.getActivity(
                context, appWidgetId, articleIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_container, articlePendingIntent)
        } else {
            // Fall back to refresh action if no valid article ID
            val refreshIntent = Intent(context, DetailedNewsWidgetProvider::class.java).apply {
                action = ACTION_REFRESH
            }
            val refreshPendingIntent = PendingIntent.getBroadcast(
                context, appWidgetId, refreshIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_container, refreshPendingIntent)
        }
        
        // Navigation Buttons
        val nextIntent = Intent(context, DetailedNewsWidgetProvider::class.java).apply {
            action = ACTION_NEXT
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
        }
        val nextPendingIntent = PendingIntent.getBroadcast(
            context, appWidgetId, nextIntent, // Use appWidgetId as requestCode to distinct
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(R.id.widget_btn_next, nextPendingIntent)

        val prevIntent = Intent(context, DetailedNewsWidgetProvider::class.java).apply {
            action = ACTION_PREV
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
        }
        val prevPendingIntent = PendingIntent.getBroadcast(
            context, -appWidgetId, prevIntent, // Use distinct requestCode (negative?) or just distinct val
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(R.id.widget_btn_prev, prevPendingIntent)
        
        // Refresh Button
        val refreshIntent = Intent(context, DetailedNewsWidgetProvider::class.java).apply {
            action = WidgetDataProvider.ACTION_WIDGET_REFRESH
        }
        val refreshPendingIntent = PendingIntent.getBroadcast(
             context, 0, refreshIntent, 
             PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(R.id.widget_btn_refresh, refreshPendingIntent)
        
        // Update widget first with text content
        appWidgetManager.updateAppWidget(appWidgetId, views)
        
        // Load image asynchronously using Glide
        loadImage(context, appWidgetManager, appWidgetId, article.pictureURL)
    }

    private fun updateSentimentBar(views: RemoteViews, sentiment: Double) {
        // Clamp sentiment between -1 and 1
        val clampedSentiment = sentiment.coerceIn(-1.0, 1.0)
        
        // Use setImageLevel with ClipDrawable to control bar width (0-10000 range)
        // Positive bar grows from center to right
        // Negative bar grows from center to left
        
        // Check for neutral range [-0.1, 0.1]
        val isNeutral = abs(clampedSentiment) <= 0.1
        
        if (isNeutral) {
            // Use neutral gray clips
            views.setImageViewResource(R.id.widget_sentiment_positive, R.drawable.widget_clip_neutral_positive)
            views.setImageViewResource(R.id.widget_sentiment_negative, R.drawable.widget_clip_neutral_negative)
        } else {
            // Restore standard color clips
            views.setImageViewResource(R.id.widget_sentiment_positive, R.drawable.widget_clip_positive)
            views.setImageViewResource(R.id.widget_sentiment_negative, R.drawable.widget_clip_negative)
        }
        
        if (clampedSentiment >= 0) {
            val level = (clampedSentiment * 10000).toInt()
            views.setInt(R.id.widget_sentiment_positive, "setImageLevel", level)
            views.setInt(R.id.widget_sentiment_negative, "setImageLevel", 0)
        } else {
            val level = (kotlin.math.abs(clampedSentiment) * 10000).toInt()
            views.setInt(R.id.widget_sentiment_positive, "setImageLevel", 0)
            views.setInt(R.id.widget_sentiment_negative, "setImageLevel", level)
        }
    }

    private fun loadImage(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        imageUrl: String?
    ) {
        if (imageUrl.isNullOrEmpty()) return

        val appWidgetTarget = object : AppWidgetTarget(
            context.applicationContext,
            R.id.widget_news_image,
            RemoteViews(context.packageName, R.layout.widget_detailed),
            appWidgetId
        ) {
            override fun onResourceReady(resource: Bitmap, transition: Transition<in Bitmap>?) {
                val views = RemoteViews(context.packageName, R.layout.widget_detailed)
                views.setImageViewBitmap(R.id.widget_news_image, resource)
                appWidgetManager.partiallyUpdateAppWidget(appWidgetId, views)
            }
        }

        try {
            Glide.with(context.applicationContext)
                .asBitmap()
                .load(imageUrl)
                .placeholder(R.drawable.widget_image_placeholder)
                .error(R.drawable.ic_image_not_supported_24)
                .override(400, 300)  // Limit image size for widget
                .centerCrop()
                .into(appWidgetTarget)
        } catch (e: Exception) {
            Log.e(TAG, "Error loading image", e)
        }
    }
}
