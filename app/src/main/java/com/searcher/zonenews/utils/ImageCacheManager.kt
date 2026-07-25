package com.searcher.zonenews.utils

import android.graphics.Bitmap
import android.util.LruCache

/**
 * Premium Image Cache Manager
 * Provides a persistent, app-wide manual memory layer for high-resolution bitmaps.
 * This ensures that news images never flicker or "pop-in" once they have been viewed.
 * Works in tandem with Glide but provides an even more reliable, manual control layer.
 */
object ImageCacheManager {
    // 96MB manual bitmap cache (increased for premium feel)
    private val memoryCache = object : LruCache<String, Bitmap>(1024 * 1024 * 96) {
        override fun sizeOf(key: String, value: Bitmap): Int {
            return value.byteCount
        }
    }

    /**
     * Store a bitmap in the persistent cache
     */
    fun put(url: String?, bitmap: Bitmap) {
        if (url.isNullOrEmpty()) return
        memoryCache.put(url, bitmap)
    }

    /**
     * Retrieve a bitmap from the persistent cache
     */
    fun get(url: String?): Bitmap? {
        if (url.isNullOrEmpty()) return null
        return memoryCache.get(url)
    }
    
    /**
     * Clear the cache if needed (e.g. on logout or memory pressure)
     */
    fun clear() {
        memoryCache.evictAll()
    }
}
