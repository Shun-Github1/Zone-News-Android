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
    suspend fun queryHomeData(tag:String,pageNo:Int,pageSize:Int):HomeDataListEntry{
        val jsonObject = JSONObject()
        jsonObject.put("tag",tag)
        jsonObject.put("offset",pageNo)
        jsonObject.put("limit",pageSize)

        return  appHttpService.getHomeData(Utils.createJsonRequestBody(jsonObject.toString()))
    }
}