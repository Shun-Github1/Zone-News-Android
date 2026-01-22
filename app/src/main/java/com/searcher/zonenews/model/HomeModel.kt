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
                    homeRepository.queryHomeData(apiTag, pageNo, size, languageManager.getCurrentLanguageCode())
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
