package com.anssy.znewspro.repository

import com.anssy.znewspro.entry.CommonResponseEntry
import com.anssy.znewspro.entry.LoginEntry
import com.anssy.znewspro.net.AppHttpService
import com.anssy.znewspro.utils.Utils
import com.anssy.znewspro.utils.network.exception.GenericResponse
import org.json.JSONObject
import javax.inject.Inject

/**
 * @Description TODO
 * @Author yulu
 * @CreateTime 2025年07月04日 10:55:09
 */

class LoginRepository @Inject constructor(private val appHttpService: AppHttpService) {
    suspend fun loginApp(name:String,pass:String):GenericResponse<LoginEntry>{
        val jsonObject = JSONObject()
        jsonObject.put("username",name)
        jsonObject.put("password",pass)
       return appHttpService.loginApp(Utils.createJsonRequestBody(jsonObject.toString()))
    }

    suspend fun outLoginApp():GenericResponse<CommonResponseEntry>{
        return  appHttpService.logoutApp()
    }

    suspend fun registerApp(email:String,userName:String,passWord:String):GenericResponse<CommonResponseEntry>{
        val jsonObject = JSONObject()
        jsonObject.put("email",email)
        jsonObject.put("username",userName)
        jsonObject.put("password",passWord)
        return appHttpService.registerApp(Utils.createJsonRequestBody(jsonObject.toString()))
    }
}