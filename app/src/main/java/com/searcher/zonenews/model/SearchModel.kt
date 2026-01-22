package com.searcher.zonenews.model

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.searcher.zonenews.entry.SearchListEntry
import com.searcher.zonenews.repository.SearchRepository
import com.searcher.zonenews.utils.LanguageManager
import com.searcher.zonenews.utils.network.exception.NetworkResponse
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject
import razerdp.util.PopupUtils.getString
import com.searcher.zonenews.R
import com.searcher.zonenews.utils.ErrorUtils

/**
 * @Description 搜索
 * @Author yulu
 * @CreateTime 2025年07月04日 16:28:13
 */

@HiltViewModel
class SearchModel @Inject constructor(
    private val searchRepository: SearchRepository,
    private val languageManager: LanguageManager
): ViewModel() {
    private var _searchListEntry: MutableLiveData<SearchListEntry> = MutableLiveData<SearchListEntry>()
    var searchListEntry: LiveData<SearchListEntry> = _searchListEntry
    
    fun querySearchList() {
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) {
                searchRepository.querySearchList(languageManager.getCurrentLanguageCode())
            }
            when (result) {
                is NetworkResponse.NetError -> {
                    val searchListEntry = SearchListEntry()
                    searchListEntry.code = 1000
                    searchListEntry.msg = ErrorUtils.getErrorMessage(result.errorMsg)
                    _searchListEntry.value = searchListEntry
                }
                is NetworkResponse.Success -> {
                    _searchListEntry.value = result.body
                }
                is NetworkResponse.UnknownError -> {
                    val searchListEntry = SearchListEntry()
                    searchListEntry.code = 1000
                    searchListEntry.msg = "未知错误"
                    _searchListEntry.value = searchListEntry
                }
            }
        }
    }
    
    fun queryListByTitle(
        title: String,
        page: Int? = null,
        limit: Int? = null,
        sortBy: String? = null
    ) {
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) {
                searchRepository.queryNewsByTitle(
                    title,
                    languageManager.getCurrentLanguageCode(),
                    page,
                    limit,
                    sortBy
                )
            }
            when (result) {
                is NetworkResponse.NetError -> {
                    val searchListEntry = SearchListEntry()
                    searchListEntry.code = 1000
                    searchListEntry.msg = ErrorUtils.getErrorMessage(result.errorMsg)
                    _searchListEntry.value = searchListEntry
                }
                is NetworkResponse.Success -> {
                    _searchListEntry.value = result.body
                }
                is NetworkResponse.UnknownError -> {
                    val searchListEntry = SearchListEntry()
                    searchListEntry.code = 1000
                    searchListEntry.msg = "未知错误"
                    _searchListEntry.value = searchListEntry
                }
            }
        }
    }
}
