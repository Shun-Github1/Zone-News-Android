package com.anssy.znewspro.ui.login

import android.content.Intent
import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import android.widget.Toast
import androidx.activity.viewModels
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.Credential
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.ClearCredentialException
import androidx.credentials.exceptions.GetCredentialException
import androidx.lifecycle.lifecycleScope
import com.anssy.znewspro.R
import com.anssy.znewspro.base.BaseActivity
import com.anssy.znewspro.databinding.ActivityLoginBinding
import com.anssy.znewspro.model.LoginModel
import com.anssy.znewspro.ui.MainActivity
import com.anssy.znewspro.utils.SharedPreferenceUtils
import com.anssy.znewspro.utils.ToastUtils
import com.facebook.AccessToken
import com.facebook.CallbackManager
import com.facebook.FacebookCallback
import com.facebook.FacebookException
import com.facebook.login.LoginResult
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential.Companion.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
import com.google.firebase.Firebase
import com.google.firebase.auth.FacebookAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.auth
import com.jaeger.library.StatusBarUtil
import com.kongzue.dialogx.dialogs.WaitDialog
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlin.getValue

/**
 * @Description 登录界面
 * @Author yulu
 * @CreateTime 2025年07月03日 15:32:28
 */

class LoginActivity :BaseActivity() {
    private lateinit var mViewBinding:ActivityLoginBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var credentialManager: CredentialManager
    private lateinit var callbackManager: CallbackManager
    private val loginModel: LoginModel by viewModels()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mViewBinding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(mViewBinding.root)
        if (SharedPreferenceUtils.getBoolean(mContext,"isLogin")){
            val intent = Intent(mContext,MainActivity::class.java)
            startActivity(intent)
            finish()
        }else{
            initView()
            auth = Firebase.auth
            credentialManager = CredentialManager.Companion.create(this)
            callbackManager = CallbackManager.Factory.create()
            StatusBarUtil.setTranslucentForImageView(this,0,null)
            initFacebookAuth()
            initModel()
        }
    }


    private fun initFacebookAuth() {
        mViewBinding.loginButton.setReadPermissions(getString(R.string.email), "public_profile")
        mViewBinding.loginButton.registerCallback(
            callbackManager,
            object : FacebookCallback<LoginResult> {
                override fun onSuccess(result: LoginResult) {
                        Log.d(TAG, "facebook:onSuccess:$result")
                    handleFacebookAccessToken(result.accessToken)
                }

                override fun onCancel() {
                    Log.d(TAG, "facebook:onCancel")
                }

                override fun onError(error: FacebookException) {
                    Log.d(TAG, "facebook:onError", error)
                }
            },
        )
    }
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        // Pass the activity result back to the Facebook SDK
        callbackManager.onActivityResult(requestCode, resultCode, data)
    }

    companion object{
        val TAG = this::class.simpleName
    }

    /**
     * FaceBook登录
     */
    private fun handleFacebookAccessToken(token: AccessToken) {
        Log.d(TAG, "handleFacebookAccessToken:$token")
        WaitDialog.show(getString(R.string.wait_logging_in))
        val credential = FacebookAuthProvider.getCredential(token.token)
        auth.signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->
                WaitDialog.dismiss()
                if (task.isSuccessful) {
                    // Sign in success, update UI with the signed-in user's information
                    Log.d(TAG, "signInWithCredential:success")
                    val user = auth.currentUser
                    SharedPreferenceUtils.saveBoolean(mContext,"thirdLogin",true)
                    ToastUtils.showShortToast(mContext!!,getString(R.string.email_prefix, user?.email ?: ""))
                } else {
                    // If sign in fails, display a message to the user.
                    Log.w(TAG, "signInWithCredential:failure", task.exception)
                     Toast.makeText(
                        baseContext,
                        getString(R.string.auth_failed),
                        Toast.LENGTH_SHORT,
                    ).show()

                }
            }
    }

    private suspend  fun initGoogleAuth(){
        WaitDialog.show(getString(R.string.wait_logging_in))
        // Instantiate a Google sign-in request
        val googleIdOption = GetGoogleIdOption.Builder()
            // Your server's client ID, not your Android client ID.
            .setServerClientId("1027106789501-199b76gsugq2aejldiq6hf8prqudv9i7.apps.googleusercontent.com")
            // Only show accounts previously used to sign in.
            .setAutoSelectEnabled(true)
            .setNonce(System.currentTimeMillis().toString())
            .build()
        val request: GetCredentialRequest = GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .build()

 //Create the Credential Manager request
        coroutineScope {
            try {
                val result = credentialManager.getCredential(
                    request = request,
                    context = this@LoginActivity,
                )
                handleSignIn(result.credential)
            } catch (e: GetCredentialException) {
                ToastUtils.showShortToast(mContext!!,getString(R.string.login_failed))
                WaitDialog.dismiss()
                 e.printStackTrace()
            }
    }
    }

    private fun handleSignIn(credential: Credential) {
        // Check if credential is of type Google ID
        if (credential is CustomCredential && credential.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
            // Create Google ID Token
            val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)

            // Sign in to Firebase with using the token
            firebaseAuthWithGoogle(googleIdTokenCredential.idToken)
        } else {
            ToastUtils.showShortToast(mContext!!,getString(R.string.login_failed))
            WaitDialog.dismiss()
            Log.w("xxx", "Credential is not of type Google ID!")
        }
    }
    private fun initView() {
        mViewBinding.googleLoginLayout.setOnClickListener {
            lifecycleScope.launch {
                initGoogleAuth()
            }
        }
        // Trigger hidden Facebook LoginButton from custom row
        val facebookRow = mViewBinding.root.findViewById<android.view.View>(R.id.facebook_login_layout)
        facebookRow?.setOnClickListener { mViewBinding.loginButton.performClick() }

        // Email/password login
        mViewBinding.loginBtn.setOnClickListener {
            val email = mViewBinding.emailEt.text?.toString()?.trim() ?: ""
            val password = mViewBinding.passEt.text?.toString()?.trim() ?: ""
            if (TextUtils.isEmpty(email)){
                ToastUtils.showShortToast(mContext!!,getString(R.string.login_enter_account))
                return@setOnClickListener
            }
            if (TextUtils.isEmpty(password)){
                ToastUtils.showShortToast(mContext!!,getString(R.string.login_enter_password))
                return@setOnClickListener
            }
            com.kongzue.dialogx.dialogs.WaitDialog.show(getString(R.string.login_logging_in))
            loginModel.loginApp(email,password)
        }

        // Go to register
        mViewBinding.registerTv.setOnClickListener {
            val intent = Intent(mContext, RegisterActivity::class.java)
            startActivity(intent)
        }
    }

    private fun firebaseAuthWithGoogle(idToken: String) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        auth.signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    // Sign in success, update UI with the signed-in user's information
                    val user = auth.currentUser
                    WaitDialog.dismiss()
                    user?.email.let {
                        if (!TextUtils.isEmpty(user?.email)){
                            ToastUtils.showShortToast(mContext!!,getString(R.string.email_prefix, user?.email ?: ""))
                            SharedPreferenceUtils.saveBoolean(mContext,"thirdLogin",true)
                        }else{
                            ToastUtils.showShortToast(mContext!!,getString(R.string.email_empty))
                        }
                    }

                } else {
                    ToastUtils.showShortToast(mContext!!,getString(R.string.login_failed))
                    WaitDialog.dismiss()
                    // If sign in fails, display a message to the user
                    Log.w("xxx", "signInWithCredential:failure", task.exception)

                }
            }
    }

    private fun initModel(){
        loginModel.loginEntry.observe(this){
            if (it.code == com.anssy.znewspro.utils.Constants.SUCCESS_CODE){
                com.kongzue.dialogx.dialogs.TipDialog.show(getString(R.string.login_success), com.kongzue.dialogx.dialogs.WaitDialog.TYPE.SUCCESS)
                SharedPreferenceUtils.saveString(mContext,"token",it.data.access_token)
                SharedPreferenceUtils.saveBoolean(mContext,"isLogin",true)
                val intent = Intent(mContext,MainActivity::class.java)
                startActivity(intent)
                finishAffinity()
            }else{
                com.kongzue.dialogx.dialogs.WaitDialog.dismiss()
                ToastUtils.showShortToast(mContext!!,it.msg)
            }
        }
    }
}