package com.searcher.zonenews.repository

import com.searcher.zonenews.entry.CommonResponseEntry
import com.searcher.zonenews.entry.PublisherRegionEntry
import com.searcher.zonenews.net.AppHttpService
import com.searcher.zonenews.utils.network.exception.GenericResponse
import javax.inject.Inject

/**
 * Publisher Region Repository
 * @Author yulu
 * @CreateTime 2025年01月15日 10:00:00
 */

class PublisherRegionRepository @Inject constructor(private val appHttpService: AppHttpService) {

    suspend fun getPublisherRegions(language: String? = null): GenericResponse<PublisherRegionEntry> {
        return appHttpService.getPublisherRegions(language)
    }

    suspend fun editPublisherRegion(action: String, tag: String, language: String? = null): GenericResponse<CommonResponseEntry> {
        return appHttpService.editPublisherRegion(action, tag, language)
    }
}
