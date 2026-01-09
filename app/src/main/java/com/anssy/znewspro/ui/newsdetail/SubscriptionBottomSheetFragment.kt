package com.anssy.znewspro.ui.newsdetail

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.lifecycle.ViewModelProvider
import com.anssy.znewspro.R
import com.anssy.znewspro.databinding.FragmentSubscriptionBottomSheetBinding
import com.anssy.znewspro.entry.CommonResponseEntry
import com.anssy.znewspro.model.MyModel
import com.anssy.znewspro.utils.Constants
import com.anssy.znewspro.utils.SystemDialogUtils
import com.anssy.znewspro.utils.ToastUtils
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import androidx.viewpager2.widget.ViewPager2
import android.widget.ImageView
import android.graphics.drawable.GradientDrawable
import androidx.core.content.ContextCompat

class SubscriptionBottomSheetFragment : BottomSheetDialogFragment() {
    
    private var _binding: FragmentSubscriptionBottomSheetBinding? = null
    private val binding get() = _binding!!
    
    private lateinit var myModel: MyModel
    private var isProUser = false
    private lateinit var screenshotsAdapter: ScreenshotsCarouselAdapter
    private var hasShownCancelMessage = false // Track if we've shown cancel message in this session
    private var isRedeemCodeOperation = false // Track if we're currently processing a redeem code operation
    
    companion object {
        fun newInstance(isPro: Boolean = false): SubscriptionBottomSheetFragment {
            return SubscriptionBottomSheetFragment().apply {
                arguments = Bundle().apply {
                    putBoolean("isPro", isPro)
                }
            }
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        isProUser = arguments?.getBoolean("isPro", false) ?: false
    }
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSubscriptionBottomSheetBinding.inflate(inflater, container, false)
        return binding.root
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupViewModel()
        setupScreenshotsCarousel()
        setupViews()
        updateUI()
    }
    
    override fun onStart() {
        super.onStart()
        
        // Force bottom sheet to fully expanded state
        val bottomSheet = dialog?.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)
        bottomSheet?.let {
            val behavior = BottomSheetBehavior.from(it)
            behavior.state = BottomSheetBehavior.STATE_EXPANDED
        }
    }
    
    private fun setupViewModel() {
        myModel = ViewModelProvider(requireActivity())[MyModel::class.java]
        
        // Observe profile data to get subscription status
        myModel.myEntry.observe(viewLifecycleOwner) { response ->
            if (response != null && response.code == Constants.SUCCESS_CODE) {
                val newIsPro = response.data?.isPro == true
                if (newIsPro != isProUser) {
                    isProUser = newIsPro
                    updateUI()
                }
            }
        }
        
        // Observe redeem/cancel response
        myModel.commonResponseEntry.observe(viewLifecycleOwner) { response ->
            if (response != null) {
                if (response.code == Constants.SUCCESS_CODE) {
                    // Refresh profile to get updated subscription status
                    myModel.queryMyFormation()
                    val isRedeem = response.msg?.contains("redeem", ignoreCase = true) == true || 
                                   response.msg?.contains("code", ignoreCase = true) == true
                    val isCancel = response.msg?.contains("cancel", ignoreCase = true) == true
                    
                    // Check if manage page is currently visible
                    val isManagePageVisible = binding.managePageContainer.isVisible
                    
                    // Only handle redeem messages when on upgrade page (not manage page)
                    // This prevents showing redeem success message when opening manage page after upgrade
                    if (isRedeem && (isManagePageVisible || isProUser)) {
                        // User is on manage page or already Pro, so this is a stale redeem message - ignore it
                        return@observe
                    }
                    
                    // Only handle cancel messages when manage page is visible, user is Pro, and we haven't shown it yet
                    // This prevents showing cancel success message when reopening manage page after cancel
                    if (isCancel) {
                        if (!isManagePageVisible || !isProUser || hasShownCancelMessage) {
                            // User is not on manage page, not Pro, or we've already shown the message - ignore it
                            return@observe
                        }
                        // Mark that we've shown the cancel message
                        hasShownCancelMessage = true
                    }
                    
                    val message = when {
                        isRedeem -> getString(R.string.subscription_redeem_success)
                        isCancel -> getString(R.string.subscription_cancel_success)
                        else -> response.msg ?: getString(R.string.subscription_redeem_success)
                    }
                    ToastUtils.showShortToast(requireContext(), message)
                } else {
                    // Check if this is a redeem code error
                    // If we're in a redeem code operation context, show code invalid
                    val isRedeemError = isRedeemCodeOperation || 
                                       response.msg?.contains("redeem", ignoreCase = true) == true || 
                                       response.msg?.contains("code", ignoreCase = true) == true
                    
                    // Reset the flag after checking
                    isRedeemCodeOperation = false
                    
                    val errorMessage = when {
                        isRedeemError -> getString(R.string.subscription_redeem_failed)
                        response.code == 1000 -> getString(R.string.server_error_message)
                        else -> response.msg ?: getString(R.string.server_error_message)
                    }
                    ToastUtils.showShortToast(requireContext(), errorMessage)
                }
            }
        }
        
        // Load current profile
        myModel.queryMyFormation()
    }
    
    private fun setupScreenshotsCarousel() {
        screenshotsAdapter = ScreenshotsCarouselAdapter(4)
        binding.screenshotsViewPager.adapter = screenshotsAdapter
        
        // Set up page indicator dots
        setupPageIndicators()
        
        // Listen to page changes to update indicators
        binding.screenshotsViewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                updatePageIndicators(position)
            }
        })
    }
    
    private fun setupPageIndicators() {
        val container = binding.pageIndicatorContainer
        container.removeAllViews()
        
        val dotSize = dpToPx(requireContext(), 8)
        val dotMargin = dpToPx(requireContext(), 4)
        
        for (i in 0 until 4) {
            val dot = ImageView(requireContext()).apply {
                layoutParams = android.view.ViewGroup.MarginLayoutParams(dotSize, dotSize).apply {
                    setMargins(dotMargin, 0, dotMargin, 0)
                }
                setImageDrawable(createPageIndicatorDot(false))
            }
            container.addView(dot)
        }
        
        // Set first dot as selected
        updatePageIndicators(0)
    }
    
    private fun updatePageIndicators(selectedPosition: Int) {
        val container = binding.pageIndicatorContainer
        for (i in 0 until container.childCount) {
            val dot = container.getChildAt(i) as? ImageView
            dot?.setImageDrawable(createPageIndicatorDot(i == selectedPosition))
        }
    }
    
    private fun createPageIndicatorDot(isSelected: Boolean): GradientDrawable {
        return GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            setColor(
                if (isSelected) {
                    ContextCompat.getColor(requireContext(), R.color.colorTextDeep)
                } else {
                    ContextCompat.getColor(requireContext(), R.color.colorTextSmall)
                }
            )
        }
    }
    
    private fun dpToPx(context: android.content.Context, dp: Int): Int {
        return (dp * context.resources.displayMetrics.density).toInt()
    }
    
    private fun setupViews() {
        // Done button
        binding.doneButton.setOnClickListener {
            dismiss()
        }
        
        // Subscribe button (shown if not Pro)
        binding.subscribeButton.setOnClickListener {
            // For now, just show a message - actual subscription purchase would be implemented here
            ToastUtils.showShortToast(requireContext(), getString(R.string.subscription_subscribe_button))
        }
        
        // Cancel subscription button (shown if Pro) - in manage page
        binding.cancelSubscriptionLayout.setOnClickListener {
            showCancelSubscriptionConfirmation()
        }
        
        // Redeem code button (upgrade page)
        binding.redeemCodeLayout.setOnClickListener {
            showRedeemCodeDialog()
        }
        
        // Restore purchases button (upgrade page)
        binding.restorePurchasesLayout.setOnClickListener {
            // For Android, this would typically restore purchases from Google Play
            // For now, just show a message
            ToastUtils.showShortToast(requireContext(), getString(R.string.subscription_restore_success))
        }
        
        // Restore purchases button (manage page)
        binding.restorePurchasesLayoutManage.setOnClickListener {
            // For Android, this would typically restore purchases from Google Play
            // For now, just show a message
            ToastUtils.showShortToast(requireContext(), getString(R.string.subscription_restore_success))
        }
        
        // Contact support button (manage page)
        binding.contactSupportLayout.setOnClickListener {
            openContactSupport()
        }
    }
    
    private fun updateUI() {
        if (isProUser) {
            // Show Manage Subscription page
            binding.upgradePageContainer.isVisible = false
            binding.managePageContainer.isVisible = true
            binding.headerTextView.text = getString(R.string.manage_subscription_title)
            
            // Update manage subscription page data
            updateManageSubscriptionPage()
        } else {
            // Show Upgrade page
            binding.upgradePageContainer.isVisible = true
            binding.managePageContainer.isVisible = false
            binding.headerTextView.text = getString(R.string.subscription_title)
        }
        
        // Reset scroll position to top after upgrade/downgrade
        binding.scrollView.post {
            binding.scrollView.smoothScrollTo(0, 0)
        }
    }
    
    private fun updateManageSubscriptionPage() {
        // Update expiration date (if available from API)
        // For now, we'll leave it empty or show a placeholder
        // The API doesn't currently return expiration date, so we'll handle it when available
        
        // Update auto-renewal status (if available)
        // For now, we'll hide it since API doesn't provide this info
        binding.autoRenewalRow.isVisible = false
        
        // Update subscription details
        binding.detailsPlanValue.text = getString(R.string.subscription_plan_title)
        binding.detailsPriceValue.text = getString(R.string.subscription_plan_price)
        binding.detailsBillingValue.text = "--" // Will be updated when API provides expiration date
        
        // Hide transaction ID for now (not available from API)
        binding.transactionIdRow.isVisible = false
        
        // Update cancel info text
        val cancelInfo = getString(R.string.manage_subscription_cancel_info_no_date)
        binding.cancelInfoText.text = cancelInfo
    }
    
    private fun openContactSupport() {
        val email = getString(R.string.subscription_support_email)
        val intent = android.content.Intent(android.content.Intent.ACTION_SENDTO).apply {
            data = android.net.Uri.parse("mailto:")
            putExtra(android.content.Intent.EXTRA_EMAIL, arrayOf(email))
            putExtra(android.content.Intent.EXTRA_SUBJECT, "Zone News Pro Support")
        }
        try {
            startActivity(android.content.Intent.createChooser(intent, getString(R.string.manage_subscription_support_contact)))
        } catch (e: android.content.ActivityNotFoundException) {
            ToastUtils.showShortToast(requireContext(), "No email app found")
        }
    }
    
    private fun showRedeemCodeDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_redeem_code, null)
        val codeInput = dialogView.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.codeInput)
        val redeemButton = dialogView.findViewById<com.google.android.material.button.MaterialButton>(R.id.redeemButton)
        val cancelButton = dialogView.findViewById<com.google.android.material.button.MaterialButton>(R.id.btn_negative)
        
        val dialog = android.app.AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .setCancelable(true)
            .create()
        
        // Remove the default dialog background to show only our rounded card
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        
        // Add smooth window animations for a more polished appearance
        dialog.window?.let { window ->
            val params = window.attributes
            params.windowAnimations = R.style.DialogAnimation
            window.attributes = params
        }
        
        // Enable/disable redeem button based on input
        codeInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                redeemButton.isEnabled = !s.isNullOrBlank()
            }
        })
        
        // Set up button click listeners
        cancelButton.setOnClickListener {
            dialog.dismiss()
        }
        
        redeemButton.setOnClickListener {
            val code = codeInput.text.toString().trim()
            if (code.isNotEmpty()) {
                isRedeemCodeOperation = true // Mark that we're starting a redeem code operation
                myModel.redeemCode(code)
                dialog.dismiss()
            }
        }
        
        // Request focus on the input field when dialog shows
        dialog.setOnShowListener {
            codeInput.requestFocus()
            // Show keyboard
            val imm = requireContext().getSystemService(android.content.Context.INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager
            imm.showSoftInput(codeInput, android.view.inputmethod.InputMethodManager.SHOW_IMPLICIT)
        }
        
        dialog.show()
    }
    
    private fun showCancelSubscriptionConfirmation() {
        // For Android, we'll show a message that subscription will be cancelled through the backend
        // (Unlike iOS which redirects to Apple ID settings)
        SystemDialogUtils.showAlertDialog(
            requireContext(),
            getString(R.string.manage_subscription_cancel_button),
            getString(R.string.manage_subscription_cancel_confirmation_message),
            getString(R.string.manage_subscription_cancel_confirmation_confirm),
            getString(R.string.dialog_button_cancel),
            isDestructive = true,
            onPositiveClick = {
                myModel.cancelSubscription()
            }
        )
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
