package com.anssy.znewspro.base

import android.content.Context
import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import com.anssy.znewspro.utils.foresult.IMsa
import com.anssy.znewspro.utils.foresult.msa
import dagger.hilt.android.AndroidEntryPoint
import kotlin.math.abs

@AndroidEntryPoint
open class BaseFragment : Fragment(),IMsa by msa() {
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
  fun changeScoreToMargin(score: Double,mProgressWidth:Int): Double {
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

    open fun initData(){

    }
    open fun  destroyData(){

    }
    val currentLan: String
        get() {
            val locale = resources.configuration.locale
            return locale.language
        }
}