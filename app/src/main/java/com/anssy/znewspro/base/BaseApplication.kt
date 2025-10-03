package com.anssy.znewspro.base

import android.app.Application
import android.content.Context
import com.anssy.znewspro.R
import com.anssy.znewspro.utils.AppIconManager
import com.anssy.znewspro.utils.MVUtils
import com.anssy.znewspro.utils.NotificationManager
import com.anssy.znewspro.utils.ThemeManager
import com.anssy.znewspro.utils.network.NetworkApi
import com.anssy.znewspro.utils.network.NetworkRequiredInfo
import com.google.firebase.Firebase
import com.google.firebase.messaging.FirebaseMessaging


import com.scwang.smartrefresh.header.MaterialHeader
import com.scwang.smartrefresh.layout.SmartRefreshLayout
import com.anssy.znewspro.ui.components.CustomLoadMoreFooter
import com.tencent.bugly.crashreport.CrashReport
import dagger.hilt.android.HiltAndroidApp
import me.jessyan.autosize.AutoSize


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
        
        // Apply saved theme before any UI is created
        ThemeManager.applySavedTheme(this)
        
        // Initialize app icon state
        AppIconManager.initializeAppIcon(this)
        
        // Initialize Firebase Messaging
        initializeFirebaseMessaging()
        
        NetworkApi.init(NetworkRequiredInfo(this))
        MVUtils.instance.getMvUtils(applicationContext)
        instances  = this

        AutoSize.initCompatMultiProcess(this)
        CrashReport.initCrashReport(applicationContext,"d76448da42",true)
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