package com.searcher.zonenews.model

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.searcher.zonenews.entry.CommonResponseEntry
import com.searcher.zonenews.entry.PublisherRegionEntry
import com.searcher.zonenews.repository.PublisherRegionRepository
import com.searcher.zonenews.utils.LanguageManager
import com.searcher.zonenews.utils.network.exception.NetworkResponse
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject
import com.searcher.zonenews.utils.ErrorUtils

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
                    // Don't send language parameter for GET request - it causes backend to try fetching articles
                    publisherRegionRepository.getPublisherRegions(null)
                }

                when (result) {
                    is NetworkResponse.Success -> {
                        // Check if the response body has a code field indicating success/error
                        val responseBody = result.body
                        Log.d("PublisherRegionModel", "=== API RESPONSE DEBUG ===")
                        Log.d("PublisherRegionModel", "Received response: code=${responseBody.code}, msg=${responseBody.msg}")
                        Log.d("PublisherRegionModel", "Data: ${responseBody.data}")
                        
                        // Log all available regions with their exact tags
                        responseBody.data?.regions?.forEachIndexed { index, region ->
                            Log.d("PublisherRegionModel", "Available region[$index]: tag='${region.tag}', displayName='${region.displayName}'")
                        }
                        
                        // Log all selected regions with exact values
                        Log.d("PublisherRegionModel", "Selected regions count: ${responseBody.data?.selected?.size ?: 0}")
                        responseBody.data?.selected?.forEachIndexed { index, tag ->
                            Log.d("PublisherRegionModel", "Selected[$index]: '$tag' (length=${tag.length})")
                        }
                        
                        // Specific check for asia-others and europe-others
                        val selected = responseBody.data?.selected ?: emptyList()
                        Log.d("PublisherRegionModel", "Contains 'asia-others': ${selected.contains("asia-others")}")
                        Log.d("PublisherRegionModel", "Contains 'europe-others': ${selected.contains("europe-others")}")
                        Log.d("PublisherRegionModel", "=== END API RESPONSE DEBUG ===")
                        
                        if (responseBody.code == 200) {
                            _publisherRegions.value = responseBody
                            _errorMessage.value = null // Clear any previous errors
                            Log.d("PublisherRegionModel", "Publisher regions loaded successfully. Selected: ${responseBody.data?.selected}")
                        } else {
                            // API returned error in response body
                            val errorMsg = responseBody.msg ?: "Failed to load regions"
                            _errorMessage.value = errorMsg
                            Log.e("PublisherRegionModel", "API error loading regions: Code ${responseBody.code}, Message: $errorMsg")
                        }
                    }
                    is NetworkResponse.NetError -> {
                        val errorMsg = ErrorUtils.getErrorMessage(result.errorMsg)
                        _errorMessage.value = errorMsg
                        Log.e("PublisherRegionModel", "Network error loading regions: HTTP ${result.httpCode}, Message: $errorMsg", result.exception)
                    }
                    is NetworkResponse.UnknownError -> {
                        val errorMsg = result.error?.message ?: "Unknown error occurred"
                        _errorMessage.value = errorMsg
                        Log.e("PublisherRegionModel", "Unknown error loading regions", result.error)
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
        Log.d("PublisherRegionModel", "=== ADD REGION REQUEST ===")
        Log.d("PublisherRegionModel", "Attempting to ADD region with tag: '$tag'")
        
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null

            try {
                val result = withContext(Dispatchers.IO) {
                    publisherRegionRepository.editPublisherRegion(
                        action = "ADD",
                        tag = tag,
                        language = null // Don't send language parameter - it causes backend errors
                    )
                }

                when (result) {
                    is NetworkResponse.Success -> {
                        val responseBody = result.body
                        Log.d("PublisherRegionModel", "ADD response for '$tag': code=${responseBody.code}, msg=${responseBody.msg}")
                        if (responseBody.code == 200) {
                            Log.d("PublisherRegionModel", "Successfully added region: $tag")
                            _regionUpdateResult.value = responseBody
                            // Reload regions to get updated selection
                            loadPublisherRegions()
                        } else {
                            _errorMessage.value = responseBody.msg ?: "Failed to update region"
                            Log.e("PublisherRegionModel", "API error updating region: Code ${responseBody.code}, Message: ${responseBody.msg}")
                        }
                    }
                    is NetworkResponse.NetError -> {
                        val errorMsg = ErrorUtils.getErrorMessage(result.errorMsg)
                        _errorMessage.value = errorMsg
                        Log.e("PublisherRegionModel", "Network error updating region: HTTP ${result.httpCode}, Message: $errorMsg", result.exception)
                    }
                    is NetworkResponse.UnknownError -> {
                        _errorMessage.value = result.error?.message ?: "Unknown error occurred"
                        Log.e("PublisherRegionModel", "Unknown error updating region", result.error)
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
        Log.d("PublisherRegionModel", "=== REMOVE REGION REQUEST ===")
        Log.d("PublisherRegionModel", "Attempting to REMOVE region with tag: '$tag'")
        
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null

            try {
                val result = withContext(Dispatchers.IO) {
                    publisherRegionRepository.editPublisherRegion(
                        action = "REMOVE",
                        tag = tag,
                        language = null // Don't send language parameter - it causes backend errors
                    )
                }

                when (result) {
                    is NetworkResponse.Success -> {
                        val responseBody = result.body
                        Log.d("PublisherRegionModel", "REMOVE response for '$tag': code=${responseBody.code}, msg=${responseBody.msg}")
                        if (responseBody.code == 200) {
                            Log.d("PublisherRegionModel", "Successfully removed region: $tag")
                            _regionUpdateResult.value = responseBody
                            // Reload regions to get updated selection
                            loadPublisherRegions()
                        } else {
                            _errorMessage.value = responseBody.msg ?: "Failed to update region"
                            Log.e("PublisherRegionModel", "API error updating region: Code ${responseBody.code}, Message: ${responseBody.msg}")
                        }
                    }
                    is NetworkResponse.NetError -> {
                        val errorMsg = ErrorUtils.getErrorMessage(result.errorMsg)
                        _errorMessage.value = errorMsg
                        Log.e("PublisherRegionModel", "Network error updating region: HTTP ${result.httpCode}, Message: $errorMsg", result.exception)
                    }
                    is NetworkResponse.UnknownError -> {
                        _errorMessage.value = result.error?.message ?: "Unknown error occurred"
                        Log.e("PublisherRegionModel", "Unknown error updating region", result.error)
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
     * Uses normalized matching to handle API tag format variations
     */
    fun isRegionSelected(tag: String): Boolean {
        val regions = _publisherRegions.value?.data?.selected ?: return false
        
        // First try exact match
        if (regions.contains(tag)) {
            return true
        }
        
        // For asia-others and europe-others, try normalized matching
        // in case the API returns different format (e.g., asia_others)
        if (tag == "asia-others" || tag == "europe-others") {
            val normalizedTag = com.searcher.zonenews.utils.RegionMappingUtils.normalizeApiTag(tag)
            val found = regions.any { 
                com.searcher.zonenews.utils.RegionMappingUtils.normalizeApiTag(it) == normalizedTag
            }
            Log.d("PublisherRegionModel", "isRegionSelected normalized check: tag='$tag', normalizedTag='$normalizedTag', found=$found, regions=$regions")
            return found
        }
        
        return false
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

