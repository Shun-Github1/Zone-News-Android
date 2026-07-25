package com.searcher.zonenews.ui.about

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.SpannableString
import android.text.style.UnderlineSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import com.searcher.zonenews.R
import com.searcher.zonenews.databinding.FragmentAboutBottomSheetBinding
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.bottomsheet.BottomSheetBehavior

class AboutBottomSheetFragment : BottomSheetDialogFragment() {
    
    private var _binding: FragmentAboutBottomSheetBinding? = null
    private val binding get() = _binding!!
    
    companion object {
        fun newInstance(): AboutBottomSheetFragment {
            return AboutBottomSheetFragment()
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
        _binding = FragmentAboutBottomSheetBinding.inflate(inflater, container, false)
        return binding.root
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupViews()
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
        
        // Setup email click and underline
        setupEmailClick()
        
        // Setup social media clicks
        setupSocialMediaClicks()
        
        // Simple clicks for legal links (placeholder)
        binding.privacyPolicyBtn.setOnClickListener { }
        binding.termsBtn.setOnClickListener { }
    }
    
    private fun setupEmailClick() {
        // Add underline to email text
        val emailText = binding.emailTextView.text.toString()
        val spannableString = SpannableString(emailText)
        spannableString.setSpan(UnderlineSpan(), 0, emailText.length, 0)
        binding.emailTextView.text = spannableString
        
        // Open email app - Android best practice using ACTION_SENDTO with mailto:
        binding.emailTextView.setOnClickListener {
            val emailIntent = Intent(Intent.ACTION_SENDTO).apply {
                data = Uri.parse("mailto:contact@zonenews.io")
            }
            try {
                startActivity(emailIntent)
            } catch (e: ActivityNotFoundException) {
                // No email app installed - silently fail or show toast
                e.printStackTrace()
            }
        }
    }
    
    private fun setupSocialMediaClicks() {
        // Ensure all icons are clickable and set up listeners
        // Instagram - using ACTION_VIEW to open URL (same as iOS UIApplication.shared.open)
        binding.instagramIcon.apply {
            isClickable = true
            isFocusable = true
            setOnClickListener {
                openUrl("https://www.instagram.com/zonenews.io/")
            }
        }
        
        // Facebook
        binding.facebookIcon.apply {
            isClickable = true
            isFocusable = true
            setOnClickListener {
                openUrl("https://www.facebook.com/profile.php?id=61580071810702#")
            }
        }
        
        // X (Twitter)
        binding.xIcon.apply {
            isClickable = true
            isFocusable = true
            setOnClickListener {
                openUrl("https://x.com/zonenews_io")
            }
        }
        
        // LinkedIn
        binding.linkedinIcon.apply {
            isClickable = true
            isFocusable = true
            setOnClickListener {
                openUrl("https://www.linkedin.com/company/zonenews/")
            }
        }
        
        // Website
        binding.websiteIcon.apply {
            isClickable = true
            isFocusable = true
            setOnClickListener {
                openUrl(getString(R.string.about_us_website))
            }
        }
    }
    
    private fun openUrl(url: String) {
        // Android best practice: just try to start the activity
        // The system will handle finding the appropriate app
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        try {
            startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            // No browser installed - silently fail or show toast
            e.printStackTrace()
        }
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
