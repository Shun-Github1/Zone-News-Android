package com.anssy.znewspro.repository

import com.anssy.znewspro.entry.SearchListEntry
import com.anssy.znewspro.net.AppHttpService
import com.anssy.znewspro.utils.network.exception.GenericResponse
import javax.inject.Inject

/**
 * @Description TODO
 * @Author yulu
 * @CreateTime 2025年07月04日 16:26:21
 */

class SearchRepository @Inject constructor(private val appHttpService: AppHttpService) {
    suspend fun querySearchList():GenericResponse<SearchListEntry>{
        return  appHttpService.querySearchList()
    }

    suspend fun queryNewsByTitle(title:String):GenericResponse<SearchListEntry>{
        return appHttpService.searchNewsByTitle(title)
    }
}