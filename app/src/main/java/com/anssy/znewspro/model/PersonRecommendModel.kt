package com.anssy.znewspro.model

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.anssy.znewspro.R
import com.anssy.znewspro.entry.HomeDataListEntry
import com.anssy.znewspro.entry.PersonRecommendListEntry
import com.anssy.znewspro.entry.SearchListEntry
import com.anssy.znewspro.repository.PersonRecommendRepository
import com.anssy.znewspro.utils.MVUtils.Companion.getString
import com.anssy.znewspro.utils.network.exception.NetworkResponse
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import razerdp.util.PopupUtils.getString
import javax.inject.Inject

/**
 * @Description 个人推荐
 * @Author yulu
 * @CreateTime 2025年07月07日 10:20:29
 */
@HiltViewModel
class PersonRecommendModel @Inject constructor(private val personRecommendRepository: PersonRecommendRepository) : ViewModel()  {
    private var _recommendListEntry:MutableLiveData<SearchListEntry> = MutableLiveData()
    var recommendListEntry:LiveData<SearchListEntry> = _recommendListEntry


    fun  queryRecommendList(pageNo:Int,pageSize:Int){
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO){
                personRecommendRepository.queryRecommendList(pageNo,pageSize)
            }
            when(result){
                is NetworkResponse.NetError->{
                    val personRecommendListEntry = SearchListEntry()
                    personRecommendListEntry.code = 1000
                    personRecommendListEntry.msg = getString(R.string.server_error_message)
                    _recommendListEntry.value = personRecommendListEntry
                }
                is NetworkResponse.Success->{
                    _recommendListEntry.value = result.body
                }
                is NetworkResponse.UnknownError->{
                    val personRecommendListEntry = SearchListEntry()
                    personRecommendListEntry.code = 1000
                    personRecommendListEntry.msg = getString(R.string.unknown_error)
                    _recommendListEntry.value = personRecommendListEntry
                }
            }
        }
    }
}