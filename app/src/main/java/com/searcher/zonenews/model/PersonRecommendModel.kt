package com.searcher.zonenews.model

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.searcher.zonenews.R
import com.searcher.zonenews.entry.HomeDataListEntry
import com.searcher.zonenews.entry.PersonRecommendListEntry
import com.searcher.zonenews.entry.SearchListEntry
import com.searcher.zonenews.repository.PersonRecommendRepository
import com.searcher.zonenews.utils.LanguageManager
import com.searcher.zonenews.utils.MVUtils.Companion.getString
import com.searcher.zonenews.utils.network.exception.NetworkResponse
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import razerdp.util.PopupUtils.getString
import javax.inject.Inject
import com.searcher.zonenews.utils.ErrorUtils

/**
 * @Description 个人推荐
 * @Author yulu
 * @CreateTime 2025年07月07日 10:20:29
 */
@HiltViewModel
class PersonRecommendModel @Inject constructor(
    private val personRecommendRepository: PersonRecommendRepository,
    private val languageManager: LanguageManager
) : ViewModel() {
    private var _recommendListEntry: MutableLiveData<SearchListEntry> = MutableLiveData()
    var recommendListEntry: LiveData<SearchListEntry> = _recommendListEntry

    fun queryRecommendList(pageNo: Int, pageSize: Int, sortBy: String? = null) {
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) {
                personRecommendRepository.queryRecommendList(
                    pageNo, 
                    pageSize, 
                    languageManager.getCurrentLanguageCode(),
                    sortBy
                )
            }
            when (result) {
                is NetworkResponse.NetError -> {
                    val personRecommendListEntry = SearchListEntry()
                    personRecommendListEntry.code = 1000
                    personRecommendListEntry.msg = ErrorUtils.getErrorMessage(result.errorMsg)
                    _recommendListEntry.value = personRecommendListEntry
                }
                is NetworkResponse.Success -> {
                    _recommendListEntry.value = result.body
                }
                is NetworkResponse.UnknownError -> {
                    val personRecommendListEntry = SearchListEntry()
                    personRecommendListEntry.code = 1000
                    personRecommendListEntry.msg = getString(R.string.unknown_error)
                    _recommendListEntry.value = personRecommendListEntry
                }
            }
        }
    }
}
