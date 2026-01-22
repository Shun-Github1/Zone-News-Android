package com.searcher.zonenews.repository

import com.searcher.zonenews.entry.SearchListEntry
import com.searcher.zonenews.net.AppHttpService
import com.searcher.zonenews.utils.network.exception.GenericResponse
import javax.inject.Inject

/**
 * @Description TODO
 * @Author yulu
 * @CreateTime 2025年07月04日 16:26:21
 */

class SearchRepository @Inject constructor(private val appHttpService: AppHttpService) {
    suspend fun querySearchList(language: String? = null): GenericResponse<SearchListEntry> {
        return appHttpService.querySearchList(language)
    }

    suspend fun queryNewsByTitle(
        title: String,
        language: String? = null,
        page: Int? = null,
        limit: Int? = null,
        sortBy: String? = null
    ): GenericResponse<SearchListEntry> {
        return appHttpService.searchNewsByTitle(title, language, page, limit, sortBy)
    }
}
