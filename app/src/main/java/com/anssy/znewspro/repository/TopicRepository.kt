package com.anssy.znewspro.repository

import com.anssy.znewspro.entry.CommonResponseEntry
import com.anssy.znewspro.entry.TopicListEntry
import com.anssy.znewspro.net.AppHttpService
import com.anssy.znewspro.utils.network.exception.GenericResponse
import javax.inject.Inject

/**
 * @Description TODO
 * @Author yulu
 * @CreateTime 2025年07月07日 11:23:03
 */

class TopicRepository @Inject constructor(private val appHttpService: AppHttpService) {

    suspend fun queryMyTopic(language: String? = null): GenericResponse<TopicListEntry> {
        return appHttpService.queryMyTopics(language)
    }
    
    suspend fun queryAllTopic(language: String? = null): GenericResponse<TopicListEntry> {
        return appHttpService.queryAllTopics(language)
    }

    suspend fun editTopic(type: String, topic: String, language: String? = null): GenericResponse<CommonResponseEntry> {
        return appHttpService.editTopic(type, topic, language)
    }

    suspend fun getTrendingTopics(language: String? = null, all: Boolean = false): GenericResponse<TopicListEntry> {
        return appHttpService.getTrendingTopics(language, all)
    }

    suspend fun getSectors(language: String? = null): GenericResponse<TopicListEntry> {
        return appHttpService.getSectors(language)
    }

    suspend fun getRegions(language: String? = null): GenericResponse<TopicListEntry> {
        return appHttpService.getRegions(language, "regions")
    }
    
    suspend fun getAllTopics(language: String? = null): GenericResponse<TopicListEntry> {
        return appHttpService.queryAllTopics(language)
    }
}