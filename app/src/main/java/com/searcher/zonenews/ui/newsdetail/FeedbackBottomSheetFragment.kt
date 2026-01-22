package com.searcher.zonenews.ui.newsdetail

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.content.Context
import android.view.WindowManager
import androidx.core.widget.addTextChangedListener
import androidx.core.content.ContextCompat
import com.searcher.zonenews.R
import com.searcher.zonenews.databinding.FragmentFeedbackBottomSheetBinding
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.bottomsheet.BottomSheetBehavior
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay

class FeedbackBottomSheetFragment : BottomSheetDialogFragment() {
    
    private var _binding: FragmentFeedbackBottomSheetBinding? = null
    private val binding get() = _binding!!
    
    private var onFeedbackSubmitted: (suspend (String) -> Boolean)? = null
    
    companion object {
        fun newInstance(onFeedbackSubmitted: suspend (String) -> Boolean): FeedbackBottomSheetFragment {
            return FeedbackBottomSheetFragment().apply {
                this.onFeedbackSubmitted = onFeedbackSubmitted
            }
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
        _binding = FragmentFeedbackBottomSheetBinding.inflate(inflater, container, false)
        return binding.root
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupViews()
        setupFocusManagement()
    }
    
    override fun onStart() {
        super.onStart()
        
        // Set window soft input mode to adjust resize for keyboard handling
        @Suppress("DEPRECATION")
        dialog?.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
        
        // Force bottom sheet to fully expanded state (remove STATE_COLLAPSED)
        val bottomSheet = dialog?.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)
        bottomSheet?.let {
            val behavior = BottomSheetBehavior.from(it)
            behavior.state = BottomSheetBehavior.STATE_EXPANDED
            // Disable dragging to prevent keyboard issues
            behavior.isDraggable = false
        }
    }
    
    private fun setupViews() {
        // Initially disable submit button (like iOS)
        binding.submitButton.isEnabled = false
        
        // Set initial appearance (no content state)
        updateSubmitButtonAppearance(false)
        
        // Set up text watcher - matches iOS validation exactly
        binding.feedbackEditText.addTextChangedListener { text ->
            val trimmedText = text.toString().trim()
            val hasContent = trimmedText.isNotEmpty()
            
            // Update submit button state and appearance
            binding.submitButton.isEnabled = hasContent
            updateSubmitButtonAppearance(hasContent)
        }
        
        // Set up click listeners
        binding.closeButton.setOnClickListener {
            dismiss()
        }
        
        binding.submitButton.setOnClickListener {
            handleSubmission()
        }
        

    }
    
    private fun setupFocusManagement() {
        // Auto-focus with delay - matches iOS DispatchQueue.main.asyncAfter(deadline: .now() + 0.1)
        Handler(Looper.getMainLooper()).postDelayed({
            binding.feedbackEditText.requestFocus()
            showKeyboard()
            // Scroll to submit button after keyboard appears
            scrollToSubmitButton()
        }, 100) // 0.1 seconds like iOS
    }
    
    private fun scrollToSubmitButton() {
        // Delay to ensure keyboard is fully shown
        Handler(Looper.getMainLooper()).postDelayed({
            binding.submitButton.requestFocus()
            binding.submitButton.clearFocus()
        }, 300)
    }
    

    
    private fun handleSubmission() {
        val feedbackText = binding.feedbackEditText.text.toString()
        val trimmedText = feedbackText.trim() // Exact iOS validation
        
        if (trimmedText.isNotEmpty()) {
            // Disable button during submission
            binding.submitButton.isEnabled = false
            binding.submitButton.text = getString(R.string.submitting)
            // Maintain active appearance during submission
            updateSubmitButtonAppearance(true)
            
            // Async submission like iOS Task block
            CoroutineScope(Dispatchers.Main).launch {
                try {
                    val success = onFeedbackSubmitted?.invoke(trimmedText) ?: false
                    
                    if (success) {
                        // Clear text and dismiss on success (like iOS)
                        binding.feedbackEditText.setText(getString(R.string.empty_string))
                        dismiss()
                    } else {
                        // Re-enable button on failure
                        binding.submitButton.isEnabled = true
                        binding.submitButton.text = getString(R.string.submit)
                        updateSubmitButtonAppearance(true)
                    }
                } catch (e: Exception) {
                    // Re-enable button on error
                    binding.submitButton.isEnabled = true
                    binding.submitButton.text = getString(R.string.submit)
                    updateSubmitButtonAppearance(true)
                }
            }
        }
    }
    
    private fun showKeyboard() {
        val imm = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.showSoftInput(binding.feedbackEditText, InputMethodManager.SHOW_IMPLICIT)
    }
    
    private fun updateSubmitButtonAppearance(hasContent: Boolean) {
        if (hasContent) {
            // Text entered: white text + primary background
            binding.submitButton.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.white))
            binding.submitButton.backgroundTintList = ContextCompat.getColorStateList(requireContext(), R.color.main_color)
        } else {
            // No text: hint text color + light gray background
            binding.submitButton.setTextColor(ContextCompat.getColor(requireContext(), R.color.colorTextHint))
            binding.submitButton.backgroundTintList = ContextCompat.getColorStateList(requireContext(), R.color.surface_tertiary)
        }
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
