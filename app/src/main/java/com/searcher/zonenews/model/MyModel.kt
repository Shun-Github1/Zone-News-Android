package com.searcher.zonenews.model

import android.app.Activity
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.searcher.zonenews.billing.BillingManager
import com.searcher.zonenews.entry.*
import com.searcher.zonenews.utils.LanguageManager
import com.searcher.zonenews.repository.MyRepository
import com.searcher.zonenews.utils.ErrorUtils
import com.searcher.zonenews.utils.network.exception.NetworkResponse
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import javax.inject.Inject

@HiltViewModel
class MyModel @Inject constructor(
    private val myRepository: MyRepository,
    private val languageManager: LanguageManager,
    private val billingManager: BillingManager
) : ViewModel() {
    private var _myEntry: MutableLiveData<MyFormationEntry> = MutableLiveData()
    var myEntry: LiveData<MyFormationEntry> = _myEntry

    private var _myViewHisEntry: MutableLiveData<ViewHisEntry> = MutableLiveData()
    var myViewHisEntry: LiveData<ViewHisEntry> = _myViewHisEntry

    private var _myCollectEntry: MutableLiveData<ViewHisEntry> = MutableLiveData()
    var myCollectEntry: LiveData<ViewHisEntry> = _myCollectEntry

    private var _commonResponseEntry: MutableLiveData<CommonResponseEntry> = MutableLiveData()
    var commonResponseEntry: LiveData<CommonResponseEntry> = _commonResponseEntry

    // Billing related LiveData
    private var _productDetails = MutableLiveData<List<com.android.billingclient.api.ProductDetails>>()
    val productDetails: LiveData<List<com.android.billingclient.api.ProductDetails>> = _productDetails

    private var _purchaseState = MutableLiveData<BillingManager.PurchaseState>()
    val purchaseState: LiveData<BillingManager.PurchaseState> = _purchaseState

    init {
        // Observe flows from BillingManager
        viewModelScope.launch {
            billingManager.productDetails.collectLatest {
                _productDetails.postValue(it)
            }
        }
        viewModelScope.launch {
            billingManager.purchaseState.collectLatest {
                _purchaseState.postValue(it)
            }
        }
    }
    
    // Billing methods
    fun launchPurchaseFlow(activity: Activity, productDetails: com.android.billingclient.api.ProductDetails, offerToken: String) {
        billingManager.launchPurchaseFlow(activity, productDetails, offerToken)
    }

    fun restorePurchases() {
        billingManager.restorePurchases()
    }

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

    fun saveNews(requestBody: RequestBody) {
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) {
                myRepository.saveNews(requestBody)
            }
            when (result) {
                is NetworkResponse.NetError -> {
                    val commonResponseEntry = CommonResponseEntry()
                    commonResponseEntry.code = 1000
                    commonResponseEntry.msg = ErrorUtils.getErrorMessage(result.errorMsg)
                    _commonResponseEntry.value = commonResponseEntry
                }

                is NetworkResponse.Success -> {
                     val responseBody = result.body
                     if (responseBody.code == 200) {
                        _commonResponseEntry.value = responseBody
                    } else {
                        val errorEntry = CommonResponseEntry()
                        errorEntry.code = responseBody.code
                        errorEntry.msg = responseBody.msg ?: "Failed to save"
                        _commonResponseEntry.value = errorEntry
                    }
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

    fun deleteCollect(id: String) {
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) {
                myRepository.deleteCollect(id)
            }
            when (result) {
                is NetworkResponse.NetError -> {
                    val commonResponseEntry = CommonResponseEntry()
                    commonResponseEntry.code = 1000
                    commonResponseEntry.msg = ErrorUtils.getErrorMessage(result.errorMsg)
                    _commonResponseEntry.value = commonResponseEntry
                }

                is NetworkResponse.Success -> {
                     val responseBody = result.body
                     if (responseBody.code == 200) {
                        _commonResponseEntry.value = responseBody
                    } else {
                        val errorEntry = CommonResponseEntry()
                        errorEntry.code = responseBody.code
                        errorEntry.msg = responseBody.msg ?: "Failed to delete"
                        _commonResponseEntry.value = errorEntry
                    }
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
    
    fun deleteHistory(id: String) {
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) {
                myRepository.deleteHistory(id)
            }
            when (result) {
                is NetworkResponse.NetError -> {
                    val commonResponseEntry = CommonResponseEntry()
                    commonResponseEntry.code = 1000
                    commonResponseEntry.msg = ErrorUtils.getErrorMessage(result.errorMsg)
                    _commonResponseEntry.value = commonResponseEntry
                }

                is NetworkResponse.Success -> {
                     val responseBody = result.body
                     if (responseBody.code == 200) {
                        _commonResponseEntry.value = responseBody
                    } else {
                        val errorEntry = CommonResponseEntry()
                        errorEntry.code = responseBody.code
                        errorEntry.msg = responseBody.msg ?: "Failed to delete"
                        _commonResponseEntry.value = errorEntry
                    }
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
                    // If successful, auto-refresh profile
                    if (result.body.code == 200) {
                        queryMyFormation()
                    }
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
                    // If successful, auto-refresh profile
                    if (result.body.code == 200) {
                        queryMyFormation()
                    }
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

    fun deleteAccount(requestBody: RequestBody) {
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
                     val responseBody = result.body
                     if (responseBody.code == 200) {
                        _commonResponseEntry.value = responseBody
                    } else {
                        val errorEntry = CommonResponseEntry()
                        errorEntry.code = responseBody.code
                        errorEntry.msg = responseBody.msg ?: "Failed to delete account"
                        _commonResponseEntry.value = errorEntry
                    }
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
