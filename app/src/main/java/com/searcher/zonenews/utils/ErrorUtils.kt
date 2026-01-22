package com.searcher.zonenews.utils

import com.google.gson.Gson
import com.searcher.zonenews.R
import com.searcher.zonenews.entry.ErrorData
import razerdp.util.PopupUtils.getString

/**
 * @Description Utility for parsing API errors and getting localized messages
 * @Author Antigravity
 */
object ErrorUtils {

    fun parseError(json: String?): ErrorData? {
        if (json.isNullOrEmpty()) return null
        return try {
            // Attempt to parse the JSON error body
            // Format: {"msg": "Error message", "code": "AUE06"}
            Gson().fromJson(json, ErrorData::class.java)
        } catch (e: Exception) {
            // Handle HTML error pages
            if (json != null && (json.contains("<!doctype html>") || json.contains("<html"))) {
                val msg = when {
                    json.contains("sqlite3.OperationalError") -> "Database error on server. Please try again later."
                    json.contains("OperationalError") -> "Server error. Please try again later."
                    else -> "Server error occurred. Please try again later."
                }
                ErrorData(msg, "SERVER_ERROR")
            } else {
                // If parsing fails, create a wrapper with the raw string
                ErrorData(json, "UNKNOWN")
            }
        }
    }

    fun getLocalizedMessage(errorData: ErrorData?): String {
        if (errorData == null) return getString(R.string.error_server_error)

        // Check the last 2 characters for consistent error types
        // Code format: XXKNN (e.g., AUE06)
        val code = errorData.code
        val errorType = if (code.length >= 2) code.takeLast(2) else ""

        val resId = when (errorType) {
            "00" -> R.string.error_invalid_format
            "01" -> R.string.error_missing_parameter
            "02" -> R.string.error_not_authenticated
            "03" -> R.string.error_server_error
            "04" -> R.string.error_not_found
            "05" -> R.string.error_invalid_value
            "06" -> R.string.error_already_exists
            "07" -> R.string.error_auth_failed
            "08" -> R.string.error_internal_server_error
            "09" -> R.string.error_forbidden
            "0A" -> R.string.error_session_expired
            "0B" -> R.string.error_weak_password
            "0C" -> R.string.error_verification_failed
            "0D" -> R.string.error_unsupported_operation
            else -> 0 // 0 means no match found
        }

        return if (resId != 0) {
            "[${errorData.code}] ${getString(resId)}"
        } else {
            // Fallback to API message
            errorData.msg
        }
    }
    
    // Convenience method to parse and get message in one go
    fun getErrorMessage(json: String?): String {
        val errorData = parseError(json)
        return getLocalizedMessage(errorData)
    }
}
