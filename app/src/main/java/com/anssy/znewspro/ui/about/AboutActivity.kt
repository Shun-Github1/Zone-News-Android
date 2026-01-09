package com.anssy.znewspro.ui.about

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.SpannableString
import android.text.style.UnderlineSpan
import com.anssy.znewspro.R
import com.anssy.znewspro.base.BaseActivity
import com.anssy.znewspro.databinding.ActivityAboutBinding

class AboutActivity : BaseActivity() {

    private lateinit var binding: ActivityAboutBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAboutBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Status bar style consistent with app theme
        applyStatusBarStyle()

        // Setup new top bar with back functionality
        setupTopBar()

        // Setup email click and underline
        setupEmailClick()
        
        // Setup social media clicks
        setupSocialMediaClicks()

        // Simple clicks for legal links (placeholder)
        binding.privacyPolicyBtn.setOnClickListener { }
        binding.termsBtn.setOnClickListener { }
        
        // Add fade-in animation
        overridePendingTransition(R.anim.fade_in, R.anim.fade_out)
    }

    private fun setupTopBar() {
        // Setup back arrow icon click
        binding.backArrowIcon.setOnClickListener { 
            finish()
            // Add fade-out animation
            overridePendingTransition(R.anim.fade_in, R.anim.fade_out)
        }
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
                openUrl("https://zonenews.io")
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
    
    override fun finish() {
        super.finish()
        // Add fade-out animation when activity finishes
        overridePendingTransition(R.anim.fade_in, R.anim.fade_out)
    }
}


