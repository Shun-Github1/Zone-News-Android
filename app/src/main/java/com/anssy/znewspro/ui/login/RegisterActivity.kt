package com.anssy.znewspro.ui.login

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import com.anssy.znewspro.R
import com.anssy.znewspro.base.BaseActivity
import com.anssy.znewspro.databinding.ActivityRegisterBinding
import com.anssy.znewspro.ui.MainActivity
import com.anssy.znewspro.utils.SharedPreferenceUtils
import com.anssy.znewspro.utils.ToastUtils
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.jaeger.library.StatusBarUtil
import com.anssy.znewspro.utils.SystemDialogUtils

/**
 * @Description TODO
 * @Author yulu
 * @CreateTime 2025年07月03日 15:52:57
 */

class RegisterActivity : BaseActivity() {
    private lateinit var mViewBindIng: ActivityRegisterBinding
    private lateinit var auth: FirebaseAuth
    
    companion object {
        val TAG = RegisterActivity::class.simpleName
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mViewBindIng = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(mViewBindIng.root)
        applyStatusBarStyle()
        initFirebase()
        initView()
    }

    private fun initFirebase() {
        auth = Firebase.auth
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
        Log.d(TAG, "registerWithEmailAndPassword: Starting registration for email: $email")
        
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    // Registration successful
                    Log.d(TAG, "createUserWithEmailAndPassword:success")
                    val user = auth.currentUser
                    
                    // Update user profile with display name
                    user?.let { firebaseUser ->
                        val profileUpdates = com.google.firebase.auth.UserProfileChangeRequest.Builder()
                            .setDisplayName(username)
                            .build()
                        
                        firebaseUser.updateProfile(profileUpdates)
                            .addOnCompleteListener { updateTask ->
                                if (updateTask.isSuccessful) {
                                    Log.d(TAG, "User profile updated with display name: $username")
                                } else {
                                    Log.w(TAG, "Failed to update user profile", updateTask.exception)
                                }
                            }
                    }
                    
                    // Show success message and navigate to main activity
                    SystemDialogUtils.showSuccessMessage(this, getString(R.string.register_success))
                    SharedPreferenceUtils.saveBoolean(mContext, "isLogin", true)
                    
                    // Navigate to MainActivity
                    val intent = Intent(this, MainActivity::class.java)
                    startActivity(intent)
                    finish()
                } else {
                    // Registration failed
                    Log.w(TAG, "createUserWithEmailAndPassword:failure", task.exception)
                    val errorMessage = when {
                        task.exception?.message?.contains("invalid-email") == true -> 
                            getString(R.string.register_error_email_invalid)
                        task.exception?.message?.contains("email-already-in-use") == true -> 
                            getString(R.string.register_error_email_already_exists)
                        task.exception?.message?.contains("weak-password") == true -> 
                            getString(R.string.register_error_password_weak)
                        else -> getString(R.string.register_error_general)
                    }
                    SystemDialogUtils.showErrorMessage(this, errorMessage)
                }
            }
    }

    private fun setupToolbar() {
        val toolbar = mViewBindIng.topLayout.toolbar
        toolbar.title = getString(R.string.register_activity_title)
        toolbar.setNavigationOnClickListener { finish() }
    }
}