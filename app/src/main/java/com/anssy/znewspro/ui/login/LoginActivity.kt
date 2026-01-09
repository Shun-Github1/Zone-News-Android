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
        
        // CRITICAL: Always force isLogin to false first - we're on the login screen
        // This prevents auto-login from backup/restored SharedPreferences
        SharedPreferenceUtils.saveBoolean(mContext, "isLogin", false)
        SharedPreferenceUtils.saveBoolean(mContext, "thirdLogin", false)
        SharedPreferenceUtils.deleteString(mContext, "token")
        SharedPreferenceUtils.deleteString(mContext, "autoLogin")
        
        // Clear cookies from SharedPreferences (before PersistentCookieJar singleton loads them)
        val cookiePrefs = mContext!!.getSharedPreferences("cookie_prefs", android.content.Context.MODE_PRIVATE)
        cookiePrefs.edit().clear().apply()
        Log.d(TAG, "Cleared all authentication SharedPreferences and cookies")
        
        // Now initialize Firebase and Google Sign-In (needed for sign out operations)
        initFirebase()
        initGoogleSignIn()
        
        // Sign out from all third-party auth providers to ensure clean state
        clearThirdPartyAuth()
        
        // Always show login screen (never auto-login, regardless of backup restore)
        Log.d(TAG, "Showing login screen - user must authenticate")
        initView()
        // Set up observer first before registering callbacks
        initModel()
        initFacebookAuth()
    }
    
    /**
     * Clear third-party authentication state (Firebase, Google, Facebook)
     * This is called after initializing these services so we can sign out
     */
    private fun clearThirdPartyAuth() {
        try {
            // Sign out from Firebase Auth
            auth.signOut()
            Log.d(TAG, "Signed out from Firebase Auth")
            
            // Sign out from Google Sign-In
            googleSignInClient.signOut().addOnCompleteListener {
                Log.d(TAG, "Signed out from Google Sign-In")
            }
            
            // Sign out from Facebook
            LoginManager.getInstance().logOut()
            Log.d(TAG, "Signed out from Facebook")
        } catch (e: Exception) {
            Log.e(TAG, "Error clearing third-party auth: ${e.message}")
        }
    }
    


    private fun initFacebookAuth() {
        // Initialize Facebook LoginManager
        LoginManager.getInstance().registerCallback(
            callbackManager,
            object : FacebookCallback<LoginResult> {
                override fun onSuccess(result: LoginResult) {
                    Log.d(TAG, "facebook:onSuccess - Thread: ${Thread.currentThread().name}")
                    Log.d(TAG, "facebook:onSuccess - AccessToken: ${result.accessToken?.token?.take(20)}...")
                    // Ensure we're on the main thread
                    runOnUiThread {
                        handleFacebookAccessToken(result.accessToken)
                    }
                }

                override fun onCancel() {
                    Log.d(TAG, "facebook:onCancel")
                }

                override fun onError(error: FacebookException) {
                    Log.e(TAG, "facebook:onError", error)
                    runOnUiThread {
                        ToastUtils.showShortToast(mContext!!, getString(R.string.login_failed))
                    }
                }
            }
        )
    }


    /**
     * Facebook登录处理
     */
    private fun handleFacebookAccessToken(token: AccessToken) {
        Log.d(TAG, "handleFacebookAccessToken - Thread: ${Thread.currentThread().name}")
        Log.d(TAG, "handleFacebookAccessToken - Token: ${token.token.take(20)}...")
        
        val credential = FacebookAuthProvider.getCredential(token.token)
        auth.signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    // Sign in success, get Firebase ID token and authenticate with backend
                    Log.d(TAG, "signInWithCredential:success - User: ${task.result?.user?.email}")
                    val user = auth.currentUser
                    if (user != null && !TextUtils.isEmpty(user.email)) {
                        // Get Firebase ID token and authenticate with backend API
                        Log.d(TAG, "Getting Firebase ID token for user: ${user.email}")
                        user.getIdToken(false).addOnCompleteListener { tokenTask ->
                            if (tokenTask.isSuccessful) {
                                val idToken = tokenTask.result?.token
                                if (idToken != null) {
                                    Log.d(TAG, "Firebase ID token obtained, calling loginWithFirebase")
                                    SystemDialogUtils.showLoadingDialog(this, getString(R.string.login_logging_in))
                                    loginModel.loginWithFirebase(idToken)
                                } else {
                                    Log.e(TAG, "Firebase ID token is null")
                                    SystemDialogUtils.dismissLoadingDialog()
                                    ToastUtils.showShortToast(mContext!!, getString(R.string.login_failed))
                                }
                            } else {
                                Log.e(TAG, "Failed to get Firebase ID token", tokenTask.exception)
                                SystemDialogUtils.dismissLoadingDialog()
                                ToastUtils.showShortToast(mContext!!, getString(R.string.login_failed))
                            }
                        }
                    } else {
                        Log.e(TAG, "User is null or email is empty - User: $user, Email: ${user?.email}")
                        SystemDialogUtils.dismissLoadingDialog()
                        ToastUtils.showShortToast(mContext!!, getString(R.string.email_empty))
                    }
                } else {
                    // If sign in fails, display a message to the user
                    Log.e(TAG, "signInWithCredential:failure", task.exception)
                    SystemDialogUtils.dismissLoadingDialog()
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
        
        // All email/password logins go through the backend API
        // The backend handles authentication and returns JWT cookies
        SystemDialogUtils.showLoadingDialog(this, getString(R.string.login_logging_in))
        loginModel.loginApp(email, password)
    }

    private fun initModel(){
        loginModel.loginEntry.observe(this) { loginEntry ->
            if (loginEntry == null) {
                Log.w(TAG, "LoginEntry is null")
                return@observe
            }
            
            Log.d(TAG, "LoginEntry received - code: ${loginEntry.code}, msg: ${loginEntry.msg}")
            
            if (loginEntry.code != null && loginEntry.code == com.anssy.znewspro.utils.Constants.SUCCESS_CODE) {
                Log.d(TAG, "Login successful, navigating to MainActivity")
                SystemDialogUtils.dismissLoadingDialog()
                SystemDialogUtils.showSuccessMessage(this, getString(R.string.login_success))
                // With cookie-based authentication, we don't need to save token manually
                // Cookies are automatically handled by PersistentCookieJar
                // Keep token field for backward compatibility if needed, but it's not used for auth
                loginEntry.data?.access_token?.let { token ->
                    SharedPreferenceUtils.saveString(mContext,"token", token)
                }
                // Mark as third-party login if Firebase auth is active
                if (auth.currentUser != null) {
                    SharedPreferenceUtils.saveBoolean(mContext, "thirdLogin", true)
                }
                SharedPreferenceUtils.saveBoolean(mContext,"isLogin",true)
                val intent = Intent(mContext,MainActivity::class.java)
                startActivity(intent)
                finishAffinity()
            } else {
                Log.w(TAG, "Login failed - code: ${loginEntry.code}, msg: ${loginEntry.msg}")
                SystemDialogUtils.dismissLoadingDialog()
                ToastUtils.showShortToast(mContext!!, loginEntry.msg ?: getString(R.string.login_failed))
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
                    // Sign in success, get Firebase ID token and authenticate with backend
                    Log.d(TAG, "signInWithCredential:success")
                    val user = auth.currentUser
                    if (user != null && !TextUtils.isEmpty(user.email)) {
                        // Get Firebase ID token and authenticate with backend API
                        user.getIdToken(false).addOnCompleteListener { tokenTask ->
                            if (tokenTask.isSuccessful) {
                                val firebaseIdToken = tokenTask.result?.token
                                if (firebaseIdToken != null) {
                                    SystemDialogUtils.showLoadingDialog(this, getString(R.string.login_logging_in))
                                    loginModel.loginWithFirebase(firebaseIdToken)
                                } else {
                                    ToastUtils.showShortToast(mContext!!, getString(R.string.login_failed))
                                }
                            } else {
                                Log.w(TAG, "Failed to get Firebase ID token", tokenTask.exception)
                                ToastUtils.showShortToast(mContext!!, getString(R.string.login_failed))
                            }
                        }
                    } else {
                        ToastUtils.showShortToast(mContext!!, getString(R.string.email_empty))
                    }
                } else {
                    // If sign in fails, display a message to the user
                    Log.w(TAG, "signInWithCredential:failure", task.exception)
                    ToastUtils.showShortToast(mContext!!, getString(R.string.login_failed))
                }
            }
    }

}