package com.searcher.zonenews.repository

import com.searcher.zonenews.entry.HomeDataListEntry
import com.searcher.zonenews.entry.PersonRecommendListEntry
import com.searcher.zonenews.entry.SearchListEntry
import com.searcher.zonenews.net.AppHttpService
import com.searcher.zonenews.utils.network.exception.GenericResponse
import javax.inject.Inject

/**
 * @Description TODO
 * @Author yulu
 * @CreateTime 2025年07月07日 10:17:04
 */

class PersonRecommendRepository @Inject constructor(private val appHttpService: AppHttpService) {

    suspend fun queryRecommendList(
        pageNo: Int, 
        pageSize: Int, 
        language: String? = null,
        sortBy: String? = null
    ): GenericResponse<SearchListEntry> {
        // Calculate zero-based offset: first page (1) -> offset 0, second page (2) -> offset 10, etc.
        val offset = (pageNo - 1) * pageSize
        return appHttpService.queryPersonRecommend(offset, pageSize, language, sortBy)
    }
}
