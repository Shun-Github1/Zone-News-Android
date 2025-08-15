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

    suspend fun queryMyTopic():GenericResponse<TopicListEntry>{
        return  appHttpService.queryMyTopics()
    }
    suspend fun queryAllTopic():GenericResponse<TopicListEntry>{
        return  appHttpService.queryAllTopics()
    }

    suspend fun editTopic(type:String,topic:String):GenericResponse<CommonResponseEntry>{
        return appHttpService.editTopic(type,topic)
    }
}