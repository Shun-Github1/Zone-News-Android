package com.anssy.znewspro.repository

import android.util.Log
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
    private val TAG = "LoginRepository"
    
    suspend fun loginApp(name:String,pass:String):GenericResponse<LoginEntry>{
        val jsonObject = JSONObject()
        jsonObject.put("username",name)
        jsonObject.put("password",pass)
        val requestBody = Utils.createJsonRequestBody(jsonObject.toString())
        Log.d(TAG, "Making login API call with username: $name")
        Log.d(TAG, "Request body: ${jsonObject.toString()}")
        try {
            val result = appHttpService.loginApp(requestBody)
            Log.d(TAG, "API call completed. Result type: ${result::class.simpleName}")
            return result
        } catch (e: Exception) {
            Log.e(TAG, "Exception during API call: ${e.message}", e)
            throw e
        }
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