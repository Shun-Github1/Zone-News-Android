package com.searcher.zonenews.repository

import com.searcher.zonenews.entry.ArticleDetailEntry
import com.searcher.zonenews.entry.CommonResponseEntry
import com.searcher.zonenews.entry.PublisherInfoEntry
import com.searcher.zonenews.net.AppHttpService
import com.searcher.zonenews.utils.Utils
import com.searcher.zonenews.utils.network.exception.GenericResponse
import org.json.JSONObject
import javax.inject.Inject

/**
 * @Description 详情 repository
 * @Author yulu
 * @CreateTime 2025年07月04日 17:24:04
 */

class NewsDetailRepository @Inject constructor(private val appHttpService: AppHttpService) {
    suspend fun queryNewsDetail(id: String, language: String? = null): GenericResponse<ArticleDetailEntry> {
        return appHttpService.queryArticleDetail(id, language)
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

    suspend fun queryPublisherInfo(id: Int, language: String? = null): GenericResponse<PublisherInfoEntry> {
        return appHttpService.getPublisherInfo(id, language)
    }
}
