package com.searcher.zonenews.repository

import com.searcher.zonenews.entry.ArticleDetailEntry
import com.searcher.zonenews.entry.CommonResponseEntry
import com.searcher.zonenews.entry.PublisherInfoEntry
import com.searcher.zonenews.net.AppHttpService
import com.searcher.zonenews.utils.Utils
import com.searcher.zonenews.utils.network.exception.GenericResponse
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

import com.searcher.zonenews.utils.ImageCacheManager
import java.util.concurrent.ConcurrentHashMap

/**
 * @Description 详情 repository
 * @Author yulu
 * @CreateTime 2025年07月04日 17:24:04
 */

@Singleton
class NewsDetailRepository @Inject constructor(private val appHttpService: AppHttpService) {
    
    // Cache for publisher info to persist across different articles/activities during the app session
    private val publisherInfoCache = ConcurrentHashMap<Int, PublisherInfoEntry>()
    
    // Cache for article detail info to make re-visiting articles instant (similar to publisher info)
    // Use composite key "id_lang" to support localized version caching
    private val articleDetailCache = ConcurrentHashMap<String, ArticleDetailEntry>()

    fun getCachedArticleDetail(id: String, language: String? = null): ArticleDetailEntry? {
        val cacheKey = if (language != null) "${id}_${language}" else id
        return articleDetailCache[cacheKey]
    }

    suspend fun queryNewsDetail(id: String, language: String? = null): GenericResponse<ArticleDetailEntry> {
        val response = appHttpService.queryArticleDetail(id, language)
        
        if (response is com.searcher.zonenews.utils.network.exception.NetworkResponse.Success) {
            val body = response.body
            if (body.code == 200) {
                val cacheKey = if (language != null) "${id}_${language}" else id
                articleDetailCache[cacheKey] = body
            }
        }
        
        return response
    }

    // Image Caching Helpers for "instant" feel - using global ImageCacheManager
    fun cacheBitmap(url: String, bitmap: android.graphics.Bitmap) {
        ImageCacheManager.put(url, bitmap)
    }

    fun getCachedBitmap(url: String): android.graphics.Bitmap? {
        return ImageCacheManager.get(url)
    }

    suspend fun addFeedBack(id: String, content: String): GenericResponse<CommonResponseEntry> {
        val jsonObject = JSONObject()
        jsonObject.put("content", content)
        return appHttpService.addFeedBack(id, Utils.createJsonRequestBody(jsonObject.toString()))
    }

    suspend fun saveHis(id: String): GenericResponse<CommonResponseEntry> {
        val jsonObject = JSONObject()
        jsonObject.put("article_id", id)
        return appHttpService.saveNewsHis(Utils.createJsonRequestBody(jsonObject.toString()))
    }

    suspend fun collectHis(id: String): GenericResponse<CommonResponseEntry> {
        return appHttpService.collectNews(id)
    }

    suspend fun deleteCollect(id: String): GenericResponse<CommonResponseEntry> {
        return appHttpService.deleteCollect(id)
    }

    fun getCachedPublisherInfo(id: Int): PublisherInfoEntry? {
        return publisherInfoCache[id]
    }

    suspend fun queryPublisherInfo(id: Int, language: String? = null): GenericResponse<PublisherInfoEntry> {
        val response = appHttpService.getPublisherInfo(id, language)
        
        // If successful, update the cache
        if (response is com.searcher.zonenews.utils.network.exception.NetworkResponse.Success) {
            val body = response.body
            if (body.code == 200) {
                publisherInfoCache[id] = body
            }
        }
        
        return response
    }
}
