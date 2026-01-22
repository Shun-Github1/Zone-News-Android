package com.searcher.zonenews.utils.glide

import android.content.Context
import com.bumptech.glide.Glide
import com.bumptech.glide.GlideBuilder
import com.bumptech.glide.Registry
import com.bumptech.glide.annotation.GlideModule
import com.bumptech.glide.integration.okhttp3.OkHttpUrlLoader
import com.bumptech.glide.load.engine.cache.InternalCacheDiskCacheFactory
import com.bumptech.glide.load.engine.cache.LruResourceCache
import com.bumptech.glide.load.model.GlideUrl
import com.bumptech.glide.module.AppGlideModule
import okhttp3.OkHttpClient
import java.io.InputStream
import java.util.concurrent.TimeUnit

@GlideModule
class MyAppGlideModule : AppGlideModule() {

    override fun applyOptions(context: Context, builder: GlideBuilder) {
        // Best Practice: Configure memory and disk cache limits
        // 20MB memory cache (adjust based on app needs)
        val memoryCacheSizeBytes = 1024 * 1024 * 20L
        builder.setMemoryCache(LruResourceCache(memoryCacheSizeBytes))

        // 100MB disk cache
        val diskCacheSizeBytes = 1024 * 1024 * 100L
        builder.setDiskCache(InternalCacheDiskCacheFactory(context, "image_cache", diskCacheSizeBytes))
    }

    override fun registerComponents(context: Context, glide: Glide, registry: Registry) {
        // Best Practice: Use OkHttp for network operations
        val client = OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .retryOnConnectionFailure(false)
            .build()

        registry.replace(
            GlideUrl::class.java,
            InputStream::class.java,
            OkHttpUrlLoader.Factory(client)
        )
    }

    // Disable manifest parsing to improve startup time
    override fun isManifestParsingEnabled(): Boolean = false
}
