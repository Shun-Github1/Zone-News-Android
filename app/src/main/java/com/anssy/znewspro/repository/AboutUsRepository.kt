package com.anssy.znewspro.repository

import com.anssy.znewspro.entry.AboutUsEntry
import com.anssy.znewspro.net.AppHttpService
import com.anssy.znewspro.utils.network.exception.GenericResponse
import javax.inject.Inject

/**
 * About Us Repository
 * @Author yulu
 * @CreateTime 2025年01月15日 10:00:00
 */

class AboutUsRepository @Inject constructor(private val appHttpService: AppHttpService) {

    suspend fun getAboutUs(language: String? = null): GenericResponse<AboutUsEntry> {
        return appHttpService.getAboutUs(language)
    }
}
