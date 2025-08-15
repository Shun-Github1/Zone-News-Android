package com.anssy.znewspro.repository

import com.anssy.znewspro.entry.HomeDataListEntry
import com.anssy.znewspro.entry.PersonRecommendListEntry
import com.anssy.znewspro.entry.SearchListEntry
import com.anssy.znewspro.net.AppHttpService
import com.anssy.znewspro.utils.network.exception.GenericResponse
import javax.inject.Inject

/**
 * @Description TODO
 * @Author yulu
 * @CreateTime 2025年07月07日 10:17:04
 */

class PersonRecommendRepository @Inject constructor(private val appHttpService: AppHttpService) {

    suspend fun queryRecommendList(pageNo:Int,pageSize:Int):GenericResponse<SearchListEntry>{
        return appHttpService.queryPersonRecommend(pageNo,pageSize)
    }
}