package com.searcher.zonenews.repository

import com.searcher.zonenews.entry.CommonResponseEntry
import com.searcher.zonenews.entry.MyFormationEntry
import com.searcher.zonenews.entry.ViewHisEntry
import com.searcher.zonenews.net.AppHttpService
import com.searcher.zonenews.utils.network.exception.GenericResponse
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

    suspend fun redeemCode(requestBody: okhttp3.RequestBody): GenericResponse<CommonResponseEntry> {
        return appHttpService.redeemCode(requestBody)
    }

    suspend fun cancelSubscription(): GenericResponse<CommonResponseEntry> {
        return appHttpService.cancelSubscription()
    }

    suspend fun saveNews(requestBody: okhttp3.RequestBody): GenericResponse<CommonResponseEntry> {
        return appHttpService.saveNewsHis(requestBody)
    }

    suspend fun deleteAccount(requestBody: okhttp3.RequestBody): GenericResponse<CommonResponseEntry> {
        return appHttpService.deleteAccount(requestBody)
    }
}
