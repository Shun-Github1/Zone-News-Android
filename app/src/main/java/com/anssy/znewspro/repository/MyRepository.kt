package com.anssy.znewspro.repository

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
   suspend fun queryMyFormation():GenericResponse<MyFormationEntry>{
       return appHttpService.queryMyFormation()
    }

    suspend fun queryMyViewHist():GenericResponse<ViewHisEntry>{
        return appHttpService.queryViewHis()
    }

    suspend fun queryMyCollect():GenericResponse<ViewHisEntry>{

        return  appHttpService.queryMyCollect()

    }

    
}