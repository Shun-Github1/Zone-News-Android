package com.anssy.znewspro.repository

import com.anssy.znewspro.entry.CommonResponseEntry
import com.anssy.znewspro.entry.MyFormationEntry
import com.anssy.znewspro.entry.ViewHisEntry
import com.anssy.znewspro.net.AppHttpService
import com.anssy.znewspro.utils.network.exception.GenericResponse
import javax.inject.Inject

/**
 * @Description TODO
 * @Author yulu
 * @CreateTime 2025年07月07日 14:49:42
 */

class MyRepository @Inject constructor(private val appHttpService: AppHttpService) {
    suspend fun queryMyFormation(): GenericResponse<MyFormationEntry> {
        return appHttpService.queryMyFormation()
    }

    suspend fun queryMyViewHist(language: String? = null): GenericResponse<ViewHisEntry> {
        return appHttpService.queryViewHis(language)
    }

    suspend fun queryMyCollect(language: String? = null): GenericResponse<ViewHisEntry> {
        return appHttpService.queryMyCollect(language)
    }

    suspend fun deleteHistory(articleId: String): GenericResponse<CommonResponseEntry> {
        return appHttpService.deleteHistory(articleId)
    }

    suspend fun deleteCollect(articleId: String): GenericResponse<CommonResponseEntry> {
        return appHttpService.deleteCollect(articleId)
    }
}