package com.searcher.zonenews.ui.mainfrag

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.searcher.zonenews.R
import com.searcher.zonenews.databinding.FragmentWelcomePosterBottomSheetBinding
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.searcher.zonenews.utils.TutorialManager

/**
 * Welcome poster bottom sheet that appears before the tutorial for new users
 * Similar to iOS WelcomePosterSheet implementation
 */
class WelcomePosterBottomSheetFragment : BottomSheetDialogFragment() {
    
    private var _binding: FragmentWelcomePosterBottomSheetBinding? = null
    private val binding get() = _binding!!
    
    private var onContinueClick: (() -> Unit)? = null
    
    companion object {
        fun newInstance(onContinueClick: (() -> Unit)? = null): WelcomePosterBottomSheetFragment {
            return WelcomePosterBottomSheetFragment().apply {
                this.onContinueClick = onContinueClick
            }
        }
    }
    
    override fun onCreateDialog(savedInstanceState: Bundle?): android.app.Dialog {
        val dialog = super.onCreateDialog(savedInstanceState)
        // Make dialog non-dismissible by tapping outside or back button
        // User must click Continue button to proceed
        isCancelable = false
        // Apply custom animation style
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
        _binding = FragmentWelcomePosterBottomSheetBinding.inflate(inflater, container, false)
        return binding.root
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupViews()
    }
    
    override fun onStart() {
        super.onStart()
        
        // Set bottom sheet to fit contents and remove background
        val bottomSheet = dialog?.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)
        bottomSheet?.let {
            // Make background transparent to show the floating card with margins
            it.setBackgroundResource(android.R.color.transparent)
            
            val behavior = BottomSheetBehavior.from(it)
            behavior.isFitToContents = true
            behavior.state = BottomSheetBehavior.STATE_EXPANDED
            behavior.isDraggable = false // Prevent accidental dismissal
            behavior.skipCollapsed = true
        }
    }
    
    private fun setupViews() {
        // Setup continue button
        binding.continueButton.setOnClickListener {
            // Mark welcome poster as shown
            val accountId = com.searcher.zonenews.utils.SharedPreferenceUtils.getString(requireContext(), "current_account_id")
            TutorialManager.markWelcomePosterShown(requireContext(), accountId)
            
            // Call the continue callback
            onContinueClick?.invoke()
            
            // Dismiss the bottom sheet
            dismiss()
        }
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
