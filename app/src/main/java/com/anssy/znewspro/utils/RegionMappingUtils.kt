package com.anssy.znewspro.utils

/**
 * Utility class for mapping between UI region names and API tags
 * @Author yulu
 * @CreateTime 2025年01月15日 10:00:00
 */
object RegionMappingUtils {

    /**
     * Mapping from UI region names to API tags
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
     */
    private val apiToUiMapping = uiToApiMapping.entries.associate { it.value to it.key }

    /**
     * Convert UI region name to API tag
     */
    fun getApiTag(uiRegionName: String): String? {
        return uiToApiMapping[uiRegionName]
    }

    /**
     * Convert API tag to UI region name
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
     * Check if an API tag is valid
     */
    fun isValidApiTag(apiTag: String): Boolean {
        return apiToUiMapping.containsKey(apiTag)
    }
}

