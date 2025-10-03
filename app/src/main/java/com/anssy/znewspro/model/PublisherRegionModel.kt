package com.anssy.znewspro.model

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.anssy.znewspro.entry.CommonResponseEntry
import com.anssy.znewspro.entry.PublisherRegionEntry
import com.anssy.znewspro.repository.PublisherRegionRepository
import com.anssy.znewspro.utils.LanguageManager
import com.anssy.znewspro.utils.network.exception.NetworkResponse
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

/**
 * Publisher Region ViewModel
 * Handles publisher region selection and API calls
 * @Author yulu
 * @CreateTime 2025年01月15日 10:00:00
 */
@HiltViewModel
class PublisherRegionModel @Inject constructor(
    private val publisherRegionRepository: PublisherRegionRepository,
    private val languageManager: LanguageManager
) : ViewModel() {

    private val _publisherRegions = MutableLiveData<PublisherRegionEntry>()
    val publisherRegions: LiveData<PublisherRegionEntry> = _publisherRegions

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _errorMessage = MutableLiveData<String>()
    val errorMessage: LiveData<String> = _errorMessage

    private val _regionUpdateResult = MutableLiveData<CommonResponseEntry>()
    val regionUpdateResult: LiveData<CommonResponseEntry> = _regionUpdateResult

    /**
     * Load publisher regions from API
     */
    fun loadPublisherRegions() {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null

            try {
                val result = withContext(Dispatchers.IO) {
                    publisherRegionRepository.getPublisherRegions(languageManager.getCurrentLanguageCode())
                }

                when (result) {
                    is NetworkResponse.Success -> {
                        _publisherRegions.value = result.body
                    }
                    is NetworkResponse.NetError -> {
                        _errorMessage.value = "Network error occurred"
                    }
                    is NetworkResponse.UnknownError -> {
                        _errorMessage.value = "Unknown error occurred"
                    }
                }
            } catch (e: Exception) {
                _errorMessage.value = e.message ?: "Failed to load regions"
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Add a region to user's selection
     */
    fun addRegion(tag: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null

            try {
                val result = withContext(Dispatchers.IO) {
                    publisherRegionRepository.editPublisherRegion(
                        action = "ADD",
                        tag = tag,
                        language = languageManager.getCurrentLanguageCode()
                    )
                }

                when (result) {
                    is NetworkResponse.Success -> {
                        _regionUpdateResult.value = result.body
                        // Reload regions to get updated selection
                        loadPublisherRegions()
                    }
                    is NetworkResponse.NetError -> {
                        _errorMessage.value = "Network error occurred"
                    }
                    is NetworkResponse.UnknownError -> {
                        _errorMessage.value = "Unknown error occurred"
                    }
                }
            } catch (e: Exception) {
                _errorMessage.value = e.message ?: "Failed to add region"
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Remove a region from user's selection
     */
    fun removeRegion(tag: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null

            try {
                val result = withContext(Dispatchers.IO) {
                    publisherRegionRepository.editPublisherRegion(
                        action = "REMOVE",
                        tag = tag,
                        language = languageManager.getCurrentLanguageCode()
                    )
                }

                when (result) {
                    is NetworkResponse.Success -> {
                        _regionUpdateResult.value = result.body
                        // Reload regions to get updated selection
                        loadPublisherRegions()
                    }
                    is NetworkResponse.NetError -> {
                        _errorMessage.value = "Network error occurred"
                    }
                    is NetworkResponse.UnknownError -> {
                        _errorMessage.value = "Unknown error occurred"
                    }
                }
            } catch (e: Exception) {
                _errorMessage.value = e.message ?: "Failed to remove region"
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Toggle region selection (add if not selected, remove if selected)
     */
    fun toggleRegion(tag: String, isCurrentlySelected: Boolean) {
        if (isCurrentlySelected) {
            removeRegion(tag)
        } else {
            addRegion(tag)
        }
    }

    /**
     * Check if a region is selected
     */
    fun isRegionSelected(tag: String): Boolean {
        val regions = _publisherRegions.value?.data?.selected
        return regions?.contains(tag) ?: false
    }

    /**
     * Get all available regions
     */
    fun getAvailableRegions(): List<PublisherRegionEntry.DataDTO.RegionDTO>? {
        return _publisherRegions.value?.data?.regions
    }

    /**
     * Get selected regions
     */
    fun getSelectedRegions(): List<String>? {
        return _publisherRegions.value?.data?.selected
    }
}
