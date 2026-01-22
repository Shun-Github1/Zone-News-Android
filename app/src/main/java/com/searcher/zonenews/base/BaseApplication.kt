package com.searcher.zonenews.base

import android.app.Application
import android.content.Context
import com.searcher.zonenews.R
import com.searcher.zonenews.utils.AppIconManager
import com.searcher.zonenews.utils.MVUtils
import com.searcher.zonenews.utils.NotificationManager
import com.searcher.zonenews.utils.ThemeManager
import com.searcher.zonenews.utils.network.NetworkApi
import com.searcher.zonenews.utils.network.NetworkRequiredInfo
import com.google.firebase.Firebase
import com.google.firebase.messaging.FirebaseMessaging


import com.scwang.smartrefresh.header.MaterialHeader
import com.scwang.smartrefresh.layout.SmartRefreshLayout
import com.searcher.zonenews.ui.components.CustomLoadMoreFooter
import com.google.firebase.crashlytics.FirebaseCrashlytics
import dagger.hilt.android.HiltAndroidApp



/**
 * @Description TODO
 * @Author yulu
 * @CreateTime 2025年06月25日 10:01:36
 */

@HiltAndroidApp
class BaseApplication : Application() {
    companion object{
        var instances: BaseApplication?=null
        init {
            SmartRefreshLayout.setDefaultRefreshHeaderCreator { context, layout ->
                layout.setPrimaryColorsId(R.color.surface_tertiary, R.color.black)
                MaterialHeader(context)
            }
            SmartRefreshLayout.setDefaultRefreshFooterCreator { context, layout ->
                CustomLoadMoreFooter(context).apply {
                    setAccentColorId(R.color.black)
                    setDrawableSize(20.0f)
                    setDrawableMarginRight(20f)
                }
            }
        }
    }



    override fun attachBaseContext(base: Context) {
        super.attachBaseContext(base)
    }

    override fun onCreate() {
        super.onCreate()
        
        // Initialize MMKV for multi-process and background support
        com.tencent.mmkv.MMKV.initialize(this)
        
        // Apply saved theme before any UI is created
        ThemeManager.applySavedTheme(this)
        
        // Initialize app icon state
        AppIconManager.initializeAppIcon(this)
        
        // Initialize Firebase Messaging
        initializeFirebaseMessaging()
        
        NetworkApi.init(NetworkRequiredInfo(this))
        MVUtils.instance.getMvUtils(applicationContext)
        instances  = this


        // Firebase Crashlytics initialization (replaces Bugly for 16KB compatibility)
        FirebaseCrashlytics.getInstance().isCrashlyticsCollectionEnabled = true
    }

    private fun initializeFirebaseMessaging() {
        // Create notification channel
        NotificationManager.createNotificationChannel(this)
        
        // Get FCM token
        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (!task.isSuccessful) {
                return@addOnCompleteListener
            }
            
            // Get new FCM registration token
            val token = task.result
            android.util.Log.d("FCM", "FCM Registration Token: $token")
            
            // TODO: Send token to your server if needed
            // sendTokenToServer(token)
        }
    }

}
