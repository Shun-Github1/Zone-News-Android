package com.searcher.zonenews.base

import android.content.Context
import android.os.Build
import android.os.Bundle
import android.view.View
import androidx.core.os.ConfigurationCompat
import androidx.fragment.app.Fragment
import com.searcher.zonenews.utils.foresult.IMsa
import com.searcher.zonenews.utils.foresult.msa
import dagger.hilt.android.AndroidEntryPoint
import kotlin.math.abs

@AndroidEntryPoint
open class BaseFragment : Fragment(), IMsa by msa() {
    var mContext: Context? = null

    override fun onAttach(context: Context) {
        super.onAttach(context)
        mContext = context
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initManageStartActivity()
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initData()
    }

    override fun onDestroy() {
        super.onDestroy()
        destroyData()
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

    open fun initData() {
    }
    
    open fun destroyData() {
    }
    
    // Deprecated: Use LanguageManager instead
    @Deprecated("Use LanguageManager.getCurrentLanguageCode() instead")
    val currentLan: String
        get() {
            val locale = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                ConfigurationCompat.getLocales(resources.configuration)[0]
            } else {
                @Suppress("DEPRECATION")
                resources.configuration.locale
            }
            return locale?.language ?: "en"
        }
}
