package com.anssy.znewspro.ui.login

import android.annotation.SuppressLint
import android.os.Bundle
import android.text.TextUtils
import androidx.activity.viewModels
import com.anssy.znewspro.utils.Constants
import com.anssy.znewspro.R
import com.anssy.znewspro.base.BaseActivity
import com.anssy.znewspro.databinding.ActivityRegisterBinding
import com.anssy.znewspro.model.LoginModel
import com.anssy.znewspro.utils.ToastUtils
import com.jaeger.library.StatusBarUtil
import com.kongzue.dialogx.dialogs.TipDialog
import com.kongzue.dialogx.dialogs.WaitDialog

/**
 * @Description TODO
 * @Author yulu
 * @CreateTime 2025年07月03日 15:52:57
 */

class RegisterActivity :BaseActivity() {
    private lateinit var mViewBindIng:ActivityRegisterBinding
    private val loginModel:LoginModel by viewModels()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mViewBindIng = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(mViewBindIng.root)
        applyStatusBarStyle()
        initView()
        initModel()
    }

    private fun initModel() {
        loginModel.outLoginEntry.observe(this){
            if (it!=null){
                if (it.code== Constants.SUCCESS_CODE){
                    TipDialog.show(getString(R.string.register_success),WaitDialog.TYPE.SUCCESS)
                }else{
                    TipDialog.show(it.msg,WaitDialog.TYPE.ERROR)
                }
            }else{
                WaitDialog.dismiss()
            }
        }
    }

    @SuppressLint("SetTextI18n")
    private fun initView() {
        mViewBindIng.topLayout.titleTv.text = getString(R.string.register_activity_title)
        mViewBindIng.registerBtn.setOnClickListener {
            if (TextUtils.isEmpty(mViewBindIng.emailEt.text)){
                ToastUtils.showShortToast(mContext!!,getString(R.string.register_error_email_empty))
                return@setOnClickListener
            }
            if (TextUtils.isEmpty(mViewBindIng.nameEt.text)){
                ToastUtils.showShortToast(mContext!!,getString(R.string.register_error_username_empty))
                return@setOnClickListener
            }
            if (TextUtils.isEmpty(mViewBindIng.passEt.text)){
                ToastUtils.showShortToast(mContext!!,getString(R.string.register_error_password_empty))
                return@setOnClickListener
            }
            if (TextUtils.isEmpty(mViewBindIng.passAgainEt.text)){
                ToastUtils.showShortToast(mContext!!,getString(R.string.register_error_password_again_empty))
                return@setOnClickListener
            }
            if (!TextUtils.equals(mViewBindIng.passEt.text,mViewBindIng.passAgainEt.text)){
                ToastUtils.showShortToast(mContext!!,getString(R.string.register_error_passwords_mismatch))
                return@setOnClickListener
            }
            WaitDialog.show(getString(R.string.register_status_registering))
            loginModel.registerApp(mViewBindIng.emailEt.text.toString(),mViewBindIng.nameEt.text.toString(),mViewBindIng.passEt.text.toString())
        }
    }
}