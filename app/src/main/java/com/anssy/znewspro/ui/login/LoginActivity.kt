package com.anssy.znewspro.ui.login

import android.content.Intent
import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import android.widget.Toast
import androidx.activity.viewModels
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
import com.facebook.login.LoginManager
import com.facebook.login.LoginResult
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.Firebase
import com.google.firebase.auth.FacebookAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.auth
import com.jaeger.library.StatusBarUtil
import com.anssy.znewspro.utils.SystemDialogUtils
import kotlinx.coroutines.launch

/**
 * @Description 登录界面
 * @Author yulu
 * @CreateTime 2025年07月03日 15:32:28
 */

class LoginActivity : BaseActivity() {
    private lateinit var mViewBinding: ActivityLoginBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var googleSignInClient: GoogleSignInClient
    private lateinit var callbackManager: CallbackManager
    private val loginModel: LoginModel by viewModels()
    
    companion object {
        private const val RC_SIGN_IN = 9001
        val TAG = LoginActivity::class.simpleName
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mViewBinding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(mViewBinding.root)
        
        if (SharedPreferenceUtils.getBoolean(mContext, "isLogin")) {
            val intent = Intent(mContext, MainActivity::class.java)
            startActivity(intent)
            finish()
        } else {
            Log.d(TAG, "onCreate: User not logged in, initializing views")
            initView()
            initFirebase()
            initGoogleSignIn()
            initFacebookAuth()
            initModel()
        }
    }


    private fun initFacebookAuth() {
        // Initialize Facebook LoginManager
        LoginManager.getInstance().registerCallback(
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
                    ToastUtils.showShortToast(mContext!!, getString(R.string.login_failed))
                }
            }
        )
    }


    /**
     * Facebook登录处理
     */
    private fun handleFacebookAccessToken(token: AccessToken) {
        Log.d(TAG, "handleFacebookAccessToken:$token")
        
        val credential = FacebookAuthProvider.getCredential(token.token)
        auth.signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    // Sign in success, update UI with the signed-in user's information
                    Log.d(TAG, "signInWithCredential:success")
                    val user = auth.currentUser
                    user?.email.let {
                        if (!TextUtils.isEmpty(user?.email)) {
                            ToastUtils.showShortToast(mContext!!, getString(R.string.email_prefix, user?.email ?: ""))
                            SharedPreferenceUtils.saveBoolean(mContext, "thirdLogin", true)
                            SharedPreferenceUtils.saveBoolean(mContext, "isLogin", true)
                            
                            // Navigate to MainActivity
                            val intent = Intent(mContext, MainActivity::class.java)
                            startActivity(intent)
                            finish()
                        } else {
                            ToastUtils.showShortToast(mContext!!, getString(R.string.email_empty))
                        }
                    }
                } else {
                    // If sign in fails, display a message to the user
                    Log.w(TAG, "signInWithCredential:failure", task.exception)
                    ToastUtils.showShortToast(mContext!!, getString(R.string.login_failed))
                }
            }
    }

    private fun initFirebase() {
        auth = Firebase.auth
        callbackManager = CallbackManager.Factory.create()
        StatusBarUtil.setTranslucentForImageView(this, 0, null)
    }

    private fun initGoogleSignIn() {
        // Configure Google Sign In
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()

        googleSignInClient = GoogleSignIn.getClient(this, gso)
    }

    private fun signInWithGoogle() {
        Log.d(TAG, "signInWithGoogle: Starting Google Sign-in process")
        
        val signInIntent = googleSignInClient.signInIntent
        startActivityForResult(signInIntent, RC_SIGN_IN)
    }

    private fun signInWithFacebook() {
        Log.d(TAG, "signInWithFacebook: Starting Facebook Sign-in process")
        
        LoginManager.getInstance().logInWithReadPermissions(
            this,
            listOf("email", "public_profile")
        )
    }

    private fun signInWithEmailAndPassword(email: String, password: String) {
        Log.d(TAG, "signInWithEmailAndPassword: Starting email/password sign-in")
        
        // Check for special development admin access - use old API
        if (email == "admin" && password == "admin") {
            Log.d(TAG, "Development admin access - using old API method")
            SystemDialogUtils.showLoadingDialog(this, getString(R.string.login_logging_in))
            loginModel.loginApp(email, password)
            return
        }
        
        // Use Firebase for regular users
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    // Sign in success, update UI with the signed-in user's information
                    Log.d(TAG, "signInWithEmailAndPassword:success")
                    val user = auth.currentUser
                    user?.email.let {
                        if (!TextUtils.isEmpty(user?.email)) {
                            ToastUtils.showShortToast(mContext!!, getString(R.string.email_prefix, user?.email ?: ""))
                            SharedPreferenceUtils.saveBoolean(mContext, "isLogin", true)
                            
                            // Navigate to MainActivity
                            val intent = Intent(mContext, MainActivity::class.java)
                            startActivity(intent)
                            finish()
                        } else {
                            ToastUtils.showShortToast(mContext!!, getString(R.string.email_empty))
                        }
                    }
                } else {
                    // If sign in fails, display a message to the user
                    Log.w(TAG, "signInWithEmailAndPassword:failure", task.exception)
                    val errorMessage = when {
                        task.exception?.message?.contains("invalid-email") == true -> 
                            getString(R.string.login_error_invalid_email)
                        task.exception?.message?.contains("user-disabled") == true -> 
                            getString(R.string.login_error_user_disabled)
                        task.exception?.message?.contains("user-not-found") == true -> 
                            getString(R.string.login_error_user_not_found)
                        task.exception?.message?.contains("wrong-password") == true -> 
                            getString(R.string.login_error_wrong_password)
                        else -> getString(R.string.login_failed)
                    }
                    ToastUtils.showShortToast(mContext!!, errorMessage)
                }
            }
    }

    private fun initModel(){
        loginModel.loginEntry.observe(this){
            if (it.code == com.anssy.znewspro.utils.Constants.SUCCESS_CODE){
                SystemDialogUtils.showSuccessMessage(this, getString(R.string.login_success))
                SharedPreferenceUtils.saveString(mContext,"token",it.data.access_token)
                SharedPreferenceUtils.saveBoolean(mContext,"isLogin",true)
                val intent = Intent(mContext,MainActivity::class.java)
                startActivity(intent)
                finishAffinity()
            }else{
                SystemDialogUtils.dismissLoadingDialog()
                ToastUtils.showShortToast(mContext!!,it.msg)
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        // Pass the activity result back to the Facebook SDK
        callbackManager.onActivityResult(requestCode, resultCode, data)

        // Result returned from launching the Intent from GoogleSignInApi.getSignInIntent(...)
        if (requestCode == RC_SIGN_IN) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            try {
                // Google Sign In was successful, authenticate with Firebase
                val account = task.getResult(ApiException::class.java)!!
                Log.d(TAG, "firebaseAuthWithGoogle:" + account.id)
                firebaseAuthWithGoogle(account.idToken!!)
            } catch (e: ApiException) {
                // Google Sign In failed, update UI appropriately
                Log.w(TAG, "Google sign in failed", e)
                ToastUtils.showShortToast(mContext!!, getString(R.string.login_failed))
            }
        }
    }
    private fun initView() {
        Log.d(TAG, "initView: Setting up Google Sign-in button click listener")
        mViewBinding.googleLoginLayout.setOnClickListener {
            Log.d(TAG, "Google Sign-in button clicked")
            signInWithGoogle()
        }
        Log.d(TAG, "initView: Google Sign-in button click listener set up")
        // Set up Facebook login button click listener
        val facebookRow = mViewBinding.root.findViewById<android.view.View>(R.id.facebook_login_layout)
        facebookRow?.setOnClickListener {
            Log.d(TAG, "Facebook Sign-in button clicked")
            signInWithFacebook()
        }

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
            signInWithEmailAndPassword(email, password)
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
                    Log.d(TAG, "signInWithCredential:success")
                    val user = auth.currentUser
                    user?.email.let {
                        if (!TextUtils.isEmpty(user?.email)) {
                            ToastUtils.showShortToast(mContext!!, getString(R.string.email_prefix, user?.email ?: ""))
                            SharedPreferenceUtils.saveBoolean(mContext, "thirdLogin", true)
                            SharedPreferenceUtils.saveBoolean(mContext, "isLogin", true)
                            
                            // Navigate to MainActivity
                            val intent = Intent(mContext, MainActivity::class.java)
                            startActivity(intent)
                            finish()
                        } else {
                            ToastUtils.showShortToast(mContext!!, getString(R.string.email_empty))
                        }
                    }
                } else {
                    // If sign in fails, display a message to the user
                    Log.w(TAG, "signInWithCredential:failure", task.exception)
                    ToastUtils.showShortToast(mContext!!, getString(R.string.login_failed))
                }
            }
    }

}