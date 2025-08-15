package com.anssy.znewspro.utils

import android.annotation.SuppressLint
import android.content.Context
import android.view.Gravity
import android.view.View
import android.widget.Toast

@SuppressLint("StaticFieldLeak")
object ToastUtils {
    private var toast: Toast? = null
    private var view: View? = null
    private fun getToast(context: Context) {
        if (toast == null) {
            toast = Toast(context)
        }
        if (view == null) {
            view = Toast.makeText(context, "", Toast.LENGTH_SHORT).view
        }
        toast!!.view = view
    }
    @JvmStatic
    fun showShortToast(context: Context, msg: CharSequence) {
        showToast(context.applicationContext, msg, 0)
    }

    fun showShortToast(context: Context, resId: Int) {
        showToast(context.applicationContext, resId, 0)
    }

    fun showLongToast(context: Context, msg: CharSequence) {
        showToast(context.applicationContext, msg, 0)
    }

    fun showLongToast(context: Context, resId: Int) {
        showToast(context.applicationContext, resId, 1)
    }

    private fun showToast(context: Context, msg: CharSequence, duration: Int) {
        try {
            getToast(context)
            toast!!.setText(msg)
            toast!!.duration = duration
            toast!!.setGravity(Gravity.CENTER, 0,0)
            toast!!.show()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun showToast(context: Context, resId: Int, duration: Int) {
        if (resId != 0) {
            try {
                getToast(context)
                toast!!.setText(resId)
                toast!!.duration = duration
                toast!!.setGravity(Gravity.CENTER, 0,0)
                toast!!.show()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun cancelToast() {
        val toast2 = toast
        toast2?.cancel()
    }
}