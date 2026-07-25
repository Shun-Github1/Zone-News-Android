package com.searcher.zonenews.repository

import com.searcher.zonenews.entry.HomeDataListEntry
import com.searcher.zonenews.net.AppHttpService
import com.searcher.zonenews.utils.Utils
import org.json.JSONObject
import javax.inject.Inject

/**
 * @Description 主页仓库
 * @Author yulu
 * @CreateTime 2025年07月04日 14:40:32
 */

class HomeRepository @Inject constructor(private val appHttpService: AppHttpService) {
    suspend fun queryHomeData(
        tag: String? = null, 
        pageNo: Int? = null, 
        pageSize: Int? = null, 
        language: String? = null
    ): HomeDataListEntry {
        // Calculate zero-based offset: first page (1) -> offset 0, second page (2) -> offset 10, etc.
        val offset = if (pageNo != null && pageSize != null) {
            (pageNo - 1) * pageSize
        } else {
            null
        }
        return appHttpService.getHomeData(tag, offset, pageSize, language)
    }
    
    suspend fun queryFeedByTopic(
        topic: String,
        pageNo: Int? = null,
        pageSize: Int? = null,
        language: String? = null
    ): HomeDataListEntry {
        val offset = if (pageNo != null && pageSize != null) {
            (pageNo - 1) * pageSize
        } else {
            null
        }
        return appHttpService.getFeedByTopic(topic, offset, pageSize, language)
    }

    suspend fun getLevityFeed(
        language: String? = null,
        pageNo: Int? = null,
        pageSize: Int? = null
    ): HomeDataListEntry {
        // Calculate zero-based offset
        val offset = if (pageNo != null && pageSize != null) {
            (pageNo - 1) * pageSize
        } else {
            null
        }
        return appHttpService.getLevityFeed(language, offset, pageSize)
    }
}
