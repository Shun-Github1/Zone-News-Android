package com.searcher.zonenews.widget

import android.content.Context
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Intent
import android.util.Log
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.searcher.zonenews.entry.HomeDataListEntry
import com.searcher.zonenews.utils.Constants
import com.tencent.mmkv.MMKV
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query
import java.security.SecureRandom
import java.security.cert.X509Certificate
import java.util.Locale
import java.util.concurrent.TimeUnit
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager
import com.google.gson.reflect.TypeToken

/**
 * Widget-specific API service interface
 */
interface WidgetApiService {
    @GET("feed")
    suspend fun getHomeData(
        @Query("tag") tag: String? = null,
        @Query("offset") offset: Int? = null,
        @Query("limit") limit: Int? = null,
        @Query("lang") language: String? = null
    ): HomeDataListEntry
}

/**
 * Provides news data for widgets
 * Now caches a LIST of articles for navigation support
 */
object WidgetDataProvider {
    
    const val ACTION_WIDGET_REFRESH = "com.searcher.zonenews.widget.ACTION_REFRESH"
    
    private const val TAG = "WidgetDataProvider"
    private const val WIDGET_CACHE_KEY_LIST = "widget_cached_article_list"
    private const val WIDGET_CACHE_TIMESTAMP_KEY = "widget_cache_timestamp"
    private const val CACHE_VALIDITY_MS = 15 * 60 * 1000L  // 15 minutes cache validity
    
    // SharedPreferences for persistent widget data
    private const val WIDGET_PREFS_NAME = "widget_persistent_data"
    
    // Key to track current index per widget
    private const val KEY_WIDGET_INDEX_PREFIX = "widget_index_"
    
    // Legacy keys (kept for compatibility or fallback)
    private const val KEY_WIDGET_ARTICLE_ID_PREFIX = "widget_article_id_"
    private const val KEY_LAST_ARTICLE_ID = "last_article_id"
    private const val KEY_LAST_ARTICLE_TITLE = "last_article_title"
    
    private val gson = Gson()
    
    private val widgetApiService: WidgetApiService by lazy {
        createWidgetApiService()
    }
    
    private fun createWidgetApiService(): WidgetApiService {
        // Trust all certificates
        val trustAllCerts = arrayOf<TrustManager>(object : X509TrustManager {
            override fun checkClientTrusted(chain: Array<X509Certificate>, authType: String) {}
            override fun checkServerTrusted(chain: Array<X509Certificate>, authType: String) {}
            override fun getAcceptedIssuers(): Array<X509Certificate> = arrayOf()
        })
        
        val sslContext = SSLContext.getInstance("SSL").apply {
            init(null, trustAllCerts, SecureRandom())
        }
        
        val okHttpClient = OkHttpClient.Builder()
            .sslSocketFactory(sslContext.socketFactory, trustAllCerts[0] as X509TrustManager)
            .hostnameVerifier { _, _ -> true }
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .retryOnConnectionFailure(false)
            .build()
        
        val retrofit = Retrofit.Builder()
            .baseUrl(Constants.COMMON_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create(GsonBuilder().create()))
            .build()
        
        return retrofit.create(WidgetApiService::class.java)
    }
    
    /**
     * Get the current article for a specific widget
     * Fetches fresh data if cache missing/expired
     */
    suspend fun getCurrentArticle(context: Context, widgetId: Int): HomeDataListEntry.DataDTO.ArticlesDTO? {
        return withContext(Dispatchers.IO) {
            // 1. Try to get cached list
            var articles = getCachedArticles()
            
            // 2. If no cache or expired, fetch fresh data
            if (articles.isNullOrEmpty()) {
                articles = fetchFreshData(context)
                if (!articles.isNullOrEmpty()) {
                    cacheArticles(context, articles)
                }
            }
            
            // 3. Return current article based on widget index
            if (!articles.isNullOrEmpty()) {
                val index = getWidgetIndex(context, widgetId)
                // Ensure index is valid
                val safeIndex = index % articles.size
                val article = articles[safeIndex]
                
                // Save this ID as the persistent/current one for this widget
                saveWidgetArticleId(context, widgetId, article.articleID, article.title)
                return@withContext article
            }
            
            null
        }
    }
    
    /**
     * Advance to NEXT article for this widget
     */
    suspend fun getNextArticle(context: Context, widgetId: Int): HomeDataListEntry.DataDTO.ArticlesDTO? {
        return withContext(Dispatchers.IO) {
            val articles = getCachedArticles() ?: fetchFreshData(context)
            if (articles.isNullOrEmpty()) return@withContext null
            
            // Move index forward
            var index = getWidgetIndex(context, widgetId)
            index = (index + 1) % articles.size
            saveWidgetIndex(context, widgetId, index)
            
            val article = articles[index]
            saveWidgetArticleId(context, widgetId, article.articleID, article.title)
            article
        }
    }
    
    /**
     * Go to PREVIOUS article for this widget
     */
    suspend fun getPreviousArticle(context: Context, widgetId: Int): HomeDataListEntry.DataDTO.ArticlesDTO? {
        return withContext(Dispatchers.IO) {
            val articles = getCachedArticles() ?: fetchFreshData(context)
            if (articles.isNullOrEmpty()) return@withContext null
            
            // Move index backward
            var index = getWidgetIndex(context, widgetId)
            index = (index - 1 + articles.size) % articles.size
            saveWidgetIndex(context, widgetId, index)
            
            val article = articles[index]
            saveWidgetArticleId(context, widgetId, article.articleID, article.title)
            article
        }
    }
    
    /**
     * Fetch fresh data from API (10 items from "today" feed)
     */
    private suspend fun fetchFreshData(context: Context): List<HomeDataListEntry.DataDTO.ArticlesDTO>? {
        return try {
            // Use APP language instead of system language
            val languageCode = getAppLanguageCode(context)
            
            val response = widgetApiService.getHomeData(
                tag = "today",
                offset = 0,
                limit = 10,
                language = languageCode
            )
            
            if (response.code == 200) {
                val articles = response.data?.articles
                if (!articles.isNullOrEmpty()) {
                    Log.d(TAG, "Fetched ${articles.size} fresh articles (Lang: $languageCode)")
                    return articles
                }
            }
            null
        } catch (e: Exception) {
            Log.e(TAG, "API call failed", e)
            null
        }
    }
    
    /**
     * Cache the LIST of articles
     */
    private fun cacheArticles(context: Context, articles: List<HomeDataListEntry.DataDTO.ArticlesDTO>) {
        try {
            val mmkv = MMKV.defaultMMKV()
            val json = gson.toJson(articles)
            mmkv.encode(WIDGET_CACHE_KEY_LIST, json)
            mmkv.encode(WIDGET_CACHE_TIMESTAMP_KEY, System.currentTimeMillis())
            Log.d(TAG, "Cached ${articles.size} articles")
        } catch (e: Exception) {
            Log.e(TAG, "Error caching articles", e)
        }
    }
    
    /**
     * Get cached list
     */
    private fun getCachedArticles(): List<HomeDataListEntry.DataDTO.ArticlesDTO>? {
        try {
            val mmkv = MMKV.defaultMMKV()
            val json = mmkv.decodeString(WIDGET_CACHE_KEY_LIST)
            if (json.isNullOrEmpty()) return null
            
            // Check expiry
            val cacheTimestamp = mmkv.decodeLong(WIDGET_CACHE_TIMESTAMP_KEY, 0)
            if (System.currentTimeMillis() - cacheTimestamp > CACHE_VALIDITY_MS) {
                return null
            }
            
            val type = object : TypeToken<List<HomeDataListEntry.DataDTO.ArticlesDTO>>() {}.type
            return gson.fromJson(json, type)
        } catch (e: Exception) {
            Log.e(TAG, "Error reading cache", e)
            return null
        }
    }
    
    /**
     * Save current index for a widget
     */
    private fun saveWidgetIndex(context: Context, widgetId: Int, index: Int) {
        context.getSharedPreferences(WIDGET_PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putInt(KEY_WIDGET_INDEX_PREFIX + widgetId, index)
            .apply()
    }
    
    /**
     * Get current index for a widget (default 0)
     */
    private fun getWidgetIndex(context: Context, widgetId: Int): Int {
        return context.getSharedPreferences(WIDGET_PREFS_NAME, Context.MODE_PRIVATE)
            .getInt(KEY_WIDGET_INDEX_PREFIX + widgetId, 0)
    }

    /**
     * Get page status (current index 1-based, total count)
     */
    fun getPageStatus(context: Context, widgetId: Int): Pair<Int, Int>? {
        val articles = getCachedArticles()
        if (articles.isNullOrEmpty()) return null
        
        val index = getWidgetIndex(context, widgetId)
        val safeIndex = index % articles.size
        
        return Pair(safeIndex + 1, articles.size)
    }

    /**
     * Save article ID to persistent SharedPreferences (for deep linking)
     */
    fun saveWidgetArticleId(context: Context, widgetId: Int, articleId: String?, title: String?) {
        if (articleId.isNullOrEmpty()) return
        try {
            val prefs = context.getSharedPreferences(WIDGET_PREFS_NAME, Context.MODE_PRIVATE)
            prefs.edit()
                .putString(KEY_WIDGET_ARTICLE_ID_PREFIX + widgetId, articleId)
                .apply()
        } catch (e: Exception) {
            Log.e(TAG, "Error saving widget data", e)
        }
    }
    
    /**
     * Get stored article ID for deep linking
     */
    fun getStoredArticleId(context: Context, widgetId: Int): String? {
        return context.getSharedPreferences(WIDGET_PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_WIDGET_ARTICLE_ID_PREFIX + widgetId, null)
    }

    /**
     * Update from home data (called by App)
     */
    suspend fun updateFromHomeData(context: Context, articles: List<HomeDataListEntry.DataDTO.ArticlesDTO>) {
        withContext(Dispatchers.IO) {
            if (articles.isNotEmpty()) {
                cacheArticles(context, articles)
                
                // Trigger update for both widgets
                try {
                    val appWidgetManager = AppWidgetManager.getInstance(context)
                    
                    // Detailed Widget
                    val detailedIds = appWidgetManager.getAppWidgetIds(ComponentName(context, DetailedNewsWidgetProvider::class.java))
                    if (detailedIds.isNotEmpty()) {
                        val intent = Intent(context, DetailedNewsWidgetProvider::class.java)
                        intent.action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
                        intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, detailedIds)
                        context.sendBroadcast(intent)
                    }
                    
                    // Compact Widget
                    val compactIds = appWidgetManager.getAppWidgetIds(ComponentName(context, CompactNewsWidgetProvider::class.java))
                    if (compactIds.isNotEmpty()) {
                        val intent = Intent(context, CompactNewsWidgetProvider::class.java)
                        intent.action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
                        intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, compactIds)
                        context.sendBroadcast(intent)
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error updating widgets from home data", e)
                }
            }
        }
    }
    fun clearWidgetData(context: Context, widgetId: Int) {
        try {
            context.getSharedPreferences(WIDGET_PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .remove(KEY_WIDGET_INDEX_PREFIX + widgetId)
                .remove(KEY_WIDGET_ARTICLE_ID_PREFIX + widgetId)
                .apply()
        } catch (e: Exception) {
            Log.e(TAG, "Error clearing widget data", e)
        }
    }
    
    /**
     * Force refresh data from API and update widgets
     */
    suspend fun refreshWidgetData(context: Context) {
        val articles = fetchFreshData(context)
        if (!articles.isNullOrEmpty()) {
            updateFromHomeData(context, articles)
        }
    }


    /**
     * Get backend language code based on app preference
     */
    fun getAppLanguageCode(context: Context): String {
        val prefs = context.getSharedPreferences("language_preferences", Context.MODE_PRIVATE)
        val savedCode = prefs.getString("selected_language", null)
        
        // If we have a saved code, use it to look up the Language enum (which has the correct backend code)
        if (savedCode != null) {
            val language = com.searcher.zonenews.utils.Language.values().find { it.code == savedCode }
            if (language != null) {
                return language.code
            }
        }
        
        // Fallback to system default if no preference saved
        val locale = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
            context.resources.configuration.locales[0]
        } else {
            @Suppress("DEPRECATION")
            context.resources.configuration.locale
        }
        
        val localeTag = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            locale?.toLanguageTag() ?: "en"
        } else {
            val language = locale?.language ?: "en"
            val country = try { locale?.country } catch (_: Throwable) { null }
            if (!country.isNullOrEmpty()) "$language-$country" else language
        }
        
        return com.searcher.zonenews.utils.Language.getBackendCode(localeTag)
    }

    /**
     * Get a Context wrapped with the specific Locale for the App's selected language
     * This ensures getString() returns the correct language regardless of system language
     */
    fun getLocalizedContext(context: Context): Context {
        val prefs = context.getSharedPreferences("language_preferences", Context.MODE_PRIVATE)
        val savedCode = prefs.getString("selected_language", null) ?: return context
        
        val language = com.searcher.zonenews.utils.Language.values().find { it.code == savedCode } ?: return context
        
        // Map backend code back to Locale
        // Simple mapping based on known supported languages
        val locale = when (language) {
            com.searcher.zonenews.utils.Language.SIMPLIFIED_CHINESE -> Locale.SIMPLIFIED_CHINESE
            com.searcher.zonenews.utils.Language.TRADITIONAL_CHINESE -> Locale.TRADITIONAL_CHINESE
            com.searcher.zonenews.utils.Language.CHINESE_TAIWAN -> Locale.TRADITIONAL_CHINESE
            com.searcher.zonenews.utils.Language.ENGLISH_UK -> Locale.UK
            com.searcher.zonenews.utils.Language.ENGLISH_US -> Locale.US
            else -> Locale.UK
        }
        
        val config = android.content.res.Configuration(context.resources.configuration)
        config.setLocale(locale)
        return context.createConfigurationContext(config)
    }
}
