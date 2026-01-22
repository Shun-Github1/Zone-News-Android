package com.searcher.zonenews.model

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.searcher.zonenews.entry.ArticleDetailEntry
import com.searcher.zonenews.entry.CommonResponseEntry
import com.searcher.zonenews.repository.NewsDetailRepository
import com.searcher.zonenews.utils.LanguageManager
import com.searcher.zonenews.utils.network.exception.NetworkResponse
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject
import razerdp.util.PopupUtils.getString
import com.searcher.zonenews.entry.PublisherInfoEntry
import com.searcher.zonenews.R
import java.util.concurrent.ConcurrentHashMap
import com.searcher.zonenews.utils.ErrorUtils

/**
 * @Description 详情model
 * @Author yulu
 * @CreateTime 2025年07月04日 17:26:42
 */
@HiltViewModel
class NewsDetailModel @Inject constructor(
    private val newsDetailRepository: NewsDetailRepository,
    private val languageManager: LanguageManager
) : ViewModel() {
    private var _newsDetailEntry: MutableLiveData<ArticleDetailEntry> = MutableLiveData<ArticleDetailEntry>()
    var newsDetailEntry: LiveData<ArticleDetailEntry> = _newsDetailEntry

    private var _feedBackResponseEntry: MutableLiveData<CommonResponseEntry> = MutableLiveData()
    var feeBackResponseEntry: LiveData<CommonResponseEntry> = _feedBackResponseEntry

    private var _addHisEntry: MutableLiveData<CommonResponseEntry> = MutableLiveData()
    var addHisEntry: LiveData<CommonResponseEntry> = _addHisEntry

    private var _collectEntry: MutableLiveData<CommonResponseEntry> = MutableLiveData()
    var collectEntry: LiveData<CommonResponseEntry> = _collectEntry

    private var _deleteCollectEntry: MutableLiveData<CommonResponseEntry> = MutableLiveData()
    var deleteCollectEntry: LiveData<CommonResponseEntry> = _deleteCollectEntry

    private var _publisherInfoEntry: MutableLiveData<PublisherInfoEntry> = MutableLiveData()
    var publisherInfoEntry: LiveData<PublisherInfoEntry> = _publisherInfoEntry

    // Cache for publisher info to prevent redundant network calls
    private val publisherInfoCache = ConcurrentHashMap<Int, PublisherInfoEntry>()

    fun queryNewsDetail(id: String, summaryLanguage: String? = null) {
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) {
                // Use summaryLanguage if provided, otherwise fall back to app language
                val languageToUse = summaryLanguage ?: languageManager.getCurrentLanguageCode()
                newsDetailRepository.queryNewsDetail(id, languageToUse)
            }
            when (result) {
                is NetworkResponse.NetError -> {
                    val articleDetailEntry = ArticleDetailEntry()
                    articleDetailEntry.code = 1000
                    articleDetailEntry.msg = ErrorUtils.getErrorMessage(result.errorMsg)
                    _newsDetailEntry.value = articleDetailEntry
                }
                is NetworkResponse.Success -> {
                    val responseBody = result.body
                    // Check if the response body has a code field indicating success/error
                    if (responseBody.code == 200) {
                        _newsDetailEntry.value = responseBody
                    } else {
                        // API returned error in response body
                        _newsDetailEntry.value = responseBody
                    }
                }
                is NetworkResponse.UnknownError -> {
                    val articleDetailEntry = ArticleDetailEntry()
                    articleDetailEntry.code = 1000
                    articleDetailEntry.msg = result.error?.message ?: "Unknown error"
                    _newsDetailEntry.value = articleDetailEntry
                }
            }
        }
    }

    fun addFeedBack(id: String, content: String) {
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) {
                newsDetailRepository.addFeedBack(id, content)
            }
            when (result) {
                is NetworkResponse.NetError -> {
                    val commonResponseEntry = CommonResponseEntry()
                    commonResponseEntry.code = 1000
                    commonResponseEntry.msg = ErrorUtils.getErrorMessage(result.errorMsg)
                    _feedBackResponseEntry.value = commonResponseEntry
                }
                is NetworkResponse.Success -> {
                    _feedBackResponseEntry.value = result.body
                }
                is NetworkResponse.UnknownError -> {
                    val commonResponseEntry = CommonResponseEntry()
                    commonResponseEntry.code = 1000
                    commonResponseEntry.msg = "未知错误"
                    _feedBackResponseEntry.value = commonResponseEntry
                }
            }
        }
    }
    
    fun addNewsHis(id: String) {
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) {
                newsDetailRepository.saveHis(id)
            }
            when (result) {
                is NetworkResponse.NetError -> {
                    val commonResponseEntry = CommonResponseEntry()
                    commonResponseEntry.code = 1000
                    commonResponseEntry.msg = ErrorUtils.getErrorMessage(result.errorMsg)
                    _addHisEntry.value = commonResponseEntry
                }
                is NetworkResponse.Success -> {
                    val responseBody = result.body
                    // Check if the response body has a code field indicating success/error
                    if (responseBody.code == 200) {
                        _addHisEntry.value = responseBody
                    } else {
                        // API returned error in response body
                        _addHisEntry.value = responseBody
                    }
                }
                is NetworkResponse.UnknownError -> {
                    val commonResponseEntry = CommonResponseEntry()
                    commonResponseEntry.code = 1000
                    commonResponseEntry.msg = "未知错误"
                    _addHisEntry.value = commonResponseEntry
                }
            }
        }
    }

    /**
     * 收藏
     */
    fun collectHis(id: String) {
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) {
                newsDetailRepository.collectHis(id)
            }
            when (result) {
                is NetworkResponse.NetError -> {
                    val commonResponseEntry = CommonResponseEntry()
                    commonResponseEntry.code = 1000
                    commonResponseEntry.msg = ErrorUtils.getErrorMessage(result.errorMsg)
                    _collectEntry.value = commonResponseEntry
                }
                is NetworkResponse.Success -> {
                    val responseBody = result.body
                    // Check if the response body has a code field indicating success/error
                    if (responseBody.code == 200) {
                        _collectEntry.value = responseBody
                    } else {
                        // API returned error in response body
                        _collectEntry.value = responseBody
                    }
                }
                is NetworkResponse.UnknownError -> {
                    val commonResponseEntry = CommonResponseEntry()
                    commonResponseEntry.code = 1000
                    commonResponseEntry.msg = "未知错误"
                    _collectEntry.value = commonResponseEntry
                }
            }
        }
    }
    
    /**
     * 取消收藏
     */
    fun deleteCollect(id: String) {
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) {
                newsDetailRepository.deleteCollect(id)
            }
            when (result) {
                is NetworkResponse.NetError -> {
                    val commonResponseEntry = CommonResponseEntry()
                    commonResponseEntry.code = 1000
                    commonResponseEntry.msg = ErrorUtils.getErrorMessage(result.errorMsg)
                    _deleteCollectEntry.value = commonResponseEntry
                }
                is NetworkResponse.Success -> {
                    val responseBody = result.body
                    // Check if the response body has a code field indicating success/error
                    if (responseBody.code == 200) {
                        _deleteCollectEntry.value = responseBody
                    } else {
                        // API returned error in response body
                        _deleteCollectEntry.value = responseBody
                    }
                }
                is NetworkResponse.UnknownError -> {
                    val commonResponseEntry = CommonResponseEntry()
                    commonResponseEntry.code = 1000
                    commonResponseEntry.msg = "未知错误"
                    _deleteCollectEntry.value = commonResponseEntry
                }
            }
        }
    }

    /**
     * Get publisher info with caching
     */
    fun queryPublisherInfo(id: Int) {
        // Check cache first
        if (publisherInfoCache.containsKey(id)) {
            _publisherInfoEntry.value = publisherInfoCache[id]
            return
        }

        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) {
                newsDetailRepository.queryPublisherInfo(id, languageManager.getCurrentLanguageCode())
            }
            when (result) {
                is NetworkResponse.NetError -> {
                    val entry = PublisherInfoEntry()
                    entry.code = 1000
                    entry.msg = ErrorUtils.getErrorMessage(result.errorMsg)
                    _publisherInfoEntry.value = entry
                }
                is NetworkResponse.Success -> {
                    val responseBody = result.body
                    if (responseBody.code == 200) {
                        // Cache successful response
                        publisherInfoCache[id] = responseBody
                        _publisherInfoEntry.value = responseBody
                    } else {
                        _publisherInfoEntry.value = responseBody
                    }
                }
                is NetworkResponse.UnknownError -> {
                    val entry = PublisherInfoEntry()
                    entry.code = 1000
                    entry.msg = result.error?.message ?: "Unknown error"
                    _publisherInfoEntry.value = entry
                }
            }
        }
    }
}
