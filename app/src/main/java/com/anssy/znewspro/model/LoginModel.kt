package com.anssy.znewspro.model

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import razerdp.util.PopupUtils.getString
import com.anssy.znewspro.R
import com.anssy.znewspro.entry.CommonResponseEntry
import com.anssy.znewspro.entry.LoginEntry
import com.anssy.znewspro.repository.LoginRepository
import com.anssy.znewspro.utils.network.exception.NetworkResponse
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

/**
 * @Description TODO
 * @Author yulu
 * @CreateTime 2025年07月04日 10:54:19
 */
@HiltViewModel
class LoginModel @Inject constructor(private val loginRepository: LoginRepository):ViewModel() {

    private var _loginEntry: MutableLiveData<LoginEntry> = MutableLiveData<LoginEntry>()
    var loginEntry: LiveData<LoginEntry> = _loginEntry

    private var _outLoginEntry:MutableLiveData<CommonResponseEntry> = MutableLiveData<CommonResponseEntry>()
    var outLoginEntry:LiveData<CommonResponseEntry> = _outLoginEntry

    fun loginApp(name: String,pass:String){
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) {
                loginRepository.loginApp(name,pass)// 一个耗时的异步操作
            }
            when(result){
                is NetworkResponse.NetError->{
                    val loginEntry = LoginEntry()
                    loginEntry.code = 1000
                    loginEntry.msg = getString(R.string.server_error_message)
                    _loginEntry.value = loginEntry
                }
                is NetworkResponse.Success->{
                    _loginEntry.value = result.body
                }
                is NetworkResponse.UnknownError->{
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
                    loginEntry.code = 1000
                    loginEntry.msg = getString(R.string.server_error_message)
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
                    loginEntry.code = 1000
                    loginEntry.msg = getString(R.string.server_error_message)
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
}