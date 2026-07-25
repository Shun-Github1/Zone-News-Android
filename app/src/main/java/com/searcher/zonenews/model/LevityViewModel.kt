package com.searcher.zonenews.model

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.searcher.zonenews.entry.HomeDataListEntry
import com.searcher.zonenews.repository.HomeRepository
import com.searcher.zonenews.utils.LanguageManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class LevityViewModel @Inject constructor(
    private val homeRepository: HomeRepository,
    private val languageManager: LanguageManager
) : ViewModel() {

    private val _levityFeed = MutableLiveData<HomeDataListEntry>()
    val levityFeed: LiveData<HomeDataListEntry> = _levityFeed

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading
    
    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    fun getLevityFeed(pageNo: Int, pageSize: Int) {
        _isLoading.value = true
        _error.value = null
        
        viewModelScope.launch {
            try {
                val result = withContext(Dispatchers.IO) {
                    homeRepository.getLevityFeed(
                        language = languageManager.getCurrentLanguageCode(),
                        pageNo = pageNo,
                        pageSize = pageSize
                    )
                }
                
                // Assuming HomeDataListEntry is the direct response or has code/msg
                // Adjust based on actual structure if it's GenericResponse wrapper
                // Based on AppHttpService: getLevityFeed returns HomeDataListEntry directly
                
                if (result.code == 200) { // Assuming 200 is success
                     _levityFeed.value = result
                } else {
                    _error.value = result.msg ?: "Failed to load"
                }
                
            } catch (e: Exception) {
                _error.value = e.message ?: "Unknown error"
            } finally {
                _isLoading.value = false
            }
        }
    }
}
