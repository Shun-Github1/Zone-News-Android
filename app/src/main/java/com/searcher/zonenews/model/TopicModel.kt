package com.searcher.zonenews.model

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.searcher.zonenews.entry.CommonResponseEntry
import com.searcher.zonenews.entry.TopicListEntry
import com.searcher.zonenews.repository.TopicRepository
import com.searcher.zonenews.utils.LanguageManager
import com.searcher.zonenews.utils.network.exception.NetworkResponse
import com.searcher.zonenews.utils. SystemDialogUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject
import razerdp.util.PopupUtils.getString
import com.searcher.zonenews.R
import com.searcher.zonenews.utils.ErrorUtils

/**
 * @Description 主题
 * @Author yulu
 * @CreateTime 2025年07月07日 11:31:20
 */
@HiltViewModel
class TopicModel @Inject constructor(
    private val topicRepository: TopicRepository,
    private val languageManager: LanguageManager
): ViewModel() {
    private var _topicListEntry: MutableLiveData<TopicListEntry> = MutableLiveData()
    var topicListEntry: LiveData<TopicListEntry> = _topicListEntry

    private var _myTopicsEntry: MutableLiveData<TopicListEntry> = MutableLiveData()
    var myTopicsEntry: LiveData<TopicListEntry> = _myTopicsEntry

    private var _sectorsEntry: MutableLiveData<TopicListEntry> = MutableLiveData()
    var sectorsEntry: LiveData<TopicListEntry> = _sectorsEntry

    private var _regionsEntry: MutableLiveData<TopicListEntry> = MutableLiveData()
    var regionsEntry: LiveData<TopicListEntry> = _regionsEntry

    private var _trendingTopicsEntry: MutableLiveData<TopicListEntry> = MutableLiveData()
    var trendingTopicsEntry: LiveData<TopicListEntry> = _trendingTopicsEntry

    private var _trendingTopicsLimitedEntry: MutableLiveData<TopicListEntry> = MutableLiveData()
    var trendingTopicsLimitedEntry: LiveData<TopicListEntry> = _trendingTopicsLimitedEntry

    private var _commonResponseEntry: MutableLiveData<CommonResponseEntry> = MutableLiveData()
    var commonResponseEntry: LiveData<CommonResponseEntry> = _commonResponseEntry

    fun queryMyTopics() {
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) {
                topicRepository.queryMyTopic(languageManager.getCurrentLanguageCode())
            }
            when (result) {
                is NetworkResponse.NetError -> {
                    val topicListEntry = TopicListEntry()
                    topicListEntry.code = 1000
                    topicListEntry.msg = ErrorUtils.getErrorMessage(result.errorMsg)
                    _myTopicsEntry.value = topicListEntry
                }
                is NetworkResponse.Success -> {
                    _myTopicsEntry.value = result.body
                }
                is NetworkResponse.UnknownError -> {
                    val topicListEntry = TopicListEntry()
                    topicListEntry.code = 1000
                    topicListEntry.msg = "未知错误"
                    _myTopicsEntry.value = topicListEntry
                }
            }
        }
    }

    fun queryAllTopics() {
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) {
                topicRepository.queryAllTopic(languageManager.getCurrentLanguageCode())
            }
            when (result) {
                is NetworkResponse.NetError -> {
                    val topicListEntry = TopicListEntry()
                    topicListEntry.code = 1000
                    topicListEntry.msg = ErrorUtils.getErrorMessage(result.errorMsg)
                    _topicListEntry.value = topicListEntry
                }
                is NetworkResponse.Success -> {
                    _topicListEntry.value = result.body
                }
                is NetworkResponse.UnknownError -> {
                    val topicListEntry = TopicListEntry()
                    topicListEntry.code = 1000
                    topicListEntry.msg = "未知错误"
                    _topicListEntry.value = topicListEntry
                }
            }
        }
    }

    fun editTopic(type: String, topic: String) {
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) {
                topicRepository.editTopic(type, topic, languageManager.getCurrentLanguageCode())
            }
            when (result) {
                is NetworkResponse.NetError -> {
                    val commonResponseEntry = CommonResponseEntry()
                    commonResponseEntry.code = 1000
                    commonResponseEntry.msg = ErrorUtils.getErrorMessage(result.errorMsg)
                    _commonResponseEntry.value = commonResponseEntry
                }

                is NetworkResponse.Success -> {
                    result.body.msg = topic
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

    /**
     * Optimistically update the user's followed topics list in memory.
     * This allows multiple components (Feed, Popup, etc.) to stay in sync instantly.
     */
    fun updateMyTopicsOptimistically(type: String, topicTag: String, displayName: String? = null) {
        val currentEntry = _myTopicsEntry.value ?: return
        val currentData = currentEntry.data ?: return
        val currentTopics = currentData.topics?.toMutableList() ?: mutableListOf()
        
        if (type == com.searcher.zonenews.utils.Constants.TYPE_TOPIC_ADD) {
            if (!currentTopics.any { it.tag.equals(topicTag, ignoreCase = true) }) {
                // Lookup localized displayName from cached all-topics list if not provided
                val resolvedDisplayName = displayName 
                    ?: _topicListEntry.value?.data?.topics
                        ?.find { it.tag.equals(topicTag, ignoreCase = true) }?.displayName
                    ?: topicTag
                val newTopic = TopicListEntry.TopicDTO().apply {
                    tag = topicTag
                    this.displayName = resolvedDisplayName
                }
                currentTopics.add(newTopic)
            }
        } else if (type == com.searcher.zonenews.utils.Constants.TYPE_TOPIC_DELETE) {
            currentTopics.removeAll { it.tag.equals(topicTag, ignoreCase = true) }
        }
        
        currentData.topics = currentTopics
        _myTopicsEntry.value = currentEntry
    }

    fun getTrendingTopics() {
        // Fetches ALL topics for "All" tab using /profile/listtopics (no limits, like regions)
        // This is separate from /feed/trending-topics which is limited
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) {
                topicRepository.getAllTopics(languageManager.getCurrentLanguageCode())
            }
            when (result) {
                is NetworkResponse.NetError -> {
                    val topicListEntry = TopicListEntry()
                    topicListEntry.code = 1000
                    topicListEntry.msg = ErrorUtils.getErrorMessage(result.errorMsg)
                    _trendingTopicsEntry.value = topicListEntry
                }
                is NetworkResponse.Success -> {
                    _trendingTopicsEntry.value = result.body
                }
                is NetworkResponse.UnknownError -> {
                    val topicListEntry = TopicListEntry()
                    topicListEntry.code = 1000
                    topicListEntry.msg = "未知错误"
                    _trendingTopicsEntry.value = topicListEntry
                }
            }
        }
    }

    fun getTrendingTopicsLimited() {
        // Fetches 3-6 trending topics for "Trending" tab
        // Uses /feed/trending-topics which returns 3-6 randomly selected topics
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) {
                topicRepository.getTrendingTopics(languageManager.getCurrentLanguageCode(), all = false)
            }
            when (result) {
                is NetworkResponse.NetError -> {
                    val topicListEntry = TopicListEntry()
                    topicListEntry.code = 1000
                    topicListEntry.msg = ErrorUtils.getErrorMessage(result.errorMsg)
                    _trendingTopicsLimitedEntry.value = topicListEntry
                }
                is NetworkResponse.Success -> {
                    _trendingTopicsLimitedEntry.value = result.body
                }
                is NetworkResponse.UnknownError -> {
                    val topicListEntry = TopicListEntry()
                    topicListEntry.code = 1000
                    topicListEntry.msg = "未知错误"
                    _trendingTopicsLimitedEntry.value = topicListEntry
                }
            }
        }
    }

    fun querySectors() {
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) {
                topicRepository.getSectors(languageManager.getCurrentLanguageCode())
            }
            when (result) {
                is NetworkResponse.NetError -> {
                    val topicListEntry = TopicListEntry()
                    topicListEntry.code = 1000
                    topicListEntry.msg = ErrorUtils.getErrorMessage(result.errorMsg)
                    _sectorsEntry.value = topicListEntry
                }
                is NetworkResponse.Success -> {
                    _sectorsEntry.value = result.body
                }
                is NetworkResponse.UnknownError -> {
                    val topicListEntry = TopicListEntry()
                    topicListEntry.code = 1000
                    topicListEntry.msg = "未知错误"
                    _sectorsEntry.value = topicListEntry
                }
            }
        }
    }

    fun queryRegions() {
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) {
                topicRepository.getRegions(languageManager.getCurrentLanguageCode())
            }
            when (result) {
                is NetworkResponse.NetError -> {
                    val topicListEntry = TopicListEntry()
                    topicListEntry.code = 1000
                    topicListEntry.msg = ErrorUtils.getErrorMessage(result.errorMsg)
                    _regionsEntry.value = topicListEntry
                }
                is NetworkResponse.Success -> {
                    _regionsEntry.value = result.body
                }
                is NetworkResponse.UnknownError -> {
                    val topicListEntry = TopicListEntry()
                    topicListEntry.code = 1000
                    topicListEntry.msg = "未知错误"
                    _regionsEntry.value = topicListEntry
                }
            }
        }
    }
}
