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
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.core.content.ContextCompat
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
import com.anssy.znewspro.utils.AppIconManager
import com.anssy.znewspro.utils.RegionMappingUtils
import com.anssy.znewspro.utils.SharedPreferenceUtils
import com.anssy.znewspro.utils.ThemeManager
import com.anssy.znewspro.utils.ToastUtils
import com.anssy.znewspro.utils.Utils
import com.bumptech.glide.Glide
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.anssy.znewspro.utils.SystemDialogUtils
import kotlinx.coroutines.launch
import razerdp.util.PopupUtils.getString

/**
 * @Description 我的
 * @Author yulu
 * @CreateTime 2025年06月30日 09:28:07
 */

class MyFrag : BaseFragment() {
    private lateinit var  mViewBinding:FragMyBinding
    private val loginModel:LoginModel by viewModels()
    private val myModel:MyModel by viewModels()
    private val publisherRegionModel:PublisherRegionModel by viewModels()
    private lateinit var auth: FirebaseAuth
    private lateinit var credentialManager: CredentialManager
    
    // Dropdown state management
    private var activeDropdown: String? = null
    
    
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
        }
        
        // Language settings - open app settings instead of in-app language change
        mViewBinding.languageLayout.setOnClickListener {
            openAppSettings()
        }
        
        // Media Region settings with dropdown
        mViewBinding.mediaRegionLayout.setOnClickListener {
            toggleDropdown("mediaRegion")
        }
        
        // Appearance settings with dropdown
        mViewBinding.appearanceLayout.setOnClickListener {
            toggleDropdown("appearance")
        }
        
        // App Icon settings with dropdown
        mViewBinding.appIconLayout.setOnClickListener {
            toggleDropdown("appIcon")
        }
        
        mViewBinding.feedbackLayout.setOnClickListener { showFeedbackPopup() }
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
        when (dropdownType) {
            "mediaRegion" -> {
                mViewBinding.mediaRegionDropdown.visibility = View.VISIBLE
                mViewBinding.mediaRegionChevron.setImageResource(R.drawable.ic_chevron_down_24)
                // Initialize media region grid
                setupMediaRegionGrid()
            }
            "appearance" -> {
                mViewBinding.appearanceDropdown.visibility = View.VISIBLE
                mViewBinding.appearanceChevron.setImageResource(R.drawable.ic_chevron_down_24)
            }
            "appIcon" -> {
                mViewBinding.appIconDropdown.visibility = View.VISIBLE
                mViewBinding.appIconChevron.setImageResource(R.drawable.ic_chevron_down_24)
            }
        }
    }
    
    private fun closeDropdown(dropdownType: String) {
        when (dropdownType) {
            "mediaRegion" -> {
                mViewBinding.mediaRegionDropdown.visibility = View.GONE
                mViewBinding.mediaRegionChevron.setImageResource(R.drawable.ic_chevron_right_24)
            }
            "appearance" -> {
                mViewBinding.appearanceDropdown.visibility = View.GONE
                mViewBinding.appearanceChevron.setImageResource(R.drawable.ic_chevron_right_24)
            }
            "appIcon" -> {
                mViewBinding.appIconDropdown.visibility = View.GONE
                mViewBinding.appIconChevron.setImageResource(R.drawable.ic_chevron_right_24)
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
                updateRegionUI(it)
            }
        })
        
        // Observe loading state
        publisherRegionModel.isLoading.observe(this, Observer { isLoading ->
            // Handle loading state if needed
            if (isLoading) {
                Log.d("MyFrag", "Loading publisher regions...")
            }
        })
        
        // Observe error messages
        publisherRegionModel.errorMessage.observe(this, Observer { errorMessage ->
            errorMessage?.let {
                ToastUtils.showShortToast(mContext!!, "Error: $it")
                Log.e("MyFrag", "Publisher region error: $it")
            }
        })
        
        // Observe region update results
        publisherRegionModel.regionUpdateResult.observe(this, Observer { result ->
            result?.let {
                if (it.code == 200) {
                    ToastUtils.showShortToast(mContext!!, "Region updated successfully")
                } else {
                    ToastUtils.showShortToast(mContext!!, "Failed to update region: ${it.msg}")
                }
            }
        })
    }
    
    private fun updateRegionUI(regionsEntry: com.anssy.znewspro.entry.PublisherRegionEntry) {
        val selectedRegions = regionsEntry.data?.selected ?: emptyList()
        
        // Update UI for each region based on API data
        updateRegionCard("hongKong", mViewBinding.hongKongCard, mViewBinding.hongKongCheck, selectedRegions)
        updateRegionCard("mainlandChina", mViewBinding.mainlandChinaCard, mViewBinding.mainlandChinaCheck, selectedRegions)
        updateRegionCard("unitedKingdom", mViewBinding.unitedKingdomCard, mViewBinding.unitedKingdomCheck, selectedRegions)
        updateRegionCard("unitedStates", mViewBinding.unitedStatesCard, mViewBinding.unitedStatesCheck, selectedRegions)
        updateRegionCard("asiaOther", mViewBinding.asiaOtherCard, mViewBinding.asiaOtherCheck, selectedRegions)
        updateRegionCard("europeOther", mViewBinding.europeOtherCard, mViewBinding.europeOtherCheck, selectedRegions)
    }
    
    private fun updateRegionCard(uiRegionName: String, card: com.google.android.material.card.MaterialCardView, checkIcon: ImageView, selectedRegions: List<String>) {
        val apiTag = RegionMappingUtils.getApiTag(uiRegionName)
        val isSelected = apiTag?.let { selectedRegions.contains(it) } ?: false
        
        if (isSelected) {
            card.setCardBackgroundColor(mContext!!.getColor(R.color.media_region_card_selected))
            checkIcon.setColorFilter(mContext!!.getColor(R.color.media_region_check_selected))
        } else {
            card.setCardBackgroundColor(mContext!!.getColor(R.color.media_region_card_unselected))
            checkIcon.setColorFilter(mContext!!.getColor(R.color.media_region_check_unselected))
        }
    }

    private fun toggleMediaRegion(region: String) {
        // Get API tag for the region
        val apiTag = RegionMappingUtils.getApiTag(region)
        if (apiTag == null) {
            Log.e("MyFrag", "Invalid region name: $region")
            return
        }
        
        // Check current selection state
        val isCurrentlySelected = publisherRegionModel.isRegionSelected(apiTag)
        
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
                "default" -> "Icon: light"
                "alternate" -> "Icon: dark"
                else -> "Icon updated"
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

    @SuppressLint("SetTextI18n")
    private fun iniModel(){
        myModel.queryMyFormation()
        myModel.myEntry.observe(viewLifecycleOwner){
            if (it!=null){
                if (it.code== Constants.SUCCESS_CODE){
                    Glide.with(mContext!!).load(it.data.profileIcon).error(R.drawable.ease_default_image)
                        .into(mViewBinding.avatarIv)
                    // No name in current API; show ID as name
                    mViewBinding.userNameTv.text = "${getString(R.string.user_id_label)}${it.data.profileID}"
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
            SharedPreferenceUtils.clear(mContext)
            SharedPreferenceUtils.saveBoolean(mContext,"isLogin",false)
            if (SharedPreferenceUtils.getBoolean(mContext,"thirdLogin")){
                signOut()
            }
            val intent =  Intent(mContext,LoginActivity::class.java)
            startActivity(intent)
            requireActivity().finishAffinity()
        }
    }

    private fun signOut() {
        // Firebase sign out
        auth.signOut()

        lifecycleScope.launch {
            try {
                val clearRequest = ClearCredentialStateRequest()
                credentialManager.clearCredentialState(clearRequest)
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

