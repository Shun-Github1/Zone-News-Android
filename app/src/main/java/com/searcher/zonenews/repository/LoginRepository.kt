package com.searcher.zonenews.repository

import android.util.Log
import com.searcher.zonenews.entry.CommonResponseEntry
import com.searcher.zonenews.entry.LoginEntry
import com.searcher.zonenews.net.AppHttpService
import com.searcher.zonenews.utils.Utils
import com.searcher.zonenews.utils.network.PersistentCookieJar
import com.searcher.zonenews.utils.network.exception.GenericResponse
import org.json.JSONObject
import javax.inject.Inject

/**
 * @Description TODO
 * @Author yulu
 * @CreateTime 2025年07月04日 10:55:09
 */

class LoginRepository @Inject constructor(
    private val appHttpService: AppHttpService,
    private val cookieJar: PersistentCookieJar
) {
    private val TAG = "LoginRepository"
    
    suspend fun loginApp(name:String,pass:String):GenericResponse<LoginEntry>{
        val jsonObject = JSONObject()
        jsonObject.put("username",name)
        jsonObject.put("password",pass)
        val requestBody = Utils.createJsonRequestBody(jsonObject.toString())
        Log.d(TAG, "Making login API call with username: $name")
        Log.d(TAG, "Request body: $jsonObject")
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
        val result = appHttpService.logoutApp()
        // Clear cookies on logout
        cookieJar.clearCookies()
        return result
    }

    suspend fun registerApp(email:String,userName:String,passWord:String):GenericResponse<CommonResponseEntry>{
        val jsonObject = JSONObject()
        jsonObject.put("email",email)
        jsonObject.put("username",userName)
        jsonObject.put("password",passWord)
        return appHttpService.registerApp(Utils.createJsonRequestBody(jsonObject.toString()))
    }

    suspend fun loginWithFirebase(idToken: String): GenericResponse<LoginEntry> {
        val jsonObject = JSONObject()
        jsonObject.put("idToken", idToken)
        val requestBody = Utils.createJsonRequestBody(jsonObject.toString())
        Log.d(TAG, "Making Firebase login API call")
        Log.d(TAG, "Request body: $jsonObject")
        try {
            val result = appHttpService.loginWithFirebase(requestBody)
            Log.d(TAG, "Firebase API call completed. Result type: ${result::class.simpleName}")
            return result
        } catch (e: Exception) {
            Log.e(TAG, "Exception during Firebase API call: ${e.message}", e)
            throw e
        }
    }
}
