package com.anssy.znewspro.repository

import com.anssy.znewspro.entry.HomeDataListEntry
import com.anssy.znewspro.net.AppHttpService
import com.anssy.znewspro.utils.Utils
import org.json.JSONObject
import javax.inject.Inject

/**
 * @Description 主页仓库
 * @Author yulu
 * @CreateTime 2025年07月04日 14:40:32
 */

class HomeRepository @Inject constructor(private val appHttpService: AppHttpService) {
    suspend fun queryHomeData(
        tag: String? = null, 
        pageNo: Int? = null, 
        pageSize: Int? = null, 
        language: String? = null
    ): HomeDataListEntry {
        return appHttpService.getHomeData(tag, pageNo, pageSize, language)
    }
}