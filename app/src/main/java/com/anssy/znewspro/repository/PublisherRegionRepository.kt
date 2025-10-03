package com.anssy.znewspro.repository

import com.anssy.znewspro.entry.CommonResponseEntry
import com.anssy.znewspro.entry.PublisherRegionEntry
import com.anssy.znewspro.net.AppHttpService
import com.anssy.znewspro.utils.network.exception.GenericResponse
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
