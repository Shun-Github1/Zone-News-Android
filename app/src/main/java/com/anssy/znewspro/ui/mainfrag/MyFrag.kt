package com.anssy.znewspro.ui.mainfrag

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.exceptions.ClearCredentialException
import androidx.fragment.app.viewModels
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.core.content.ContextCompat
import androidx.transition.AutoTransition
import androidx.transition.TransitionManager
import com.anssy.znewspro.utils.Constants
import com.anssy.znewspro.R
import com.anssy.znewspro.base.BaseFragment
import com.anssy.znewspro.databinding.FragMyBinding
import com.anssy.znewspro.model.LoginModel
import com.anssy.znewspro.model.MyModel
import com.anssy.znewspro.model.PublisherRegionModel
import com.anssy.znewspro.ui.MainActivity
import com.anssy.znewspro.ui.collect.MyCollectListActivity
import com.anssy.znewspro.ui.his.BrownHisActivity
import com.anssy.znewspro.ui.login.LoginActivity
import com.anssy.znewspro.ui.login.LoginActivity.Companion.TAG
import com.anssy.znewspro.ui.about.AboutActivity
import com.anssy.znewspro.ui.newsdetail.FeedbackBottomSheetFragment
import com.anssy.znewspro.ui.newsdetail.ReadingHistoryBottomSheetFragment
import com.anssy.znewspro.ui.newsdetail.SavedArticlesBottomSheetFragment
import com.anssy.znewspro.ui.newsdetail.SubscriptionBottomSheetFragment
import com.anssy.znewspro.utils.AppIconManager
import com.anssy.znewspro.utils.RegionMappingUtils
import com.anssy.znewspro.utils.SharedPreferenceUtils
import com.anssy.znewspro.utils.ThemeManager
import com.anssy.znewspro.utils.ToastUtils
import com.anssy.znewspro.utils.Utils
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.anssy.znewspro.utils.SystemDialogUtils
import kotlinx.coroutines.launch
import razerdp.util.PopupUtils.getString
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.facebook.login.LoginManager
import android.content.SharedPreferences

/**
 * @Description 我的
 * @Author yulu
 * @CreateTime 2025年06月30日 09:28:07
 */

class MyFrag : BaseFragment() {
    private lateinit var  mViewBinding:FragMyBinding
    private val loginModel:LoginModel by viewModels()
    private val myModel:MyModel by activityViewModels() // Use activity scope to share with SubscriptionBottomSheetFragment
    private val publisherRegionModel:PublisherRegionModel by viewModels()
    private lateinit var auth: FirebaseAuth
    private lateinit var credentialManager: CredentialManager
    
    // Dropdown state management
    private var activeDropdown: String? = null
    
    // Transition for smooth dropdown animations
    private val dropdownTransition by lazy {
        AutoTransition().apply {
            duration = 300
        }
    }
    
    companion object{
        fun  getInstance():MyFrag{
            return MyFrag()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        mViewBinding = FragMyBinding.inflate(layoutInflater)
        return mViewBinding.root
    }

    @SuppressLint("SetTextI18n")
    override fun initData() {
        auth = Firebase.auth
        credentialManager = CredentialManager.Companion.create(mContext!!)
        
        // Setup publisher region model observers
        setupPublisherRegionObservers()
        
        // Load publisher regions from API
        publisherRegionModel.loadPublisherRegions()
        
        // Header simplified per new design
        mViewBinding.hisLayout.setOnClickListener {
            val readingHistoryFragment = ReadingHistoryBottomSheetFragment.newInstance() // Browsing History
            readingHistoryFragment.show(childFragmentManager, "ReadingHistory")
        }
        mViewBinding.collectLayout.setOnClickListener {
            val savedArticlesFragment = SavedArticlesBottomSheetFragment.newInstance()
            savedArticlesFragment.show(childFragmentManager, "SavedArticles")
        }

        mViewBinding.aboutLayout.setOnClickListener {
            val intent = Intent(mContext, AboutActivity::class.java)
            startActivity(intent)
            activity?.overridePendingTransition(com.anssy.znewspro.R.anim.fade_in, com.anssy.znewspro.R.anim.fade_out)
        }
        
        // Subscription layout click listener
        mViewBinding.subscriptionLayout.setOnClickListener {
            val isPro = myModel.myEntry.value?.data?.isPro == true
            val subscriptionFragment = SubscriptionBottomSheetFragment.newInstance(isPro)
            subscriptionFragment.show(childFragmentManager, "Subscription")
        }
        
        // Language settings - open app settings instead of in-app language change
        mViewBinding.languageLayout.setOnClickListener {
            openAppSettings()
        }
        
        // Notifications settings with dropdown
        mViewBinding.notificationsLayout.setOnClickListener {
            toggleDropdown("notifications")
        }
        
        // Media Region settings with dropdown
        mViewBinding.mediaRegionLayout.setOnClickListener {
            toggleDropdown("mediaRegion")
        }
        
        // Landing Page settings with dropdown
        mViewBinding.landingPageLayout.setOnClickListener {
            toggleDropdown("landingPage")
        }
        
        // Setup landing page selection
        setupLandingPageSelection()
        
        // Article Opening Method settings with dropdown
        mViewBinding.articleOpeningMethodLayout.setOnClickListener {
            toggleDropdown("articleOpeningMethod")
        }
        
        // Setup article opening method selection
        setupArticleOpeningMethodSelection()
        
        // Appearance settings with dropdown
        mViewBinding.appearanceLayout.setOnClickListener {
            toggleDropdown("appearance")
        }
        
        // App Icon settings with dropdown
        mViewBinding.appIconLayout.setOnClickListener {
            toggleDropdown("appIcon")
        }
        
        mViewBinding.feedbackLayout.setOnClickListener { showFeedbackPopup() }
        
        // Show Tips Again button - show coming soon message
        mViewBinding.showTipsLayout.setOnClickListener {
            ToastUtils.showShortToast(mContext!!, getString(R.string.tutorial_coming_soon))
        }
        
        // Reset Settings button - show confirmation dialog
        mViewBinding.resetSettingsLayout.setOnClickListener {
            SystemDialogUtils.showAlertDialog(
                requireContext(),
                getString(R.string.account_menu_reset_settings_confirmation_title),
                getString(R.string.account_menu_reset_settings_confirmation_message),
                getString(R.string.account_menu_reset_settings_confirmation_confirm),
                getString(R.string.account_menu_reset_settings_confirmation_cancel),
                isDestructive = true,
                onPositiveClick = {
                    resetAllSettings()
                }
            )
        }
        
        // Setup social media click listeners
        setupSocialMediaClicks()
        
        // Setup report patterns toggle
        setupReportPatternsToggle()
        
        iniModel()
        mViewBinding.logoutLayout.setOnClickListener {
            SystemDialogUtils.showAlertDialog(
                requireContext(),
                getString(R.string.dialog_title_reminder),
                getString(R.string.logout_confirmation_message),
                getString(R.string.confirm),
                onPositiveClick = {
                    loginModel.outLoginApp()
                }
            )
        }
        
        // Initialize appearance mode selection
        setupAppearanceModeSelection()
        
        // Initialize app icon selection
        setupAppIconSelection()
    }

    private fun toggleDropdown(dropdownType: String) {
        if (activeDropdown == dropdownType) {
            // Close current dropdown
            closeDropdown(dropdownType)
            activeDropdown = null
        } else {
            // Close any other open dropdown first
            if (activeDropdown != null) {
                closeDropdown(activeDropdown!!)
            }
            // Open new dropdown
            openDropdown(dropdownType)
            activeDropdown = dropdownType
        }
    }
    
    private fun openDropdown(dropdownType: String) {
        // Begin transition on the parent container
        TransitionManager.beginDelayedTransition(mViewBinding.root as ViewGroup, dropdownTransition)
        
        when (dropdownType) {
            "notifications" -> {
                mViewBinding.notificationsDropdown.visibility = View.VISIBLE
                mViewBinding.notificationsChevron.setImageResource(R.drawable.ic_chevron_up_24)
            }
            "mediaRegion" -> {
                setupMediaRegionGrid()
                mViewBinding.mediaRegionDropdown.visibility = View.VISIBLE
                mViewBinding.mediaRegionChevron.setImageResource(R.drawable.ic_chevron_up_24)
            }
            "landingPage" -> {
                setupLandingPageGrid()
                mViewBinding.landingPageDropdown.visibility = View.VISIBLE
                mViewBinding.landingPageChevron.setImageResource(R.drawable.ic_chevron_up_24)
            }
            "articleOpeningMethod" -> {
                setupArticleOpeningMethodGrid()
                mViewBinding.articleOpeningMethodDropdown.visibility = View.VISIBLE
                mViewBinding.articleOpeningMethodChevron.setImageResource(R.drawable.ic_chevron_up_24)
            }
            "appearance" -> {
                mViewBinding.appearanceDropdown.visibility = View.VISIBLE
                mViewBinding.appearanceChevron.setImageResource(R.drawable.ic_chevron_up_24)
            }
            "appIcon" -> {
                mViewBinding.appIconDropdown.visibility = View.VISIBLE
                mViewBinding.appIconChevron.setImageResource(R.drawable.ic_chevron_up_24)
            }
        }
    }
    
    private fun closeDropdown(dropdownType: String) {
        // Begin transition on the parent container
        TransitionManager.beginDelayedTransition(mViewBinding.root as ViewGroup, dropdownTransition)
        
        when (dropdownType) {
            "notifications" -> {
                mViewBinding.notificationsDropdown.visibility = View.GONE
                mViewBinding.notificationsChevron.setImageResource(R.drawable.ic_chevron_down_24)
            }
            "mediaRegion" -> {
                mViewBinding.mediaRegionDropdown.visibility = View.GONE
                mViewBinding.mediaRegionChevron.setImageResource(R.drawable.ic_chevron_down_24)
            }
            "landingPage" -> {
                mViewBinding.landingPageDropdown.visibility = View.GONE
                mViewBinding.landingPageChevron.setImageResource(R.drawable.ic_chevron_down_24)
            }
            "articleOpeningMethod" -> {
                mViewBinding.articleOpeningMethodDropdown.visibility = View.GONE
                mViewBinding.articleOpeningMethodChevron.setImageResource(R.drawable.ic_chevron_down_24)
            }
            "appearance" -> {
                mViewBinding.appearanceDropdown.visibility = View.GONE
                mViewBinding.appearanceChevron.setImageResource(R.drawable.ic_chevron_down_24)
            }
            "appIcon" -> {
                mViewBinding.appIconDropdown.visibility = View.GONE
                mViewBinding.appIconChevron.setImageResource(R.drawable.ic_chevron_down_24)
            }
        }
    }
    
    private fun setupMediaRegionGrid() {
        // Set up click listeners for all media region cards
        mViewBinding.hongKongCard.setOnClickListener {
            toggleMediaRegion("hongKong")
        }
        mViewBinding.mainlandChinaCard.setOnClickListener {
            toggleMediaRegion("mainlandChina")
        }
        mViewBinding.unitedKingdomCard.setOnClickListener {
            toggleMediaRegion("unitedKingdom")
        }
        mViewBinding.unitedStatesCard.setOnClickListener {
            toggleMediaRegion("unitedStates")
        }
        mViewBinding.asiaOtherCard.setOnClickListener {
            toggleMediaRegion("asiaOther")
        }
        mViewBinding.europeOtherCard.setOnClickListener {
            toggleMediaRegion("europeOther")
        }
        
    }
    
    
    private fun setupPublisherRegionObservers() {
        // Observe publisher regions data
        publisherRegionModel.publisherRegions.observe(this, Observer { regionsEntry ->
            regionsEntry?.let {
                Log.d("MyFrag", "Publisher regions updated: code=${it.code}, data=${it.data}, selected=${it.data?.selected}")
                if (it.code == 200 && it.data != null) {
                    updateRegionUI(it)
                } else {
                    Log.e("MyFrag", "Invalid publisher regions data: code=${it.code}, data is null=${it.data == null}")
                }
            }
        })
        
        // Observe loading state
        publisherRegionModel.isLoading.observe(this, Observer { isLoading ->
            // Handle loading state if needed
            if (isLoading) {
                Log.d("MyFrag", "Loading publisher regions...")
            }
        })
        
        // Observe error messages - only show toast for actual errors, not during initial load
        publisherRegionModel.errorMessage.observe(this, Observer { errorMessage ->
            errorMessage?.let {
                // Only log the error, don't show toast on initial load to avoid annoying users
                // Toast will be shown if user tries to update a region and it fails
                Log.e("MyFrag", "Publisher region error: $it")
            }
        })
        
        // Observe region update results
        publisherRegionModel.regionUpdateResult.observe(this, Observer { result ->
            result?.let {
                if (it.code == 200) {
                    ToastUtils.showShortToast(mContext!!, "Region updated")
                } else {
                    ToastUtils.showShortToast(mContext!!, "Failed to update region: ${it.msg}")
                }
            }
        })
    }
    
    private fun updateRegionUI(regionsEntry: com.anssy.znewspro.entry.PublisherRegionEntry) {
        val selectedRegions = regionsEntry.data?.selected ?: emptyList()
        val availableRegions = regionsEntry.data?.regions ?: emptyList()
        
        // Debug logging to identify tag format discrepancies
        Log.d("MyFrag", "=== REGION DEBUG START ===")
        Log.d("MyFrag", "Selected regions from API: $selectedRegions")
        Log.d("MyFrag", "Available regions from API: ${availableRegions.map { "${it.tag} -> ${it.displayName}" }}")
        
        // Log expected tags from mapping
        val expectedAsiaOtherTag = RegionMappingUtils.getApiTag("asiaOther")
        val expectedEuropeOtherTag = RegionMappingUtils.getApiTag("europeOther")
        Log.d("MyFrag", "Expected asiaOther API tag: $expectedAsiaOtherTag")
        Log.d("MyFrag", "Expected europeOther API tag: $expectedEuropeOtherTag")
        
        // Check if the expected tags exist in available regions
        val asiaOtherInAvailable = availableRegions.any { it.tag == expectedAsiaOtherTag }
        val europeOtherInAvailable = availableRegions.any { it.tag == expectedEuropeOtherTag }
        Log.d("MyFrag", "asiaOther tag found in available: $asiaOtherInAvailable")
        Log.d("MyFrag", "europeOther tag found in available: $europeOtherInAvailable")
        
        // Check if they're selected
        val asiaOtherSelected = selectedRegions.contains(expectedAsiaOtherTag)
        val europeOtherSelected = selectedRegions.contains(expectedEuropeOtherTag)
        Log.d("MyFrag", "asiaOther is selected: $asiaOtherSelected")
        Log.d("MyFrag", "europeOther is selected: $europeOtherSelected")
        Log.d("MyFrag", "=== REGION DEBUG END ===")
        
        // Update UI for each region based on API data
        updateRegionCard("hongKong", mViewBinding.hongKongCard, selectedRegions)
        updateRegionCard("mainlandChina", mViewBinding.mainlandChinaCard, selectedRegions)
        updateRegionCard("unitedKingdom", mViewBinding.unitedKingdomCard, selectedRegions)
        updateRegionCard("unitedStates", mViewBinding.unitedStatesCard, selectedRegions)
        updateRegionCard("asiaOther", mViewBinding.asiaOtherCard, selectedRegions)
        updateRegionCard("europeOther", mViewBinding.europeOtherCard, selectedRegions)
    }
    
    private fun updateRegionCard(uiRegionName: String, card: com.google.android.material.card.MaterialCardView, selectedRegions: List<String>) {
        val apiTag = RegionMappingUtils.getApiTag(uiRegionName)
        
        // Check if region is selected using flexible matching
        // This handles potential API tag format variations (e.g., asia-others vs asia_others)
        val isSelected = apiTag?.let { expectedTag ->
            selectedRegions.any { actualTag ->
                // First try exact match
                actualTag == expectedTag ||
                // Then try normalized comparison for asia/europe other regions
                RegionMappingUtils.isTagMatchingRegion(actualTag, uiRegionName)
            }
        } ?: false
        
        // Debug logging for asia-others and europe-others
        if (uiRegionName == "asiaOther" || uiRegionName == "europeOther") {
            Log.d("MyFrag", "updateRegionCard: $uiRegionName -> apiTag='$apiTag', isSelected=$isSelected, selectedRegions=$selectedRegions")
        }
        
        // Update card background and stroke color based on selection state
        if (isSelected) {
            card.setCardBackgroundColor(mContext!!.getColor(R.color.media_region_card_selected))
            card.strokeColor = mContext!!.getColor(R.color.media_region_stroke_selected)
        } else {
            card.setCardBackgroundColor(mContext!!.getColor(R.color.media_region_card_unselected))
            card.strokeColor = mContext!!.getColor(R.color.divider_color) // Use divider color for unselected state (adapts to light/dark mode)
        }
    }

    private fun toggleMediaRegion(region: String) {
        // Get API tag for the region
        val apiTag = RegionMappingUtils.getApiTag(region)
        if (apiTag == null) {
            Log.e("MyFrag", "Invalid region name: $region")
            return
        }
        
        Log.d("MyFrag", "=== TOGGLE REGION DEBUG ===")
        Log.d("MyFrag", "UI region name: $region")
        Log.d("MyFrag", "API tag being sent: $apiTag")
        
        // Check current selection state
        val isCurrentlySelected = publisherRegionModel.isRegionSelected(apiTag)
        Log.d("MyFrag", "Is currently selected: $isCurrentlySelected")
        Log.d("MyFrag", "Action to be taken: ${if (isCurrentlySelected) "REMOVE" else "ADD"}")
        
        // Toggle the region through the API
        publisherRegionModel.toggleRegion(apiTag, isCurrentlySelected)
    }
    
    
    private fun setupAppearanceModeSelection() {
        // Restore the saved theme selection
        restoreThemeSelection()
        
        mViewBinding.systemModeLayout.setOnClickListener {
            selectAppearanceMode("system")
        }
        mViewBinding.lightModeLayout.setOnClickListener {
            selectAppearanceMode("light")
        }
        mViewBinding.darkModeLayout.setOnClickListener {
            selectAppearanceMode("dark")
        }
    }
    
    private fun selectAppearanceMode(mode: String) {
        // Save the theme preference
        ThemeManager.saveTheme(mContext!!, mode)
        
        // Apply the theme immediately
        ThemeManager.applyTheme(mode)
        
        // Update UI selection state
        updateThemeSelectionUI(mode)
        
        // Show confirmation
        val modeText = when (mode) {
            "system" -> getString(R.string.theme_system)
            "light" -> getString(R.string.theme_light)
            "dark" -> getString(R.string.theme_dark)
            else -> mode
        }
        ToastUtils.showShortToast(mContext!!, "Theme: $modeText")
        
        // Close the dropdown
        closeDropdown("appearance")
        activeDropdown = null
        
        // Simply recreate the activity to apply theme changes
        requireActivity().recreate()
    }
    
    private fun restoreThemeSelection() {
        // Get the current saved theme
        val currentTheme = ThemeManager.getCurrentTheme(mContext!!)
        
        // Update UI to reflect the saved theme
        updateThemeSelectionUI(currentTheme)
    }
    
    private fun updateThemeSelectionUI(selectedMode: String) {
        // Reset all appearance options to unselected state
        resetAppearanceSelection()
        
        // Apply selected state to the chosen mode
        when (selectedMode) {
            "system" -> {
                mViewBinding.systemModeLayout.background = ContextCompat.getDrawable(mContext!!, R.drawable.appearance_option_selected_background)
                mViewBinding.systemIconContainer.background = ContextCompat.getDrawable(mContext!!, R.drawable.appearance_icon_selected_background)
                mViewBinding.systemThemeText.setTypeface(null, android.graphics.Typeface.BOLD)
            }
            "light" -> {
                mViewBinding.lightModeLayout.background = ContextCompat.getDrawable(mContext!!, R.drawable.appearance_option_selected_background)
                mViewBinding.lightIconContainer.background = ContextCompat.getDrawable(mContext!!, R.drawable.appearance_icon_selected_background)
                mViewBinding.lightThemeText.setTypeface(null, android.graphics.Typeface.BOLD)
            }
            "dark" -> {
                mViewBinding.darkModeLayout.background = ContextCompat.getDrawable(mContext!!, R.drawable.appearance_option_selected_background)
                mViewBinding.darkIconContainer.background = ContextCompat.getDrawable(mContext!!, R.drawable.appearance_icon_selected_background)
                mViewBinding.darkThemeText.setTypeface(null, android.graphics.Typeface.BOLD)
            }
        }
    }
    
    private fun resetAppearanceSelection() {
        // Reset option backgrounds to unselected state
        mViewBinding.systemModeLayout.background = ContextCompat.getDrawable(mContext!!, R.drawable.appearance_option_background)
        mViewBinding.lightModeLayout.background = ContextCompat.getDrawable(mContext!!, R.drawable.appearance_option_background)
        mViewBinding.darkModeLayout.background = ContextCompat.getDrawable(mContext!!, R.drawable.appearance_option_background)
        
        // Reset icon backgrounds to unselected state
        mViewBinding.systemIconContainer.background = ContextCompat.getDrawable(mContext!!, R.drawable.appearance_icon_background)
        mViewBinding.lightIconContainer.background = ContextCompat.getDrawable(mContext!!, R.drawable.appearance_icon_background)
        mViewBinding.darkIconContainer.background = ContextCompat.getDrawable(mContext!!, R.drawable.appearance_icon_background)
        
        // Reset text styling to normal (unbolded)
        mViewBinding.systemThemeText.setTypeface(null, android.graphics.Typeface.NORMAL)
        mViewBinding.lightThemeText.setTypeface(null, android.graphics.Typeface.NORMAL)
        mViewBinding.darkThemeText.setTypeface(null, android.graphics.Typeface.NORMAL)
    }
    
    private fun setupAppIconSelection() {
        // Restore the saved app icon selection
        restoreAppIconSelection()
        
        mViewBinding.defaultIconLayout.setOnClickListener {
            selectAppIcon("default")
        }
        mViewBinding.alternateIconLayout.setOnClickListener {
            selectAppIcon("alternate")
        }
    }
    
    private fun selectAppIcon(icon: String) {
        // Change the actual app icon using AppIconManager
        val success = AppIconManager.changeAppIcon(mContext!!, icon)
        
        if (success) {
            // Update UI to show selection
            updateAppIconSelectionUI(icon)
            
            // Show success message
            val message = when (icon) {
                "default" -> "Icon: light\nRequires app restart"
                "alternate" -> "Icon: dark\nRequires app restart"
                else -> "Icon updated\nRequires app restart"
            }
            ToastUtils.showShortToast(mContext!!, message)
            
            // Close dropdown
            closeDropdown("appIcon")
            activeDropdown = null
            
            // Verify the icon change was successful
            if (AppIconManager.isIconActive(mContext!!, icon)) {
                Log.d("MyFrag", "App icon change verified successfully")
            } else {
                Log.w("MyFrag", "App icon change may not have taken effect")
            }
        } else {
            // Show error message
            ToastUtils.showShortToast(mContext!!, "Failed to change app icon. Please try again.")
        }
    }
    

    
    private fun updateAppIconSelectionUI(selectedIcon: String) {
        // Reset all app icon options to unselected state
        resetAppIconSelection()
        
        // Apply selected state to the chosen icon
        when (selectedIcon) {
            "default" -> {
                mViewBinding.defaultIconLayout.background = ContextCompat.getDrawable(mContext!!, R.drawable.appearance_option_selected_background)
                mViewBinding.defaultIconContainer.background = ContextCompat.getDrawable(mContext!!, R.drawable.rounded_app_icon_default_selected)
                mViewBinding.defaultIconText.setTypeface(null, android.graphics.Typeface.BOLD)
            }
            "alternate" -> {
                mViewBinding.alternateIconLayout.background = ContextCompat.getDrawable(mContext!!, R.drawable.appearance_option_selected_background)
                mViewBinding.alternateIconContainer.background = ContextCompat.getDrawable(mContext!!, R.drawable.rounded_app_icon_alternate_selected)
                mViewBinding.alternateIconText.setTypeface(null, android.graphics.Typeface.BOLD)
            }
        }
    }
    
    private fun resetAppIconSelection() {
        // Reset option backgrounds to unselected state
        mViewBinding.defaultIconLayout.background = ContextCompat.getDrawable(mContext!!, R.drawable.appearance_option_background)
        mViewBinding.alternateIconLayout.background = ContextCompat.getDrawable(mContext!!, R.drawable.appearance_option_background)
        
        // Reset icon container backgrounds to unselected state
        mViewBinding.defaultIconContainer.background = ContextCompat.getDrawable(mContext!!, R.drawable.rounded_app_icon_default)
        mViewBinding.alternateIconContainer.background = ContextCompat.getDrawable(mContext!!, R.drawable.rounded_app_icon_alternate)
        
        // Reset text styles to normal (unbolded)
        mViewBinding.defaultIconText.setTypeface(null, android.graphics.Typeface.NORMAL)
        mViewBinding.alternateIconText.setTypeface(null, android.graphics.Typeface.NORMAL)
    }
    
    private fun restoreAppIconSelection() {
        // Get the currently selected app icon from AppIconManager
        val currentIcon = AppIconManager.getCurrentIconType(mContext!!)
        
        // Icons are now set as backgrounds of the containers in the layout XML
        
        // Update UI to reflect the saved preference
        updateAppIconSelectionUI(currentIcon)
    }
    
    private fun openAppSettings() {
        try {
            // For Android 13+ (API 33+), try to open app-specific language settings directly
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                try {
                    val intent = Intent(Settings.ACTION_APP_LOCALE_SETTINGS)
                    val uri = android.net.Uri.fromParts("package", requireContext().packageName, null)
                    intent.data = uri
                    startActivity(intent)
                    ToastUtils.showShortToast(mContext!!, getString(R.string.language_settings_help_modern))
                    return
                } catch (e: Exception) {
                    // Fallback to app details if app locale settings not available
                }
            }
            
            // Fallback: Open app details where language setting should be available
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
            val uri = android.net.Uri.fromParts("package", requireContext().packageName, null)
            intent.data = uri
            startActivity(intent)
            
            // Show appropriate help message based on Android version
            val helpMessage = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                getString(R.string.language_settings_help_modern)
            } else {
                getString(R.string.language_settings_help)
            }
            ToastUtils.showShortToast(mContext!!, helpMessage)
            
        } catch (e: Exception) {
            // Final fallback to general settings
            try {
                val intent = Intent(Settings.ACTION_SETTINGS)
                startActivity(intent)
                ToastUtils.showShortToast(mContext!!, getString(R.string.language_settings_fallback))
            } catch (e2: Exception) {
                ToastUtils.showShortToast(mContext!!, getString(R.string.language_settings_error))
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Refresh subscription status when fragment becomes visible
        // This ensures the UI is updated after subscription changes in the bottom sheet
        myModel.queryMyFormation()
    }
    
    @SuppressLint("SetTextI18n")
    private fun iniModel(){
        myModel.queryMyFormation()
        myModel.myEntry.observe(viewLifecycleOwner){
            if (it!=null){
                if (it.code== Constants.SUCCESS_CODE){
                    // Display username from API response
                    mViewBinding.userNameTv.text = it.data.username ?: it.data.email ?: ""
                    // Display email if available
                    mViewBinding.userEmailTv.text = it.data.email ?: ""
                    
                    // Update subscription UI based on isPro status
                    updateSubscriptionUI(it.data.isPro == true)
                }else{
                    if (it.code==1000){
                        ToastUtils.showShortToast(mContext!!,getString(R.string.server_error_message))
                    }else{
                        ToastUtils.showShortToast(mContext!!,it.msg)
                    }
                }
            }
        }
        // Old web-based About page removed; no network observer needed
        loginModel.outLoginEntry.observe(viewLifecycleOwner){
            // Check if third-party login BEFORE clearing SharedPreferences
            val isThirdPartyLogin = SharedPreferenceUtils.getBoolean(mContext, "thirdLogin")
            
            // Sign out from all third-party providers
            if (isThirdPartyLogin) {
                signOut()
            }
            
            // Clear all SharedPreferences (including login state)
            SharedPreferenceUtils.clear(mContext)
            
            // Clear cookie preferences explicitly
            val cookiePrefs: SharedPreferences = mContext!!.getSharedPreferences("cookie_prefs", android.content.Context.MODE_PRIVATE)
            cookiePrefs.edit().clear().apply()
            
            // Ensure isLogin is false
            SharedPreferenceUtils.saveBoolean(mContext, "isLogin", false)
            
            // Navigate to login page
            val intent = Intent(mContext, LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            requireActivity().finishAffinity()
        }
    }

    private fun signOut() {
        // Firebase sign out
        auth.signOut()
        
        // Google Sign-In sign out
        try {
            val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build()
            val googleSignInClient = GoogleSignIn.getClient(requireContext(), gso)
            googleSignInClient.signOut().addOnCompleteListener {
                Log.d(TAG, "Google Sign-In signed out")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error signing out from Google: ${e.message}")
        }
        
        // Facebook LoginManager sign out
        try {
            LoginManager.getInstance().logOut()
            Log.d(TAG, "Facebook signed out")
        } catch (e: Exception) {
            Log.e(TAG, "Error signing out from Facebook: ${e.message}")
        }

        // Clear credential manager state
        lifecycleScope.launch {
            try {
                val clearRequest = ClearCredentialStateRequest()
                credentialManager.clearCredentialState(clearRequest)
                Log.d(TAG, "Credential manager cleared")
            } catch (e: ClearCredentialException) {
                Log.e(TAG, "Couldn't clear user credentials: ${e.localizedMessage}")
            }
        }
    }

    private fun showFeedbackPopup() {
        val feedbackBottomSheet = FeedbackBottomSheetFragment.newInstance { feedbackContent ->
            // For now, just show success message like the original implementation
            // This could be enhanced to actually submit feedback to backend
            ToastUtils.showShortToast(mContext!!, getString(R.string.success_message))
            true // Return success like iOS
        }
        feedbackBottomSheet.show(childFragmentManager, "FeedbackBottomSheet")
    }

    /**
     * Refresh the fragment data
     */
    fun refreshData() {
        Log.d("MyFrag", "Refreshing my fragment data")
        
        // Check if fragment is properly attached before accessing ViewModels
        if (!isAdded || isDetached || activity == null) {
            Log.w("MyFrag", "Fragment not properly attached, skipping refresh")
            return
        }
        
        // Scroll to top of the page
        mViewBinding.profileScrollView.smoothScrollTo(0, 0)
        
        myModel.queryMyFormation()
    }
    
    private fun setupSocialMediaClicks() {
        // Instagram
        mViewBinding.instagramIcon.apply {
            isClickable = true
            isFocusable = true
            setOnClickListener {
                openUrl("https://www.instagram.com/zonenews.io/")
            }
        }
        
        // Facebook
        mViewBinding.facebookIcon.apply {
            isClickable = true
            isFocusable = true
            setOnClickListener {
                openUrl("https://www.facebook.com/profile.php?id=61580071810702#")
            }
        }
        
        // X (Twitter)
        mViewBinding.xIcon.apply {
            isClickable = true
            isFocusable = true
            setOnClickListener {
                openUrl("https://x.com/zonenews_io")
            }
        }
        
        // LinkedIn
        mViewBinding.linkedinIcon.apply {
            isClickable = true
            isFocusable = true
            setOnClickListener {
                openUrl("https://www.linkedin.com/company/zonenews/")
            }
        }
        
        // Website
        mViewBinding.websiteIcon.apply {
            isClickable = true
            isFocusable = true
            setOnClickListener {
                openUrl("https://zonenews.io")
            }
        }
    }
    
    private fun openUrl(url: String) {
        val intent = Intent(Intent.ACTION_VIEW, android.net.Uri.parse(url))
        try {
            startActivity(intent)
        } catch (e: android.content.ActivityNotFoundException) {
            e.printStackTrace()
        }
    }
    
    private fun setupReportPatternsToggle() {
        // Load saved state (default is false/off)
        val isEnabled = SharedPreferenceUtils.getBoolean(mContext, "report_patterns_enabled")
        mViewBinding.reportPatternsSwitch.isChecked = isEnabled
        
        // Set up listener to save state when toggled
        mViewBinding.reportPatternsSwitch.setOnCheckedChangeListener { _, isChecked ->
            SharedPreferenceUtils.saveBoolean(mContext, "report_patterns_enabled", isChecked)
        }
    }
    
    private fun setupLandingPageSelection() {
        // Restore the saved landing page selection
        restoreLandingPageSelection()
    }
    
    private fun setupLandingPageGrid() {
        // Set up click listeners for all landing page cards
        mViewBinding.landingPageTodayCard.setOnClickListener {
            selectLandingPage("today")
        }
        mViewBinding.landingPageHongKongCard.setOnClickListener {
            selectLandingPage("hongKong")
        }
        mViewBinding.landingPageChinaCard.setOnClickListener {
            selectLandingPage("china")
        }
        mViewBinding.landingPageYourFeedCard.setOnClickListener {
            selectLandingPage("yourFeed")
        }
        mViewBinding.landingPageLevityModeCard.setOnClickListener {
            // Show coming soon message for Levity Mode
            ToastUtils.showShortToast(mContext!!, getString(R.string.levity_mode_coming_soon))
        }
    }
    
    private fun selectLandingPage(landingPage: String) {
        // Save the landing page preference
        SharedPreferenceUtils.saveString(mContext, "landing_page", landingPage)
        
        // Update UI selection state
        updateLandingPageSelectionUI(landingPage)
        
        // Show confirmation
        val pageText = when (landingPage) {
            "today" -> getString(R.string.landing_page_today)
            "hongKong" -> getString(R.string.landing_page_hong_kong)
            "china" -> getString(R.string.landing_page_china)
            "yourFeed" -> getString(R.string.landing_page_your_feed)
            else -> landingPage
        }
        ToastUtils.showShortToast(mContext!!, "Landing page: $pageText")
        
        // Don't close the dropdown - let user see the selection and manually close if needed
    }
    
    private fun restoreLandingPageSelection() {
        // Get the current saved landing page (default to "today")
        val currentLandingPage = SharedPreferenceUtils.getString(mContext, "landing_page")
        if (currentLandingPage.isEmpty()) {
            // Set default to "today" if no preference is saved
            SharedPreferenceUtils.saveString(mContext, "landing_page", "today")
            updateLandingPageSelectionUI("today")
        } else {
            updateLandingPageSelectionUI(currentLandingPage)
        }
    }
    
    private fun updateLandingPageSelectionUI(selectedLandingPage: String) {
        // Reset all landing page cards to unselected state
        resetLandingPageSelection()
        
        // Apply selected state to the chosen landing page
        val selectedCard = when (selectedLandingPage) {
            "today" -> mViewBinding.landingPageTodayCard
            "hongKong" -> mViewBinding.landingPageHongKongCard
            "china" -> mViewBinding.landingPageChinaCard
            "yourFeed" -> mViewBinding.landingPageYourFeedCard
            else -> null
        }
        
        selectedCard?.let { card ->
            card.setCardBackgroundColor(mContext!!.getColor(R.color.media_region_card_selected))
            card.strokeColor = mContext!!.getColor(R.color.media_region_stroke_selected)
        }
    }
    
    private fun resetLandingPageSelection() {
        // Reset all landing page cards to unselected state
        val cards = listOf(
            mViewBinding.landingPageTodayCard,
            mViewBinding.landingPageHongKongCard,
            mViewBinding.landingPageChinaCard,
            mViewBinding.landingPageYourFeedCard,
            mViewBinding.landingPageLevityModeCard
        )
        
        cards.forEach { card ->
            card.setCardBackgroundColor(mContext!!.getColor(R.color.media_region_card_unselected))
            card.strokeColor = mContext!!.getColor(R.color.divider_color)
        }
    }
    
    private fun setupArticleOpeningMethodSelection() {
        // Restore the saved article opening method selection
        restoreArticleOpeningMethodSelection()
    }
    
    private fun setupArticleOpeningMethodGrid() {
        // Set up click listeners for all article opening method cards
        mViewBinding.articleOpeningInAppCard.setOnClickListener {
            selectArticleOpeningMethod("inApp")
        }
        mViewBinding.articleOpeningExternalCard.setOnClickListener {
            selectArticleOpeningMethod("external")
        }
    }
    
    private fun selectArticleOpeningMethod(method: String) {
        // Save the article opening method preference
        SharedPreferenceUtils.saveString(mContext, "article_opening_method", method)
        
        // Update UI selection state
        updateArticleOpeningMethodSelectionUI(method)
        
        // Show confirmation
        val methodText = when (method) {
            "inApp" -> getString(R.string.article_opening_in_app)
            "external" -> getString(R.string.article_opening_external)
            else -> method
        }
        ToastUtils.showShortToast(mContext!!, "Article opening: $methodText")
        
        // Don't close the dropdown - let user see the selection and manually close if needed
    }
    
    private fun restoreArticleOpeningMethodSelection() {
        // Get the current saved article opening method (default to "inApp")
        val currentMethod = SharedPreferenceUtils.getString(mContext, "article_opening_method")
        if (currentMethod.isEmpty()) {
            // Set default to "inApp" if no preference is saved
            SharedPreferenceUtils.saveString(mContext, "article_opening_method", "inApp")
            updateArticleOpeningMethodSelectionUI("inApp")
        } else {
            updateArticleOpeningMethodSelectionUI(currentMethod)
        }
    }
    
    private fun updateArticleOpeningMethodSelectionUI(selectedMethod: String) {
        // Reset all article opening method cards to unselected state
        resetArticleOpeningMethodSelection()
        
        // Apply selected state to the chosen method
        val selectedCard = when (selectedMethod) {
            "inApp" -> mViewBinding.articleOpeningInAppCard
            "external" -> mViewBinding.articleOpeningExternalCard
            else -> null
        }
        
        selectedCard?.let { card ->
            card.setCardBackgroundColor(mContext!!.getColor(R.color.media_region_card_selected))
            card.strokeColor = mContext!!.getColor(R.color.media_region_stroke_selected)
        }
    }
    
    private fun resetArticleOpeningMethodSelection() {
        // Reset all article opening method cards to unselected state
        val cards = listOf(
            mViewBinding.articleOpeningInAppCard,
            mViewBinding.articleOpeningExternalCard
        )
        
        cards.forEach { card ->
            card.setCardBackgroundColor(mContext!!.getColor(R.color.media_region_card_unselected))
            card.strokeColor = mContext!!.getColor(R.color.divider_color)
        }
    }
    
    /**
     * Update subscription UI based on Pro status
     */
    private fun updateSubscriptionUI(isPro: Boolean) {
        if (isPro) {
            // Show Pro Active status
            mViewBinding.subscriptionIcon.setImageResource(R.drawable.ic_verified_24)
            mViewBinding.subscriptionTitle.text = getString(R.string.account_pro_active_title)
            mViewBinding.subscriptionSubtitle.text = getString(R.string.account_pro_active_subtitle)
        } else {
            // Show Upgrade to Pro
            mViewBinding.subscriptionIcon.setImageResource(R.drawable.ic_verified_off_24)
            mViewBinding.subscriptionTitle.text = getString(R.string.account_pro_upgrade_title)
            mViewBinding.subscriptionSubtitle.text = getString(R.string.account_pro_upgrade_subtitle)
        }
    }
    
    /**
     * Reset all app settings to their default values
     */
    private fun resetAllSettings() {
        // Reset theme/appearance to system default
        ThemeManager.saveTheme(mContext!!, "system")
        ThemeManager.applyTheme("system")
        updateThemeSelectionUI("system")
        
        // Reset app icon to default
        AppIconManager.changeAppIcon(mContext!!, "default")
        updateAppIconSelectionUI("default")
        
        // Reset landing page to "today"
        SharedPreferenceUtils.saveString(mContext, "landing_page", "today")
        updateLandingPageSelectionUI("today")
        
        // Reset article opening method to "inApp"
        SharedPreferenceUtils.saveString(mContext, "article_opening_method", "inApp")
        updateArticleOpeningMethodSelectionUI("inApp")
        
        // Reset report patterns toggle to false
        SharedPreferenceUtils.saveBoolean(mContext, "report_patterns_enabled", false)
        mViewBinding.reportPatternsSwitch.isChecked = false
        
        // Reset news detail page card order to default
        val defaultCardOrder = listOf("sentiment", "publisher", "subjectivity", "timeline")
        SharedPreferenceUtils.saveString(mContext, "news_detail_card_order", defaultCardOrder.joinToString(","))
        
        // Reset news detail page card collapsed states to false (all expanded by default)
        val cardIds = listOf("sentiment", "publisher", "subjectivity", "timeline")
        cardIds.forEach { cardId ->
            SharedPreferenceUtils.saveBoolean(mContext, "news_detail_card_collapsed_$cardId", false)
        }
        
        // Reset news detail page summary language preference (will auto-detect app language)
        SharedPreferenceUtils.deleteString(mContext, "news_detail_summary_language")
        
        // Show success message
        ToastUtils.showShortToast(mContext!!, "Settings reset to default")
        
        // Recreate activity to apply theme changes
        requireActivity().recreate()
    }
}

// Data class for media regions
data class MediaRegion(
    val name: String,
    val iconResId: Int,
    var isSelected: Boolean
)

// Adapter for media regions RecyclerView
class MediaRegionAdapter(
    private val regions: List<MediaRegion>,
    private val onRegionToggled: (MediaRegion, Boolean) -> Unit
) : androidx.recyclerview.widget.RecyclerView.Adapter<MediaRegionAdapter.ViewHolder>() {
    
    class ViewHolder(view: View) : androidx.recyclerview.widget.RecyclerView.ViewHolder(view) {
        val regionIcon: ImageView = view.findViewById(R.id.regionIcon)
        val regionName: android.widget.TextView = view.findViewById(R.id.regionName)
        val regionCheckbox: ImageView = view.findViewById(R.id.regionCheckbox)
    }
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_media_region, parent, false)
        return ViewHolder(view)
    }
    
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val region = regions[position]
        holder.regionIcon.setImageResource(region.iconResId)
        holder.regionName.text = region.name
        holder.regionCheckbox.visibility = if (region.isSelected) View.VISIBLE else View.GONE
        
        holder.itemView.setOnClickListener {
            region.isSelected = !region.isSelected
            holder.regionCheckbox.visibility = if (region.isSelected) View.VISIBLE else View.GONE
            onRegionToggled(region, region.isSelected)
        }
    }
    
    override fun getItemCount() = regions.size
}

