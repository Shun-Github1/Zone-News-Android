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

        // Use standardized included toolbar
        binding.topLayout.titleTv.text = getString(com.anssy.znewspro.R.string.about)

        // Simple clicks for legal links (placeholder)
        binding.privacyPolicyBtn.setOnClickListener { }
        binding.termsBtn.setOnClickListener { }
    }
}


