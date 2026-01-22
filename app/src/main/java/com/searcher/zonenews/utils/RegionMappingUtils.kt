package com.searcher.zonenews.utils

import android.util.Log

/**
 * Utility class for mapping between UI region names and API tags
 * @Author yulu
 * @CreateTime 2025年01月15日 10:00:00
 */
object RegionMappingUtils {

    private const val TAG = "RegionMappingUtils"

    /**
     * Mapping from UI region names to API tags
     * Note: The API uses hyphenated format for asia-others and europe-others
     */
    private val uiToApiMapping = mapOf(
        "hongKong" to "hk",
        "mainlandChina" to "china", 
        "unitedKingdom" to "uk",
        "unitedStates" to "usa",
        "asiaOther" to "asia-others",
        "europeOther" to "europe-others"
    )

    /**
     * Mapping from API tags to UI region names
     * Includes alternate formats that the API might return
     */
    private val apiToUiMapping: Map<String, String> by lazy {
        val baseMapping = uiToApiMapping.entries.associate { it.value to it.key }
        // Add alternate formats that the API might return for asia-others and europe-others
        baseMapping + mapOf(
            "asia_others" to "asiaOther",      // underscore variant
            "europe_others" to "europeOther",   // underscore variant
            "asiaOthers" to "asiaOther",        // camelCase variant
            "europeOthers" to "europeOther",    // camelCase variant
            "asia" to "asiaOther",              // shortened variant
            "europe" to "europeOther"           // shortened variant
        )
    }

    /**
     * Convert UI region name to API tag
     */
    fun getApiTag(uiRegionName: String): String? {
        return uiToApiMapping[uiRegionName]
    }

    /**
     * Convert API tag to UI region name
     * Handles various formats the API might return
     */
    fun getUiRegionName(apiTag: String): String? {
        return apiToUiMapping[apiTag]
    }

    /**
     * Get all UI region names
     */
    fun getAllUiRegionNames(): List<String> {
        return uiToApiMapping.keys.toList()
    }

    /**
     * Get all API tags
     */
    fun getAllApiTags(): List<String> {
        return uiToApiMapping.values.toList()
    }

    /**
     * Check if a UI region name is valid
     */
    fun isValidUiRegionName(uiRegionName: String): Boolean {
        return uiToApiMapping.containsKey(uiRegionName)
    }

    /**
     * Check if an API tag is valid (including alternate formats)
     */
    fun isValidApiTag(apiTag: String): Boolean {
        return apiToUiMapping.containsKey(apiTag)
    }

    /**
     * Normalize an API tag to the canonical format
     * This handles alternate formats that the API might return
     */
    fun normalizeApiTag(apiTag: String): String {
        return when {
            // Handle asia-others variants
            apiTag.equals("asia-others", ignoreCase = true) -> "asia-others"
            apiTag.equals("asia_others", ignoreCase = true) -> "asia-others"
            apiTag.equals("asiaOthers", ignoreCase = true) -> "asia-others"
            apiTag.equals("asiaother", ignoreCase = true) -> "asia-others"
            // Handle europe-others variants
            apiTag.equals("europe-others", ignoreCase = true) -> "europe-others"
            apiTag.equals("europe_others", ignoreCase = true) -> "europe-others"
            apiTag.equals("europeOthers", ignoreCase = true) -> "europe-others"
            apiTag.equals("europeother", ignoreCase = true) -> "europe-others"
            // Return original for other tags
            else -> apiTag
        }
    }

    /**
     * Check if an API tag matches the expected tag for a UI region
     * Handles various API tag formats that might be returned
     */
    fun isTagMatchingRegion(apiTag: String, uiRegionName: String): Boolean {
        val expectedTag = uiToApiMapping[uiRegionName] ?: return false
        val normalizedApiTag = normalizeApiTag(apiTag)
        val matches = normalizedApiTag.equals(expectedTag, ignoreCase = true)
        
        if (!matches && (uiRegionName == "asiaOther" || uiRegionName == "europeOther")) {
            Log.d(TAG, "Tag comparison for $uiRegionName: apiTag='$apiTag' normalized='$normalizedApiTag' expected='$expectedTag' matches=$matches")
        }
        
        return matches
    }
}

