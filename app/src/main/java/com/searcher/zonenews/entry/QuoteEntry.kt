package com.searcher.zonenews.entry

import com.google.gson.annotations.SerializedName
import java.io.Serializable

data class KeyQuotesResponse(
    @SerializedName("quotes")
    val quotes: List<QuoteEntry>? = null
) : Serializable

data class QuoteEntry(
    @SerializedName("text")
    val text: String? = null,
    
    @SerializedName("entityType")
    val entityType: String? = null, // "person" or "organisation"
    
    @SerializedName("name")
    val name: String? = null,
    
    @SerializedName("role")
    val role: String? = null,
    
    @SerializedName("sourceURL")
    val sourceURL: String? = null,
    
    @SerializedName("category")
    val category: String? = null,
    
    @SerializedName("background")
    val background: String? = null
) : Serializable
