package com.searcher.zonenews.model

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.searcher.zonenews.R
import com.searcher.zonenews.entry.HomeDataListEntry
import com.searcher.zonenews.repository.HomeRepository
import com.searcher.zonenews.utils.LanguageManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import razerdp.util.PopupUtils.getString
import javax.inject.Inject

/**
 * @Description 主界面
 * @Author yulu
 * @CreateTime 2025年06月30日 14:01:42
 */
@HiltViewModel
class HomeModel @Inject constructor(
    private val homeRepository: HomeRepository,
    private val languageManager: LanguageManager,
    private val application: android.app.Application
) : ViewModel() {
    private var _homeDataList: MutableLiveData<HomeDataListEntry> = MutableLiveData<HomeDataListEntry>()
    var homeDataList: LiveData<HomeDataListEntry> = _homeDataList

    fun getHomeDataList(tag: String, pageNo: Int, pageSize: Int) {
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) {
                runCatching {
                    // Convert localized tab name to API tag
                    val apiTag = convertTabNameToApiTag(tag)
                    // Request pageSize items as requested (removed 20 minimum override to improve performance)
                    val size = pageSize
                    val langCode = languageManager.getCurrentLanguageCode()
                    android.util.Log.d("HomeModel", "Fetching home data - tag: $apiTag, page: $pageNo, size: $size, lang: $langCode")
                    homeRepository.queryHomeData(apiTag, pageNo, size, langCode)
                }
            }
            if (result.isSuccess) {
                if (result.getOrNull() == null) {
                    return@launch
                }
                val data = result.getOrNull()!!
                _homeDataList.value = data
            } else {
                val homeDataListEntry = HomeDataListEntry()
                homeDataListEntry.code = 500
                homeDataListEntry.msg = getString(R.string.server_error_message)
                _homeDataList.value = homeDataListEntry
            }
        }
    }
    
    /**
     * Get home data by topic tag (for Tag News popup).
     * Unlike getHomeDataList, this passes the tag directly without conversion.
     */
    fun getDataByTopicTag(topicTag: String, pageNo: Int, pageSize: Int) {
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) {
                runCatching {
                    // Pass topic tag AS IS (no lowercase conversion)
                    // Server might be case-sensitive or expecting the exact ID
                    val langCode = languageManager.getCurrentLanguageCode()
                    android.util.Log.d("HomeModel", "Fetching by topic tag - tag: $topicTag, page: $pageNo, size: $pageSize, lang: $langCode")
                    homeRepository.queryFeedByTopic(topicTag, pageNo, pageSize, langCode)
                }
            }
            if (result.isSuccess) {
                val data = result.getOrNull()
                if (data == null) {
                    return@launch
                }
                
                // CRITICAL: Check if response contains headlines. 
                // If requested topic has headlines, it's likely a server-side fallback to the generic "Today" feed.
                // User wants NO articles (empty state) instead of generic feed in this case.
                val hasHeadlines = !data.data?.headlines.isNullOrEmpty()
                
                if (hasHeadlines) {
                    android.util.Log.w("HomeModel", "Received headlines for topic request '$topicTag', assuming server fallback to generic feed. Returning empty.")
                    val emptyEntry = HomeDataListEntry()
                    emptyEntry.code = 200
                    val dataDTO = HomeDataListEntry.DataDTO()
                    dataDTO.articles = ArrayList()
                    emptyEntry.data = dataDTO
                    _homeDataList.value = emptyEntry
                } else {
                    _homeDataList.value = data
                }
            } else {
                val homeDataListEntry = HomeDataListEntry()
                homeDataListEntry.code = 500
                homeDataListEntry.msg = getString(R.string.server_error_message)
                _homeDataList.value = homeDataListEntry
            }
        }
    }
    
    /**
     * Convert localized tab name to API tag
     * API expects: "today", "hk", "china"
     * UI uses localized strings like "Today", "Hong Kong", "China" (in various languages)
     */
    private fun convertTabNameToApiTag(tabName: String): String? {
        // Normalize the tab name for comparison (case-insensitive, trim whitespace)
        val normalized = tabName.trim().lowercase()
        
        // Check against common localized variations
        return when {
            // Today tab variations
            normalized == getString(R.string.today).lowercase() || 
            normalized == "today" || 
            normalized == "今日" || 
            normalized == "今天" -> "today"
            
            // Hong Kong tab variations
            normalized == getString(R.string.hongkong).lowercase() || 
            normalized.contains("hong kong") || 
            normalized.contains("hongkong") || 
            normalized == "hk" || 
            normalized == "香港" -> "hk"
            
            // China tab variations
            normalized == getString(R.string.china).lowercase() || 
            normalized == "china" || 
            normalized == "中国" || 
            normalized == "中國" -> "china"
            
            // If it's already an API tag, return as-is
            normalized in listOf("today", "hk", "china") -> normalized
            
            // Unknown tab name, return null (API will handle as no tag filter)
            else -> {
                android.util.Log.w("HomeModel", "Unknown tab name: $tabName, returning null")
                null
            }
        }
    }
}
