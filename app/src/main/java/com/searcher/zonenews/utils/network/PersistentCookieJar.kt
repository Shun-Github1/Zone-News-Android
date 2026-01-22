package com.searcher.zonenews.utils.network

import android.content.Context
import android.content.SharedPreferences
import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl
import java.util.concurrent.ConcurrentHashMap

/**
 * Persistent CookieJar implementation that stores cookies in SharedPreferences
 * This is needed for the new API which uses cookie-based authentication
 */
class PersistentCookieJar(private val context: Context) : CookieJar {
    
    private val cookieStore = ConcurrentHashMap<String, MutableList<Cookie>>()
    private val prefs: SharedPreferences = context.getSharedPreferences("cookie_prefs", Context.MODE_PRIVATE)
    
    init {
        loadCookiesFromPrefs()
    }
    
    override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
        val urlHost = url.host
        
        // Filter out expired cookies
        val validCookies = cookies.filter { it.expiresAt > System.currentTimeMillis() }
        
        if (validCookies.isNotEmpty()) {
            cookieStore[urlHost] = validCookies.toMutableList()
            saveCookiesToPrefs(urlHost, validCookies)
        }
    }
    
    override fun loadForRequest(url: HttpUrl): List<Cookie> {
        val urlHost = url.host
        
        // Return cookies for this host
        val cookies = cookieStore[urlHost] ?: return emptyList()
        
        // Filter out expired cookies
        val validCookies = cookies.filter { it.expiresAt > System.currentTimeMillis() }
        
        // Also check for cookies from parent domain (e.g., api.zonenews.io cookies should work for all subdomains)
        val parentDomainCookies = cookieStore.entries
            .filter { urlHost.endsWith(it.key) || it.key.endsWith(urlHost) }
            .flatMap { it.value }
            .filter { it.expiresAt > System.currentTimeMillis() }
        
        return (validCookies + parentDomainCookies).distinctBy { "${it.name}-${it.domain}" }
    }
    
    /**
     * Clear all stored cookies (useful for logout)
     * Clears both in-memory store and SharedPreferences
     */
    fun clearCookies() {
        cookieStore.clear()
        prefs.edit().clear().apply()
    }
    
    /**
     * Force clear cookies from memory (useful when SharedPreferences are cleared externally)
     * This ensures cookies don't persist in memory after being cleared from storage
     */
    fun forceClearMemoryCookies() {
        cookieStore.clear()
    }
    
    /**
     * Get CSRF token from cookies
     * CSRF token is typically stored in a cookie by Flask-JWT-Extended
     * Common names: csrf_access_token, csrf_refresh_token, csrf_token, csrf-token
     */
    fun getCsrfToken(): String? {
        // Check all stored cookies for CSRF token
        cookieStore.values.flatten().forEach { cookie ->
            val name = cookie.name.lowercase()
            // Check for common CSRF token cookie names
            if (name == "csrf_access_token" || 
                name == "csrf_refresh_token" || 
                name == "csrf_token" || 
                name == "csrf-token" ||
                name.contains("csrf")) {
                val value = cookie.value
                // Make sure the cookie hasn't expired
                if (cookie.expiresAt > System.currentTimeMillis() && value.isNotEmpty()) {
                    return value
                }
            }
        }
        return null
    }
    
    /**
     * Check if there are any valid (non-expired) authentication cookies
     * Used to determine if auto-login should be attempted
     */
    fun hasValidCookies(): Boolean {
        val currentTime = System.currentTimeMillis()
        cookieStore.values.flatten().forEach { cookie ->
            // Check if cookie is not expired and has a value
            if (cookie.expiresAt > currentTime && cookie.value.isNotEmpty()) {
                return true
            }
        }
        return false
    }
    
    private fun saveCookiesToPrefs(host: String, cookies: List<Cookie>) {
        val editor = prefs.edit()
        val cookieStrings = cookies.joinToString("|") { cookie ->
            "${cookie.name}=${cookie.value};domain=${cookie.domain};path=${cookie.path};expires=${cookie.expiresAt};secure=${cookie.secure};httpOnly=${cookie.httpOnly};hostOnly=${cookie.hostOnly}"
        }
        editor.putString("cookies_$host", cookieStrings)
        editor.apply()
    }
    
    private fun loadCookiesFromPrefs() {
        val allEntries = prefs.all
        allEntries.forEach { (key, value) ->
            if (key.startsWith("cookies_")) {
                val host = key.removePrefix("cookies_")
                try {
                    val cookieStrings = (value as String).split("|")
                    val cookies = cookieStrings.mapNotNull { parseCookie(it, host) }
                    if (cookies.isNotEmpty()) {
                        cookieStore[host] = cookies.toMutableList()
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }
    
    private fun parseCookie(cookieString: String, defaultDomain: String): Cookie? {
        return try {
            val parts = cookieString.split(";")
            var name = ""
            var value = ""
            var domain = defaultDomain
            var path = "/"
            var expiresAt = Long.MAX_VALUE
            var secure = false
            var httpOnly = false
            var hostOnly = false
            
            parts.forEach { part ->
                val trimmed = part.trim()
                when {
                    trimmed.contains("=") -> {
                        val (key, valPart) = trimmed.split("=", limit = 2)
                        when (key.lowercase()) {
                            "domain" -> domain = valPart
                            "path" -> path = valPart
                            "expires" -> expiresAt = valPart.toLongOrNull() ?: Long.MAX_VALUE
                            else -> {
                                if (name.isEmpty()) {
                                    name = key
                                    value = valPart
                                }
                            }
                        }
                    }
                    trimmed.lowercase() == "secure" -> secure = true
                    trimmed.lowercase() == "httponly" -> httpOnly = true
                    trimmed.lowercase() == "hostonly" -> hostOnly = true
                }
            }
            
            Cookie.Builder()
                .name(name)
                .value(value)
                .domain(domain)
                .path(path)
                .expiresAt(expiresAt)
                .apply {
                    if (secure) secure()
                    if (httpOnly) httpOnly()
                    if (hostOnly) hostOnlyDomain(domain)
                }
                .build()
        } catch (e: Exception) {
            null
        }
    }
}

