package com.searcher.zonenews.model

import android.view.View
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.searcher.zonenews.entry.CommonResponseEntry
import com.searcher.zonenews.entry.MyFormationEntry
import com.searcher.zonenews.entry.ViewHisEntry
import com.searcher.zonenews.repository.MyRepository
import com.searcher.zonenews.utils.LanguageManager
import com.searcher.zonenews.utils.network.exception.NetworkResponse
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject
import razerdp.util.PopupUtils.getString
import com.searcher.zonenews.R
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import com.searcher.zonenews.utils.ErrorUtils

/**
 * @Description 个人
 * @Author yulu
 * @CreateTime 2025年07月07日 14:51:34
 */
@HiltViewModel
class MyModel @Inject constructor(
    private val myRepository: MyRepository,
    private val languageManager: LanguageManager
) : ViewModel() {
    private var _myEntry: MutableLiveData<MyFormationEntry> = MutableLiveData()
    var myEntry: LiveData<MyFormationEntry> = _myEntry

    private var _myViewHisEntry: MutableLiveData<ViewHisEntry> = MutableLiveData()
    var myViewHisEntry: LiveData<ViewHisEntry> = _myViewHisEntry

    private var _myCollectEntry: MutableLiveData<ViewHisEntry> = MutableLiveData()
    var myCollectEntry: LiveData<ViewHisEntry> = _myCollectEntry

    private var _commonResponseEntry: MutableLiveData<CommonResponseEntry> = MutableLiveData()
    var commonResponseEntry: LiveData<CommonResponseEntry> = _commonResponseEntry

    fun queryMyFormation() {
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) {
                myRepository.queryMyFormation()
            }
            when (result) {
                is NetworkResponse.NetError -> {
                    val myFormationEntry = MyFormationEntry()
                    myFormationEntry.code = 1000
                    myFormationEntry.msg = ErrorUtils.getErrorMessage(result.errorMsg)
                    _myEntry.value = myFormationEntry
                }

                is NetworkResponse.Success -> {
                    _myEntry.value = result.body
                }

                is NetworkResponse.UnknownError -> {
                    val myFormationEntry = MyFormationEntry()
                    myFormationEntry.code = 1000
                    myFormationEntry.msg = "未知错误"
                    _myEntry.value = myFormationEntry
                }
            }
        }
    }

    fun queryMyViewHis() {
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) {
                myRepository.queryMyViewHist(languageManager.getCurrentLanguageCode())
            }
            when (result) {
                is NetworkResponse.NetError -> {
                    val myFormationEntry = ViewHisEntry()
                    myFormationEntry.code = 1000
                    myFormationEntry.msg = ErrorUtils.getErrorMessage(result.errorMsg)
                    _myViewHisEntry.value = myFormationEntry
                }

                is NetworkResponse.Success -> {
                    val responseBody = result.body
                    // Check if the response body has a code field indicating success/error
                    if (responseBody.code == 200) {
                        _myViewHisEntry.value = responseBody
                    } else {
                        // API returned error in response body
                        val errorEntry = ViewHisEntry()
                        errorEntry.code = responseBody.code
                        errorEntry.msg = responseBody.msg ?: "Failed to load reading history"
                        _myViewHisEntry.value = errorEntry
                    }
                }

                is NetworkResponse.UnknownError -> {
                    val myFormationEntry = ViewHisEntry()
                    myFormationEntry.code = 1000
                    myFormationEntry.msg = "未知错误"
                    _myViewHisEntry.value = myFormationEntry
                }
            }
        }
    }

    fun queryMyCollect() {
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) {
                myRepository.queryMyCollect(languageManager.getCurrentLanguageCode())
            }
            when (result) {
                is NetworkResponse.NetError -> {
                    val myFormationEntry = ViewHisEntry()
                    myFormationEntry.code = 1000
                    myFormationEntry.msg = ErrorUtils.getErrorMessage(result.errorMsg)
                    _myCollectEntry.value = myFormationEntry
                }

                is NetworkResponse.Success -> {
                    val responseBody = result.body
                    // Check if the response body has a code field indicating success/error
                    if (responseBody.code == 200) {
                        _myCollectEntry.value = responseBody
                    } else {
                        // API returned error in response body
                        val errorEntry = ViewHisEntry()
                        errorEntry.code = responseBody.code
                        errorEntry.msg = responseBody.msg ?: "Failed to load saved articles"
                        _myCollectEntry.value = errorEntry
                    }
                }

                is NetworkResponse.UnknownError -> {
                    val myFormationEntry = ViewHisEntry()
                    myFormationEntry.code = 1000
                    myFormationEntry.msg = "未知错误"
                    _myCollectEntry.value = myFormationEntry
                }
            }
        }
    }

    fun deleteHistory(articleId: String) {
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) {
                myRepository.deleteHistory(articleId)
            }
            when (result) {
                is NetworkResponse.NetError -> {
                    val commonResponseEntry = CommonResponseEntry()
                    commonResponseEntry.code = 1000
                    commonResponseEntry.msg = ErrorUtils.getErrorMessage(result.errorMsg)
                    _commonResponseEntry.value = commonResponseEntry
                }

                is NetworkResponse.Success -> {
                    _commonResponseEntry.value = result.body
                }

                is NetworkResponse.UnknownError -> {
                    val commonResponseEntry = CommonResponseEntry()
                    commonResponseEntry.code = 1000
                    commonResponseEntry.msg = "未知错误"
                    _commonResponseEntry.value = commonResponseEntry
                }
            }
        }
    }

    fun deleteCollect(articleId: String) {
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) {
                myRepository.deleteCollect(articleId)
            }
            when (result) {
                is NetworkResponse.NetError -> {
                    val commonResponseEntry = CommonResponseEntry()
                    commonResponseEntry.code = 1000
                    commonResponseEntry.msg = ErrorUtils.getErrorMessage(result.errorMsg)
                    _commonResponseEntry.value = commonResponseEntry
                }

                is NetworkResponse.Success -> {
                    _commonResponseEntry.value = result.body
                }

                is NetworkResponse.UnknownError -> {
                    val commonResponseEntry = CommonResponseEntry()
                    commonResponseEntry.code = 1000
                    commonResponseEntry.msg = "未知错误"
                    _commonResponseEntry.value = commonResponseEntry
                }
            }
        }
    }

    fun redeemCode(code: String) {
        viewModelScope.launch {
            val mediaType = "application/json; charset=utf-8".toMediaType()
            val requestBody = "{\"code\":\"$code\"}".toRequestBody(mediaType)
            val result = withContext(Dispatchers.IO) {
                myRepository.redeemCode(requestBody)
            }
            when (result) {
                is NetworkResponse.NetError -> {
                    val commonResponseEntry = CommonResponseEntry()
                    commonResponseEntry.code = 1000
                    commonResponseEntry.msg = ErrorUtils.getErrorMessage(result.errorMsg)
                    _commonResponseEntry.value = commonResponseEntry
                }

                is NetworkResponse.Success -> {
                    _commonResponseEntry.value = result.body
                    // Refresh profile after successful redeem
                    queryMyFormation()
                }

                is NetworkResponse.UnknownError -> {
                    val commonResponseEntry = CommonResponseEntry()
                    commonResponseEntry.code = 1000
                    commonResponseEntry.msg = "未知错误"
                    _commonResponseEntry.value = commonResponseEntry
                }
            }
        }
    }

    fun cancelSubscription() {
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) {
                myRepository.cancelSubscription()
            }
            when (result) {
                is NetworkResponse.NetError -> {
                    val commonResponseEntry = CommonResponseEntry()
                    commonResponseEntry.code = 1000
                    commonResponseEntry.msg = ErrorUtils.getErrorMessage(result.errorMsg)
                    _commonResponseEntry.value = commonResponseEntry
                }

                is NetworkResponse.Success -> {
                    _commonResponseEntry.value = result.body
                    // Refresh profile after successful cancel
                    queryMyFormation()
                }

                is NetworkResponse.UnknownError -> {
                    val commonResponseEntry = CommonResponseEntry()
                    commonResponseEntry.code = 1000
                    commonResponseEntry.msg = "未知错误"
                    _commonResponseEntry.value = commonResponseEntry
                }
            }
        }
    }

    fun deleteAccount(requestBody: okhttp3.RequestBody) {
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) {
                myRepository.deleteAccount(requestBody)
            }
            when (result) {
                is NetworkResponse.NetError -> {
                    val commonResponseEntry = CommonResponseEntry()
                    commonResponseEntry.code = 1000
                    commonResponseEntry.msg = ErrorUtils.getErrorMessage(result.errorMsg)
                    _commonResponseEntry.value = commonResponseEntry
                }

                is NetworkResponse.Success -> {
                    _commonResponseEntry.value = result.body
                }

                is NetworkResponse.UnknownError -> {
                    val commonResponseEntry = CommonResponseEntry()
                    commonResponseEntry.code = 1000
                    commonResponseEntry.msg = "未知错误"
                    _commonResponseEntry.value = commonResponseEntry
                }
            }
        }
    }
}
