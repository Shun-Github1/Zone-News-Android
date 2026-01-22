package com.searcher.zonenews.model

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import razerdp.util.PopupUtils.getString
import com.searcher.zonenews.R
import com.searcher.zonenews.entry.CommonResponseEntry
import com.searcher.zonenews.entry.LoginEntry
import com.searcher.zonenews.repository.LoginRepository
import com.searcher.zonenews.utils.network.exception.NetworkResponse
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject
import com.searcher.zonenews.utils.ErrorUtils

/**
 * @Description TODO
 * @Author yulu
 * @CreateTime 2025年07月04日 10:54:19
 */
@HiltViewModel
class LoginModel @Inject constructor(private val loginRepository: LoginRepository):ViewModel() {

    private val TAG = "LoginModel"

    private var _loginEntry: MutableLiveData<LoginEntry> = MutableLiveData<LoginEntry>()
    var loginEntry: LiveData<LoginEntry> = _loginEntry

    private var _outLoginEntry:MutableLiveData<CommonResponseEntry> = MutableLiveData<CommonResponseEntry>()
    var outLoginEntry:LiveData<CommonResponseEntry> = _outLoginEntry

    fun loginApp(name: String,pass:String){
        Log.d(TAG, "Attempting login for user: $name")
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) {
                loginRepository.loginApp(name,pass)// 一个耗时的异步操作
            }
            when(result){
                is NetworkResponse.NetError->{
                    Log.e(TAG, "Login network error - HTTP Code: ${result.httpCode}, Error: ${result.errorMsg}")
                    Log.e(TAG, "Full error details: $result")
                    val loginEntry = LoginEntry()
                    loginEntry.code = result.httpCode ?: 1000
                    loginEntry.msg = ErrorUtils.getErrorMessage(result.errorMsg)
                    _loginEntry.value = loginEntry
                }
                is NetworkResponse.Success->{
                    Log.d(TAG, "Login successful for user: $name")
                    _loginEntry.value = result.body
                }
                is NetworkResponse.UnknownError->{
                    Log.e(TAG, "Login unknown error: ${result.error?.message}")
                    val loginEntry = LoginEntry()
                    loginEntry.code = 1000
                    loginEntry.msg = "未知错误"
                    _loginEntry.value = loginEntry
                }
            }
        }
    }

    fun outLoginApp(){
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO){
                loginRepository.outLoginApp()
            }
            when(result){
                is NetworkResponse.NetError->{
                    val loginEntry = CommonResponseEntry()
                    loginEntry.code = result.httpCode ?: 1000
                    loginEntry.msg = ErrorUtils.getErrorMessage(result.errorMsg)
                    _outLoginEntry.value = loginEntry
                }
                is NetworkResponse.Success->{
                    _outLoginEntry.value = result.body
                }
                is NetworkResponse.UnknownError->{
                    val loginEntry = CommonResponseEntry()
                    loginEntry.code = 1000
                    loginEntry.msg = "未知错误"
                    _outLoginEntry.value = loginEntry
                }
            }
        }
    }

    fun registerApp(email:String,userName:String,passWord:String){
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO){
                loginRepository.registerApp(email,userName,passWord)
            }
            when(result){
                is NetworkResponse.NetError->{
                    val loginEntry = CommonResponseEntry()
                    loginEntry.code = result.httpCode ?: 1000
                    loginEntry.msg = ErrorUtils.getErrorMessage(result.errorMsg)
                    _outLoginEntry.value = loginEntry
                }
                is NetworkResponse.Success->{
                    _outLoginEntry.value = result.body
                }
                is NetworkResponse.UnknownError->{
                    val loginEntry = CommonResponseEntry()
                    loginEntry.code = 1000
                    loginEntry.msg = "未知错误"
                    _outLoginEntry.value = loginEntry
                }
            }
        }
    }

    fun loginWithFirebase(idToken: String) {
        Log.d(TAG, "Attempting Firebase login")
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) {
                loginRepository.loginWithFirebase(idToken)
            }
            when(result){
                is NetworkResponse.NetError->{
                    Log.e(TAG, "Firebase login network error - HTTP Code: ${result.httpCode}, Error: ${result.errorMsg}")
                    val loginEntry = LoginEntry()
                    loginEntry.code = result.httpCode ?: 1000
                    loginEntry.msg = ErrorUtils.getErrorMessage(result.errorMsg)
                    _loginEntry.value = loginEntry
                }
                is NetworkResponse.Success->{
                    Log.d(TAG, "Firebase login successful")
                    _loginEntry.value = result.body
                }
                is NetworkResponse.UnknownError->{
                    Log.e(TAG, "Firebase login unknown error: ${result.error?.message}")
                    val loginEntry = LoginEntry()
                    loginEntry.code = 1000
                    loginEntry.msg = "未知错误"
                    _loginEntry.value = loginEntry
                }
            }
        }
    }
}
