package com.anssy.znewspro.base

import android.app.Application
import android.content.Context
import com.anssy.znewspro.R
import com.anssy.znewspro.utils.MVUtils
import com.anssy.znewspro.utils.ThemeManager
import com.anssy.znewspro.utils.network.NetworkApi
import com.anssy.znewspro.utils.network.NetworkRequiredInfo
import com.hjq.language.MultiLanguages
import com.kongzue.dialogx.DialogX
import com.scwang.smartrefresh.header.MaterialHeader
import com.scwang.smartrefresh.layout.SmartRefreshLayout
import com.scwang.smartrefresh.layout.footer.ClassicsFooter
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
                layout.setPrimaryColorsId(R.color.back_color, R.color.black)
                MaterialHeader(context)
            }
            SmartRefreshLayout.setDefaultRefreshFooterCreator { context, layout ->

                ClassicsFooter(context).setAccentColorId(R.color.black).setDrawableSize(20.0f)
            }
        }
    }



    override fun attachBaseContext(base: Context) {
        // 绑定语种
        super.attachBaseContext(MultiLanguages.attach(base))
    }

    override fun onCreate() {
        super.onCreate()
        
        // Apply saved theme before any UI is created
        ThemeManager.applySavedTheme(this)
        
        NetworkApi.init(NetworkRequiredInfo(this))
        MVUtils.instance.getMvUtils(applicationContext)
        instances  = this
        DialogX.init(this)
        AutoSize.initCompatMultiProcess(this)
        CrashReport.initCrashReport(applicationContext,"d76448da42",true)
        MultiLanguages.init(this);
    }

}