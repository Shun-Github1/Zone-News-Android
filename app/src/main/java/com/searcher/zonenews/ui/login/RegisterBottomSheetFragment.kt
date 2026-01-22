package com.searcher.zonenews.ui.login

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.InputType
import android.text.TextUtils
import android.text.TextWatcher
import android.text.method.PasswordTransformationMethod
import android.util.Log
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import com.searcher.zonenews.R
import com.searcher.zonenews.databinding.FragmentRegisterBottomSheetBinding
import com.searcher.zonenews.model.LoginModel
import com.searcher.zonenews.ui.MainActivity
import com.searcher.zonenews.utils.Constants
import com.searcher.zonenews.utils.SharedPreferenceUtils
import com.searcher.zonenews.utils.SystemDialogUtils
import com.searcher.zonenews.utils.ToastUtils
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.bottomsheet.BottomSheetBehavior
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class RegisterBottomSheetFragment : BottomSheetDialogFragment() {
    
    private var _binding: FragmentRegisterBottomSheetBinding? = null
    private val binding get() = _binding!!
    
    private val loginModel: LoginModel by viewModels()
    
    companion object {
        private val TAG = RegisterBottomSheetFragment::class.simpleName
        
        fun newInstance(): RegisterBottomSheetFragment {
            return RegisterBottomSheetFragment()
        }
    }
    
    override fun onCreateDialog(savedInstanceState: Bundle?): android.app.Dialog {
        val dialog = super.onCreateDialog(savedInstanceState)
        // Apply custom animation style to slow down the popup animation
        dialog.window?.let { window ->
            val params = window.attributes
            params.windowAnimations = R.style.BottomSheetAnimation
            window.attributes = params
        }
        return dialog
    }
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRegisterBottomSheetBinding.inflate(inflater, container, false)
        return binding.root
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupViews()
        initModel()
    }
    
    override fun onStart() {
        super.onStart()
        
        // Force bottom sheet to fully expanded state
        val bottomSheet = dialog?.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)
        bottomSheet?.let {
            val behavior = BottomSheetBehavior.from(it)
            behavior.state = BottomSheetBehavior.STATE_EXPANDED
            behavior.isDraggable = false // Prevent accidental dismissal when scrolling
            behavior.skipCollapsed = true // Skip collapsed state when dismissing
        }
    }
    
    private fun setupViews() {
        // Setup close button
        binding.closeButton.setOnClickListener {
            dismiss()
        }
        
        // Setup clear buttons and password visibility toggles
        setupClearButtons()
        
        // Setup register button
        binding.registerBtn.setOnClickListener {
            val email = binding.emailEt.text?.toString()?.trim() ?: ""
            val username = binding.nameEt.text?.toString()?.trim() ?: ""
            val password = binding.passEt.text?.toString()?.trim() ?: ""
            val confirmPassword = binding.passAgainEt.text?.toString()?.trim() ?: ""
            
            if (TextUtils.isEmpty(email)) {
                ToastUtils.showShortToast(requireContext(), getString(R.string.register_error_email_empty))
                return@setOnClickListener
            }
            if (TextUtils.isEmpty(username)) {
                ToastUtils.showShortToast(requireContext(), getString(R.string.register_error_username_empty))
                return@setOnClickListener
            }
            if (TextUtils.isEmpty(password)) {
                ToastUtils.showShortToast(requireContext(), getString(R.string.register_error_password_empty))
                return@setOnClickListener
            }
            if (TextUtils.isEmpty(confirmPassword)) {
                ToastUtils.showShortToast(requireContext(), getString(R.string.register_error_password_again_empty))
                return@setOnClickListener
            }
            if (!TextUtils.equals(password, confirmPassword)) {
                ToastUtils.showShortToast(requireContext(), getString(R.string.register_error_passwords_mismatch))
                return@setOnClickListener
            }
            if (password.length < 6) {
                ToastUtils.showShortToast(requireContext(), getString(R.string.register_error_password_weak))
                return@setOnClickListener
            }
            
            registerWithEmailAndPassword(email, password, username)
        }
    }
    
    private fun setupClearButtons() {
        // Password field clear button and visibility toggle
        binding.passEt.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val hasText = s?.toString()?.isNotEmpty() == true
                binding.passwordClearButton.isVisible = hasText
                binding.passwordVisibilityToggle.isVisible = hasText
            }
        })

        binding.passwordClearButton.setOnClickListener {
            binding.passEt.setText("")
            binding.passEt.clearFocus()
        }

        // Confirm password field clear button and visibility toggle
        binding.passAgainEt.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val hasText = s?.toString()?.isNotEmpty() == true
                binding.confirmPasswordClearButton.isVisible = hasText
                binding.confirmPasswordVisibilityToggle.isVisible = hasText
            }
        })

        binding.confirmPasswordClearButton.setOnClickListener {
            binding.passAgainEt.setText("")
            binding.passAgainEt.clearFocus()
        }

        // Setup password visibility toggles
        setupPasswordVisibilityToggle()
    }
    
    private fun setupPasswordVisibilityToggle() {
        // Password field visibility toggle
        binding.passwordVisibilityToggle.setOnTouchListener { view, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    // Show password when holding down
                    binding.passEt.transformationMethod = null
                    binding.passEt.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
                    binding.passwordVisibilityToggle.setImageResource(R.drawable.visibility_24px)
                    true
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    // Hide password when released
                    binding.passEt.transformationMethod = PasswordTransformationMethod.getInstance()
                    binding.passEt.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
                    binding.passwordVisibilityToggle.setImageResource(R.drawable.visibility_off_24px)
                    true
                }
                else -> false
            }
        }
        
        // Confirm password field visibility toggle
        binding.confirmPasswordVisibilityToggle.setOnTouchListener { view, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    // Show password when holding down
                    binding.passAgainEt.transformationMethod = null
                    binding.passAgainEt.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
                    binding.confirmPasswordVisibilityToggle.setImageResource(R.drawable.visibility_24px)
                    true
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    // Hide password when released
                    binding.passAgainEt.transformationMethod = PasswordTransformationMethod.getInstance()
                    binding.passAgainEt.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
                    binding.confirmPasswordVisibilityToggle.setImageResource(R.drawable.visibility_off_24px)
                    true
                }
                else -> false
            }
        }
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
                SystemDialogUtils.showSuccessMessage(requireContext(), getString(R.string.register_success))
                
                // Mark as logged in (backend returns JWT cookie automatically)
                SharedPreferenceUtils.saveBoolean(requireContext(), "isLogin", true)
                SharedPreferenceUtils.saveBoolean(requireContext(), "thirdLogin", false) // Manual account, not third-party
                
                // Dismiss bottom sheet first
                dismiss()
                
                // Navigate to MainActivity
                val intent = Intent(requireContext(), MainActivity::class.java)
                startActivity(intent)
                activity?.finishAffinity()
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
                SystemDialogUtils.showErrorMessage(requireContext(), errorMessage)
            }
        }
    }
    
    private fun registerWithEmailAndPassword(email: String, password: String, username: String) {
        Log.d(TAG, "registerWithEmailAndPassword: Starting registration for email: $email, username: $username")
        
        // Show loading dialog
        SystemDialogUtils.showLoadingDialog(requireContext(), getString(R.string.register_status_registering))
        
        // Call backend API for registration
        loginModel.registerApp(email, username, password)
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
