package com.searcher.zonenews.ui.settings

import android.os.Bundle
import androidx.activity.OnBackPressedCallback
import com.searcher.zonenews.base.BaseActivity
import androidx.core.view.WindowCompat
import com.searcher.zonenews.R
import com.searcher.zonenews.databinding.ActivitySettingsBinding
import com.jaeger.library.StatusBarUtil

class SettingsActivity : BaseActivity() {
    
    private lateinit var binding: ActivitySettingsBinding
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        // Set up status bar
        applyStatusBarStyle()
        
        // Set up MaterialToolbar with modern approach
        setupToolbar()
        
        // Load settings fragment
        if (savedInstanceState == null) {
            supportFragmentManager
                .beginTransaction()
                .replace(R.id.settings_container, SettingsFragment())
                .commit()
        }
        
        // Set up modern back pressed handling
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                finish()
            }
        })
    }

    private fun setupToolbar() {
        val toolbar = binding.toolbar
        toolbar.title = getString(R.string.settings)
        toolbar.setNavigationOnClickListener { finish() }
    }
    
    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }
}
