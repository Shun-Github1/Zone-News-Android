@file:Suppress("DEPRECATION")
package com.searcher.zonenews.ui.login

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.InputType
import android.text.TextUtils
import android.text.TextWatcher
import android.text.method.PasswordTransformationMethod
import android.util.Log
import android.view.MotionEvent
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.activity.viewModels
import androidx.lifecycle.lifecycleScope
import com.searcher.zonenews.R
import com.searcher.zonenews.base.BaseActivity
import com.searcher.zonenews.databinding.ActivityLoginBinding
import com.searcher.zonenews.model.LoginModel
import com.searcher.zonenews.ui.MainActivity
import com.searcher.zonenews.utils.SharedPreferenceUtils
import com.searcher.zonenews.utils.ToastUtils
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
import com.google.firebase.auth.AuthCredential
import com.google.firebase.auth.FacebookAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.auth
import com.jaeger.library.StatusBarUtil
import com.searcher.zonenews.utils.SystemDialogUtils
import com.searcher.zonenews.utils.network.PersistentCookieJar
import com.google.firebase.auth.OAuthProvider
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
    private var isManualLogin = false // Track if current login attempt is manual (username/password)
    
    companion object {
        private const val RC_SIGN_IN = 9001
        val TAG = LoginActivity::class.simpleName
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Initialize Firebase and Google Sign-In first (needed for auth operations)
        initFirebase()
        initGoogleSignIn()
        
        // Check if user is already logged in and has valid cookies (auto-login)
        val isLoggedIn = SharedPreferenceUtils.getBoolean(mContext, "isLogin")
        if (isLoggedIn) {
            // Create a temporary cookie jar instance to check for valid cookies
            // Note: This is safe because cookie jar loads from SharedPreferences
            val tempCookieJar = PersistentCookieJar(mContext!!)
            if (tempCookieJar.hasValidCookies()) {
                Log.d(TAG, "User is already logged in with valid cookies - auto-login")
                val intent = Intent(mContext, MainActivity::class.java)
                startActivity(intent)
                finish()
                return
            } else {
                // Cookies are expired or invalid - clear login state
                Log.d(TAG, "Login state found but cookies are invalid - clearing login state")
                SharedPreferenceUtils.saveBoolean(mContext, "isLogin", false)
                SharedPreferenceUtils.saveBoolean(mContext, "thirdLogin", false)
                SharedPreferenceUtils.deleteString(mContext, "token")
            }
        }
        
        mViewBinding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(mViewBinding.root)
        
        Log.d(TAG, "Showing login screen")
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
        
        // Show loading dialog
        SystemDialogUtils.showLoadingDialog(this, getString(R.string.login_logging_in))
        
        val credential = FacebookAuthProvider.getCredential(token.token)
        auth.signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    // Sign in success, get Firebase ID token and authenticate with backend
                    Log.d(TAG, "signInWithCredential:success - User: ${task.result?.user?.email}")
                    val user = auth.currentUser
                    if (user != null) {
                       processFirebaseUser(user)
                    } else {
                        Log.e(TAG, "User is null")
                        SystemDialogUtils.dismissLoadingDialog()
                        ToastUtils.showShortToast(mContext!!, getString(R.string.login_failed))
                    }
                } else {
                    // Handle failure
                    val exception = task.exception
                    Log.e(TAG, "signInWithCredential:failure", exception)
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
        @Suppress("DEPRECATION")
        startActivityForResult(signInIntent, RC_SIGN_IN)
    }

    private fun signInWithFacebook() {
        Log.d(TAG, "signInWithFacebook: Starting Facebook Sign-in process")
        
        LoginManager.getInstance().logInWithReadPermissions(
            this,
            listOf("email", "public_profile")
        )
    }



    private fun signInWithApple() {
        showLoading()
        val provider = OAuthProvider.newBuilder("apple.com")
        provider.addCustomParameter("locale", "en")
        
        val pending = auth.pendingAuthResult
        if (pending != null) {
            pending.addOnSuccessListener { authResult ->
                Log.d(TAG, "checkPending:onSuccess:$authResult")
                processFirebaseUser(authResult.user!!)
            }.addOnFailureListener { e ->
                Log.w(TAG, "checkPending:onFailure", e)
                dismissLoading()
                ToastUtils.showShortToast(mContext!!, getString(R.string.login_failed))
            }
        } else {
            auth.startActivityForSignInWithProvider(this, provider.build())
                .addOnSuccessListener { authResult ->
                    Log.d(TAG, "activitySignIn:onSuccess:$authResult")
                    processFirebaseUser(authResult.user!!)
                }
                .addOnFailureListener { e ->
                    Log.w(TAG, "activitySignIn:onFailure", e)
                    dismissLoading()
                    ToastUtils.showShortToast(mContext!!, getString(R.string.login_failed))
                }
        }
    }

    private fun showLoading() {
        SystemDialogUtils.showLoadingDialog(this, getString(R.string.login_logging_in))
    }

    private fun dismissLoading() {
        SystemDialogUtils.dismissLoadingDialog()
    }

    private fun signInWithUsernameAndPassword(username: String, password: String) {
        Log.d(TAG, "signInWithUsernameAndPassword: Starting username/password sign-in")
        
        // Mark as manual login for error handling
        isManualLogin = true
        
        // All username/password logins go through the backend API
        // The backend handles authentication and returns JWT cookies
        loginModel.loginApp(username, password)
    }

    private fun initModel(){
        loginModel.loginEntry.observe(this) { loginEntry ->
            if (loginEntry == null) {
                Log.w(TAG, "LoginEntry is null")
                return@observe
            }
            
            Log.d(TAG, "LoginEntry received - code: ${loginEntry.code}, msg: ${loginEntry.msg}")
            
            if (loginEntry.code != null && loginEntry.code == com.searcher.zonenews.utils.Constants.SUCCESS_CODE) {
                Log.d(TAG, "Login successful, navigating to MainActivity")
                SystemDialogUtils.dismissLoadingDialog()
                SystemDialogUtils.showSuccessMessage(this, getString(R.string.login_success))
                // Reset flag on successful login
                isManualLogin = false
                // With cookie-based authentication, we don't need to save token manually
                // Cookies are automatically handled by PersistentCookieJar
                // Keep token field for backward compatibility if needed, but it's not used for auth
                // Prefer csrf_token if available, fall back to access_token for backward compatibility
                loginEntry.data?.let { data ->
                    val token = data.csrf_token ?: data.access_token
                    token?.let {
                        SharedPreferenceUtils.saveString(mContext,"token", it)
                    }
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
                // Display specific error messages from API, or fallback to generic message
                val errorMessage = when (loginEntry.code) {
                    401 -> {
                        // For manual login, always show "Incorrect username or password"
                        // For Firebase login, show API message (Invalid Firebase token)
                        if (isManualLogin) {
                            getString(R.string.incorrect_username_or_password)
                        } else {
                            loginEntry.msg ?: getString(R.string.login_failed)
                        }
                    }
                    409 -> loginEntry.msg ?: getString(R.string.login_failed) // Username/Account conflict
                    else -> loginEntry.msg ?: getString(R.string.incorrect_username_or_password)
                }
                ToastUtils.showShortToast(mContext!!, errorMessage)
                // Reset flag after showing error
                isManualLogin = false
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
        
        // Apple login
        val appleRow = mViewBinding.root.findViewById<android.view.View>(R.id.apple_login_layout)
        appleRow?.setOnClickListener {
            Log.d(TAG, "Apple Sign-in button clicked")
            signInWithApple()
        }

        // Set up Facebook login button click listener
        val facebookRow = mViewBinding.root.findViewById<android.view.View>(R.id.facebook_login_layout)
        facebookRow?.setOnClickListener {
            Log.d(TAG, "Facebook Sign-in button clicked")
            signInWithFacebook()
        }

        // Username/password login
        mViewBinding.loginBtn.setOnClickListener {
            val username = mViewBinding.usernameEt.text?.toString()?.trim() ?: ""
            val password = mViewBinding.passEt.text?.toString()?.trim() ?: ""
            if (TextUtils.isEmpty(username)){
                ToastUtils.showShortToast(mContext!!,getString(R.string.login_enter_account))
                return@setOnClickListener
            }
            if (TextUtils.isEmpty(password)){
                ToastUtils.showShortToast(mContext!!,getString(R.string.login_enter_password))
                return@setOnClickListener
            }
            signInWithUsernameAndPassword(username, password)
        }

        // Go to register
        mViewBinding.registerTv.setOnClickListener {
            RegisterBottomSheetFragment.newInstance().show(supportFragmentManager, "Register")
        }

        // Setup clear buttons for email and password fields
        setupClearButtons()
    }

    private fun setupClearButtons() {
        // Username field clear button
        mViewBinding.usernameEt.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val hasText = s?.toString()?.isNotEmpty() == true
                mViewBinding.usernameClearButton.isVisible = hasText
            }
        })

        mViewBinding.usernameClearButton.setOnClickListener {
            mViewBinding.usernameEt.setText("")
            mViewBinding.usernameEt.clearFocus()
        }

        // Password field clear button
        mViewBinding.passEt.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val hasText = s?.toString()?.isNotEmpty() == true
                mViewBinding.passwordClearButton.isVisible = hasText
                mViewBinding.passwordVisibilityToggle.isVisible = hasText
            }
        })

        mViewBinding.passwordClearButton.setOnClickListener {
            mViewBinding.passEt.setText("")
            mViewBinding.passEt.clearFocus()
        }

        // Setup password visibility toggle
        setupPasswordVisibilityToggle()
    }

    private fun setupPasswordVisibilityToggle() {
        mViewBinding.passwordVisibilityToggle.setOnTouchListener { view, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    // Show password when holding down
                    mViewBinding.passEt.transformationMethod = null
                    mViewBinding.passEt.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
                    mViewBinding.passwordVisibilityToggle.setImageResource(R.drawable.visibility_24px)
                    true
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    // Hide password when released
                    mViewBinding.passEt.transformationMethod = PasswordTransformationMethod.getInstance()
                    mViewBinding.passEt.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
                    mViewBinding.passwordVisibilityToggle.setImageResource(R.drawable.visibility_off_24px)
                    true
                }
                else -> false
            }
        }
    }

    private fun processFirebaseUser(user: com.google.firebase.auth.FirebaseUser) {
        if (!TextUtils.isEmpty(user.email)) {
            // Get Firebase ID token and authenticate with backend API
            user.getIdToken(false).addOnCompleteListener { tokenTask ->
                if (tokenTask.isSuccessful) {
                    val firebaseIdToken = tokenTask.result?.token
                    if (firebaseIdToken != null) {
                        // Mark as Firebase login (not manual)
                        isManualLogin = false
                        loginModel.loginWithFirebase(firebaseIdToken)
                    } else {
                        SystemDialogUtils.dismissLoadingDialog()
                        ToastUtils.showShortToast(mContext!!, getString(R.string.login_failed))
                    }
                } else {
                    Log.w(TAG, "Failed to get Firebase ID token", tokenTask.exception)
                    SystemDialogUtils.dismissLoadingDialog()
                    ToastUtils.showShortToast(mContext!!, getString(R.string.login_failed))
                }
            }
        } else {
            SystemDialogUtils.dismissLoadingDialog()
            ToastUtils.showShortToast(mContext!!, getString(R.string.email_empty))
        }
    }

    private fun firebaseAuthWithGoogle(idToken: String) {
        // Show loading dialog
        SystemDialogUtils.showLoadingDialog(this, getString(R.string.login_logging_in))
        
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        auth.signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    // Sign in success, get Firebase ID token and authenticate with backend
                    Log.d(TAG, "signInWithCredential:success")
                    val user = auth.currentUser
                    if (user != null) {
                        processFirebaseUser(user)
                    } else {
                         SystemDialogUtils.dismissLoadingDialog()
                         ToastUtils.showShortToast(mContext!!, getString(R.string.login_failed))
                    }
                } else {
                    // Handle failure
                    val exception = task.exception
                    Log.w(TAG, "signInWithCredential:failure", exception)
                    SystemDialogUtils.dismissLoadingDialog()
                    ToastUtils.showShortToast(mContext!!, getString(R.string.login_failed))
                }
            }
    }

}
