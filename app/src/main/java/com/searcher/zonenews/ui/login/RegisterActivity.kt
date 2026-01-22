package com.searcher.zonenews.ui.login

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import androidx.activity.viewModels
import com.searcher.zonenews.R
import com.searcher.zonenews.base.BaseActivity
import com.searcher.zonenews.databinding.ActivityRegisterBinding
import com.searcher.zonenews.model.LoginModel
import com.searcher.zonenews.ui.MainActivity
import com.searcher.zonenews.utils.Constants
import com.searcher.zonenews.utils.SharedPreferenceUtils
import com.searcher.zonenews.utils.ToastUtils
import com.jaeger.library.StatusBarUtil
import com.searcher.zonenews.utils.SystemDialogUtils

/**
 * @Description TODO
 * @Author yulu
 * @CreateTime 2025年07月03日 15:52:57
 */

class RegisterActivity : BaseActivity() {
    private lateinit var mViewBindIng: ActivityRegisterBinding
    private val loginModel: LoginModel by viewModels()
    
    companion object {
        val TAG = RegisterActivity::class.simpleName
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mViewBindIng = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(mViewBindIng.root)
        applyStatusBarStyle()
        initModel()
        initView()
        
        // Add fade-in animation to match home page
        @Suppress("DEPRECATION")
        overridePendingTransition(R.anim.fade_in, R.anim.fade_out)
    }

    private fun initModel() {
        // Observe registration response
        loginModel.outLoginEntry.observe(this) { response ->
            if (response == null) {
                Log.w(TAG, "Registration response is null")
                return@observe
            }
            
            Log.d(TAG, "Registration response - code: ${response.code}, msg: ${response.msg}")
            
            if (response.code != null && response.code == Constants.SUCCESS_CODE) {
                // Registration successful
                Log.d(TAG, "Registration successful")
                SystemDialogUtils.dismissLoadingDialog()
                SystemDialogUtils.showSuccessMessage(this, getString(R.string.register_success))
                
                // Mark as logged in (backend returns JWT cookie automatically)
                SharedPreferenceUtils.saveBoolean(mContext, "isLogin", true)
                SharedPreferenceUtils.saveBoolean(mContext, "thirdLogin", false) // Manual account, not third-party
                
                // Navigate to MainActivity
                val intent = Intent(this, MainActivity::class.java)
                startActivity(intent)
                finishAffinity()
                @Suppress("DEPRECATION")
                overridePendingTransition(R.anim.fade_in, R.anim.fade_out)
            } else {
                // Registration failed
                Log.w(TAG, "Registration failed - code: ${response.code}, msg: ${response.msg}")
                SystemDialogUtils.dismissLoadingDialog()
                
                // Display specific error messages based on response code
                val errorMessage = when (response.code) {
                    409 -> response.msg ?: getString(R.string.register_error_email_already_exists) // Email/username already taken
                    401 -> response.msg ?: getString(R.string.register_error_general) // Registration failed
                    else -> response.msg ?: getString(R.string.register_error_general)
                }
                SystemDialogUtils.showErrorMessage(this, errorMessage)
            }
        }
    }

    @SuppressLint("SetTextI18n")
    private fun initView() {
        // Setup MaterialToolbar with navigation and title
        setupToolbar()
        
        mViewBindIng.registerBtn.setOnClickListener {
            val email = mViewBindIng.emailEt.text?.toString()?.trim() ?: ""
            val username = mViewBindIng.nameEt.text?.toString()?.trim() ?: ""
            val password = mViewBindIng.passEt.text?.toString()?.trim() ?: ""
            val confirmPassword = mViewBindIng.passAgainEt.text?.toString()?.trim() ?: ""
            
            if (TextUtils.isEmpty(email)){
                ToastUtils.showShortToast(mContext!!,getString(R.string.register_error_email_empty))
                return@setOnClickListener
            }
            if (TextUtils.isEmpty(username)){
                ToastUtils.showShortToast(mContext!!,getString(R.string.register_error_username_empty))
                return@setOnClickListener
            }
            if (TextUtils.isEmpty(password)){
                ToastUtils.showShortToast(mContext!!,getString(R.string.register_error_password_empty))
                return@setOnClickListener
            }
            if (TextUtils.isEmpty(confirmPassword)){
                ToastUtils.showShortToast(mContext!!,getString(R.string.register_error_password_again_empty))
                return@setOnClickListener
            }
            if (!TextUtils.equals(password, confirmPassword)){
                ToastUtils.showShortToast(mContext!!,getString(R.string.register_error_passwords_mismatch))
                return@setOnClickListener
            }
            if (password.length < 6) {
                ToastUtils.showShortToast(mContext!!, getString(R.string.register_error_password_weak))
                return@setOnClickListener
            }
            
            registerWithEmailAndPassword(email, password, username)
        }
    }

    private fun registerWithEmailAndPassword(email: String, password: String, username: String) {
        Log.d(TAG, "registerWithEmailAndPassword: Starting registration for email: $email, username: $username")
        
        // Show loading dialog
        SystemDialogUtils.showLoadingDialog(this, getString(R.string.register_status_registering))
        
        // Call backend API for registration
        loginModel.registerApp(email, username, password)
    }

    private fun setupToolbar() {
        val toolbar = mViewBindIng.topLayout.toolbar
        toolbar.title = getString(R.string.register_activity_title)
        toolbar.setNavigationOnClickListener { 
            finish()
            // Add fade-out animation when going back
            @Suppress("DEPRECATION")
            overridePendingTransition(R.anim.fade_in, R.anim.fade_out)
        }
    }
    
    override fun finish() {
        super.finish()
        // Add fade-out animation when activity finishes
        @Suppress("DEPRECATION")
        overridePendingTransition(R.anim.fade_in, R.anim.fade_out)
    }
}
