package com.anssy.znewspro.base


import android.content.Context
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import com.anssy.znewspro.R
import com.anssy.znewspro.utils.ThemeManager
import com.anssy.znewspro.utils.foresult.IMsa
import com.anssy.znewspro.utils.foresult.msa
import com.hjq.language.MultiLanguages
import com.jaeger.library.StatusBarUtil
import dagger.hilt.android.AndroidEntryPoint
import kotlin.math.abs

@AndroidEntryPoint
open class BaseActivity :AppCompatActivity(), IMsa by msa() {
    protected var mContext:Context? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mContext = this
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        initManageStartActivity()
    }



    override fun attachBaseContext(newBase: Context) {
        // 绑定语种
        super.attachBaseContext(MultiLanguages.attach(newBase))
    }

    override fun onDestroy() {
        super.onDestroy()

    }
    

    fun back(view:View){
        finish()
    }

    /**
     * Apply consistent status bar style based on the current theme.
     * Uses dark icons on light background in light mode, and light icons on dark background in dark mode.
     * The background color is always R.color.status_bar_color for consistency.
     */
    fun applyStatusBarStyle() {
        if (ThemeManager.isDarkModeActive(this)) {
            StatusBarUtil.setDarkMode(this)
        } else {
            StatusBarUtil.setLightMode(this)
        }
        StatusBarUtil.setColor(this, getColor(R.color.status_bar_color), 0)
    }
    /**
     * 转换分值为 marginStart
     */
    fun changeScoreToMargin(score: Double, mProgressWidth: Int): Double {
        if (!(score >= -1.0 && score <= 1.0)) {
            return 0.0
        }
        val marginWidth: Double
        if (score < 0) {
            val percent = abs(score)
            marginWidth = (1 - percent) * (mProgressWidth / 2)

        } else if (score == 0.00) {
            val percent = 0.5
            marginWidth = percent * mProgressWidth
        } else {
            val percent = score / 2 + 0.5
            marginWidth = percent * mProgressWidth
        }
        return marginWidth
    }

}