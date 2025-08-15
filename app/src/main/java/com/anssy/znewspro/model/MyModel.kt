package com.anssy.znewspro.model

import android.view.View
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.anssy.znewspro.entry.MyFormationEntry
import com.anssy.znewspro.entry.ViewHisEntry
import com.anssy.znewspro.repository.MyRepository
import com.anssy.znewspro.utils.network.exception.NetworkResponse
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject
import razerdp.util.PopupUtils.getString
import com.anssy.znewspro.R

/**
 * @Description 个人
 * @Author yulu
 * @CreateTime 2025年07月07日 14:51:34
 */
@HiltViewModel
class MyModel @Inject constructor(private val myRepository: MyRepository) : ViewModel() {
    private var _myEntry: MutableLiveData<MyFormationEntry> = MutableLiveData()
    var myEntry: LiveData<MyFormationEntry> = _myEntry

    private var _myViewHisEntry: MutableLiveData<ViewHisEntry> = MutableLiveData()
    var myViewHisEntry: LiveData<ViewHisEntry> = _myViewHisEntry

    



    fun queryMyFormation() {
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) {
                myRepository.queryMyFormation()
            }
            when (result) {
                is NetworkResponse.NetError -> {
                    val myFormationEntry = MyFormationEntry()
                    myFormationEntry.code = 1000
                    myFormationEntry.msg = getString(R.string.server_error_message)
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
                myRepository.queryMyViewHist()
            }
            when (result) {
                is NetworkResponse.NetError -> {
                    val myFormationEntry = ViewHisEntry()
                    myFormationEntry.code = 1000
                    myFormationEntry.msg = getString(R.string.server_error_message)
                    _myViewHisEntry.value = myFormationEntry
                }

                is NetworkResponse.Success -> {
                    _myViewHisEntry.value = result.body
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
                myRepository.queryMyCollect()
            }
            when (result) {
                is NetworkResponse.NetError -> {
                    val myFormationEntry = ViewHisEntry()
                    myFormationEntry.code = 1000
                    myFormationEntry.msg = getString(R.string.server_error_message)
                    _myViewHisEntry.value = myFormationEntry
                }

                is NetworkResponse.Success -> {
                    _myViewHisEntry.value = result.body
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

    
}