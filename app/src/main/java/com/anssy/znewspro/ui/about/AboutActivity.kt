package com.anssy.znewspro.ui.about

import android.os.Bundle
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

        // Simple clicks for legal links (placeholder)
        binding.privacyPolicyBtn.setOnClickListener { }
        binding.termsBtn.setOnClickListener { }
    }

    private fun setupTopBar() {
        // Setup back arrow icon click
        binding.backArrowIcon.setOnClickListener { finish() }
    }
}


