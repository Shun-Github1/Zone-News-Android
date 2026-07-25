package com.searcher.zonenews.ui.newsdetail

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.lifecycle.ViewModelProvider
import com.searcher.zonenews.R
import com.searcher.zonenews.databinding.FragmentSubscriptionBottomSheetBinding
import com.searcher.zonenews.entry.CommonResponseEntry
import com.searcher.zonenews.model.MyModel
import com.searcher.zonenews.utils.Constants
import com.searcher.zonenews.utils.SystemDialogUtils
import com.searcher.zonenews.utils.ToastUtils
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import androidx.viewpager2.widget.ViewPager2
import android.widget.ImageView
import android.graphics.drawable.GradientDrawable
import androidx.core.content.ContextCompat
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import android.content.Intent
import android.content.SharedPreferences
import com.searcher.zonenews.ui.login.LoginActivity
import com.searcher.zonenews.utils.SharedPreferenceUtils

import com.facebook.login.LoginManager
import androidx.credentials.CredentialManager
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.exceptions.ClearCredentialException
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import com.google.firebase.auth.FirebaseAuth

import com.searcher.zonenews.billing.BillingManager
import com.android.billingclient.api.ProductDetails
import android.widget.RadioGroup
import android.widget.RadioButton
import android.widget.TextView

class SubscriptionBottomSheetFragment : BottomSheetDialogFragment() {
    
    private var _binding: FragmentSubscriptionBottomSheetBinding? = null
    private val binding get() = _binding!!
    
    private lateinit var myModel: MyModel
    private var isProUser = false
    private lateinit var screenshotsAdapter: ScreenshotsCarouselAdapter
    private var hasShownCancelMessage = false // Track if we've shown cancel message in this session
    private var isRedeemCodeOperation = false // Track if we're currently processing a redeem code operation
    private var isDeleteAccountOperation = false // Track if we're involved in delete account operation
    
    // Billing variables
    private var activeProductDetails: ProductDetails? = null
    private var monthlyOfferToken: String? = null
    private var yearlyOfferToken: String? = null
    private var isMonthlySelected = true
    private var monthlyPrice: String = ""
    private var yearlyPrice: String = ""
    private var monthlyPriceMicros: Long = 0
    private var yearlyPriceMicros: Long = 0
    private var monthlyTrialPeriod: String = ""
    private var yearlyTrialPeriod: String = ""

    
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
        
        // Observe redeem/cancel/delete response
        myModel.commonResponseEntry.observe(viewLifecycleOwner) { response ->
            if (response != null) {
                if (response.code == Constants.SUCCESS_CODE) {
                    
                    // distinct handling for delete account
                    if (isDeleteAccountOperation) {
                        isDeleteAccountOperation = false
                        ToastUtils.showShortToast(requireContext(), getString(R.string.delete_account_success))
                        performLogout()
                        dismiss()
                        return@observe
                    }

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
                    
                    val isDeleteError = isDeleteAccountOperation
                    
                    // Reset the flags after checking
                    isRedeemCodeOperation = false
                    isDeleteAccountOperation = false
                    
                    val errorMessage = when {
                        isDeleteError -> response.msg ?: getString(R.string.server_error_message)
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
        
        // Observe billing data
        myModel.productDetails.observe(viewLifecycleOwner) { detailsList ->
            if (detailsList.isNotEmpty()) {
                activeProductDetails = detailsList[0] // Assuming one product "pro_subscription"
                processSubscriptionOffers(activeProductDetails!!)
            }
        }
        
        myModel.purchaseState.observe(viewLifecycleOwner) { state ->
            when (state) {
                is BillingManager.PurchaseState.Loading -> {
                    // Show loading if needed
                    binding.subscribeButton.text = getString(R.string.loading)
                    binding.subscribeButton.isEnabled = false
                }
                is BillingManager.PurchaseState.PurchaseSuccess -> {
                    binding.subscribeButton.text = getString(R.string.subscription_subscribe_button)
                    binding.subscribeButton.isEnabled = true
                    ToastUtils.showShortToast(requireContext(), getString(R.string.subscription_redeem_success)) // Reuse string or add new "Subscribed"
                    myModel.queryMyFormation() // Refresh profile
                }
                is BillingManager.PurchaseState.Error -> {
                    binding.subscribeButton.text = getString(R.string.subscription_subscribe_button)
                    binding.subscribeButton.isEnabled = true
                    if (state.message.contains("Restore", true)) {
                         ToastUtils.showShortToast(requireContext(), state.message)
                    } else if (state.message.contains("Purchase failed", true)) {
                        // Don't show toast for user cancellation usually, but manager handles it.
                        // If specific error, show it
                        if (!state.message.contains("User cancelled")) {
                            ToastUtils.showShortToast(requireContext(), state.message)
                        }
                    } else {
                         ToastUtils.showShortToast(requireContext(), state.message)
                    }
                }
                is BillingManager.PurchaseState.Idle -> {
                    binding.subscribeButton.text = getString(R.string.subscription_subscribe_button)
                    binding.subscribeButton.isEnabled = true
                }
            }
        }
    }
    
    private fun processSubscriptionOffers(productDetails: ProductDetails) {
        val offerDetails = productDetails.subscriptionOfferDetails
        if (offerDetails.isNullOrEmpty()) return
        
        // Find monthly and yearly offers based on base plan ID
        for (offer in offerDetails) {
            val basePlanId = offer.basePlanId
            val pricingPhases = offer.pricingPhases.pricingPhaseList
            
            // Find the recurring phase (price > 0 for standard subscription, or recurrence mode is infinite)
            // We look for the phase that defines the actual subscription cost
            val recurringPhase = pricingPhases.find { it.priceAmountMicros > 0 } ?: pricingPhases.firstOrNull()
            
            // Check for free trial (price is 0)
            val trialPhase = pricingPhases.find { it.priceAmountMicros == 0L }
            
            if (basePlanId == BillingManager.BASE_PLAN_MONTHLY) {
                monthlyOfferToken = offer.offerToken
                monthlyPrice = recurringPhase?.formattedPrice ?: ""
                monthlyPriceMicros = recurringPhase?.priceAmountMicros ?: 0L
                monthlyTrialPeriod = if (trialPhase != null) parseIsoDuration(trialPhase.billingPeriod) else ""
            } else if (basePlanId == BillingManager.BASE_PLAN_YEARLY) {
                yearlyOfferToken = offer.offerToken
                yearlyPrice = recurringPhase?.formattedPrice ?: ""
                yearlyPriceMicros = recurringPhase?.priceAmountMicros ?: 0L
                yearlyTrialPeriod = if (trialPhase != null) parseIsoDuration(trialPhase.billingPeriod) else ""
            }
        }
        
        updatePriceDisplay()
    }
    
    private fun parseIsoDuration(isoDuration: String): String {
        return try {
            // Simple logic for P1W, P7D, P1M etc.
            when {
                isoDuration.contains("P1W") || isoDuration.contains("P7D") -> getString(R.string.trial_7_days)
                isoDuration.contains("P2W") || isoDuration.contains("P14D") -> getString(R.string.trial_14_days)
                isoDuration.contains("P1M") || isoDuration.contains("P30D") -> getString(R.string.trial_1_month)
                isoDuration.contains("P3D") -> getString(R.string.trial_3_days)
                else -> ""
            }
        } catch (e: Exception) {
            ""
        }
    }
    
    private fun updatePriceDisplay() {
        if (isMonthlySelected) {
            binding.planPriceText.text = if (monthlyPrice.isNotEmpty()) getString(R.string.subscription_price_period_format, monthlyPrice, getString(R.string.subscription_plan_monthly)) else getString(R.string.loading)
            
            // Show trial if available
            if (monthlyTrialPeriod.isNotEmpty()) {
                binding.planBillingText.text = getString(R.string.subscription_trial_billed_monthly, monthlyTrialPeriod)
            } else {
                binding.planBillingText.text = getString(R.string.subscription_plan_billing)
            }
            binding.savingsBadge.isVisible = false
            
            // Update button text for trial
            if (monthlyTrialPeriod.isNotEmpty() && !isProUser) {
                binding.subscribeButton.text = getString(R.string.subscription_start_trial)
            } else if (!isProUser) {
                 binding.subscribeButton.text = getString(R.string.subscription_subscribe_button)
            }
        } else {
            binding.planPriceText.text = if (yearlyPrice.isNotEmpty()) getString(R.string.subscription_price_period_format, yearlyPrice, getString(R.string.subscription_plan_yearly)) else getString(R.string.loading)
            
            // Show trial if available
            if (yearlyTrialPeriod.isNotEmpty()) {
                binding.planBillingText.text = getString(R.string.subscription_trial_billed_yearly, yearlyTrialPeriod)
            } else {
                binding.planBillingText.text = getString(R.string.subscription_plan_billing_yearly)
            }
            
            // Calculate savings
            if (monthlyPriceMicros > 0 && yearlyPriceMicros > 0) {
                binding.savingsBadge.isVisible = true
                val annualizedMonthly = monthlyPriceMicros * 12
                if (annualizedMonthly > yearlyPriceMicros) {
                    val savingsContent = annualizedMonthly - yearlyPriceMicros
                    val savingsPercent = (savingsContent.toDouble() / annualizedMonthly.toDouble() * 100).toInt()
                    binding.savingsBadge.text = String.format(getString(R.string.subscription_save_percent), savingsPercent)
                } else {
                     binding.savingsBadge.isVisible = false
                }
            } else {
                binding.savingsBadge.isVisible = false
            }
            
            // Update button text for trial
             if (yearlyTrialPeriod.isNotEmpty() && !isProUser) {
                binding.subscribeButton.text = getString(R.string.subscription_start_trial)
            } else if (!isProUser) {
                 binding.subscribeButton.text = getString(R.string.subscription_subscribe_button)
            }
        }
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
                layoutParams = ViewGroup.MarginLayoutParams(dotSize, dotSize).apply {
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
        
        // Plan Selection
        binding.planSelectionGroup.setOnCheckedChangeListener { _, checkedId ->
            isMonthlySelected = checkedId == R.id.radioMonthly
            updatePriceDisplay()
        }
        
        // Subscribe button (shown if not Pro)
        binding.subscribeButton.setOnClickListener {
             if (activeProductDetails != null) {
                val offerToken = if (isMonthlySelected) monthlyOfferToken else yearlyOfferToken
                if (offerToken != null) {
                    myModel.launchPurchaseFlow(requireActivity(), activeProductDetails!!, offerToken)
                } else {
                    ToastUtils.showShortToast(requireContext(), getString(R.string.server_error_message))
                }
            } else {
                ToastUtils.showShortToast(requireContext(), getString(R.string.topics_loading))
            }
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
            val restoreMsg = getString(R.string.subscription_restore_loading)
            ToastUtils.showShortToast(requireContext(), restoreMsg)
            myModel.restorePurchases()
        }
        
        // Restore purchases button (manage page)
        binding.restorePurchasesLayoutManage.setOnClickListener {
            val restoreMsg = getString(R.string.subscription_restore_loading)
            ToastUtils.showShortToast(requireContext(), restoreMsg)
            myModel.restorePurchases()
        }
        
        // Contact support button (manage page)
        binding.contactSupportLayout.setOnClickListener {
            openContactSupport()
        }

        // Delete Account button (upgrade page)
        binding.deleteAccountLayout.setOnClickListener {
            showDeleteAccountConfirmation()
        }

        // Delete Account button (manage page)
        binding.deleteAccountLayoutManage.setOnClickListener {
            showDeleteAccountConfirmation()
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
        val intent = Intent(Intent.ACTION_SENDTO).apply {
            data = android.net.Uri.parse("mailto:")
            putExtra(Intent.EXTRA_EMAIL, arrayOf(email))
            putExtra(Intent.EXTRA_SUBJECT, getString(R.string.subscription_support_email_subject))
        }
        try {
            startActivity(Intent.createChooser(intent, getString(R.string.manage_subscription_support_contact)))
        } catch (e: android.content.ActivityNotFoundException) {
            ToastUtils.showShortToast(requireContext(), getString(R.string.no_email_app_found))
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
        SystemDialogUtils.showAlertDialog(
            requireContext(),
            getString(R.string.manage_subscription_cancel_button),
            getString(R.string.manage_subscription_cancel_confirmation_message) + "\n\n" + getString(R.string.manage_subscription_cancel_info_no_date),
            "Go to Play Store",
            getString(R.string.dialog_button_cancel),
            isDestructive = true,
            onPositiveClick = {
                try {
                    val intent = Intent(Intent.ACTION_VIEW, android.net.Uri.parse("https://play.google.com/store/account/subscriptions?package=${requireContext().packageName}&sku=${BillingManager.PRODUCT_ID_PRO}"))
                    startActivity(intent)
                } catch (e: Exception) {
                    ToastUtils.showShortToast(requireContext(), getString(R.string.topics_error_loading))
                }
            }
        )
    }

    private fun showDeleteAccountConfirmation() {
        SystemDialogUtils.showAlertDialog(
            requireContext(),
            getString(R.string.delete_account_confirmation_title),
            getString(R.string.delete_account_confirmation_message),
            getString(R.string.delete_account_confirm),
            getString(R.string.dialog_button_cancel),
            isDestructive = true,
            onPositiveClick = {
                performDeleteAccount()
            }
        )
    }

    private fun performDeleteAccount() {
        // Check for Firebase Auth first
        val user = FirebaseAuth.getInstance().currentUser
        if (user != null) {
            user.getIdToken(true).addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val idToken = task.result?.token
                    if (idToken != null) {
                        val mediaType = "application/json; charset=utf-8".toMediaType()
                        val requestBody = "{\"idToken\":\"$idToken\"}".toRequestBody(mediaType)
                        isDeleteAccountOperation = true
                        myModel.deleteAccount(requestBody)
                    } else {
                        ToastUtils.showShortToast(requireContext(), getString(R.string.server_error_message))
                    }
                } else {
                    ToastUtils.showShortToast(requireContext(), getString(R.string.server_error_message))
                }
            }
        } else {
            // Fallback for manual account - prompt for password
            showPasswordDialog()
        }
    }

    private fun showPasswordDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_password_input, null)
        val passwordInput = dialogView.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.passwordInput)
        val deleteButton = dialogView.findViewById<com.google.android.material.button.MaterialButton>(R.id.deleteButton)
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
        
        // Enable/disable delete button based on input
        passwordInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                deleteButton.isEnabled = !s.isNullOrBlank()
            }
        })
        
        // Set up button click listeners
        cancelButton.setOnClickListener {
            dialog.dismiss()
        }
        
        deleteButton.setOnClickListener {
            val password = passwordInput.text.toString().trim()
            if (password.isNotEmpty()) {
                val mediaType = "application/json; charset=utf-8".toMediaType()
                val requestBody = "{\"password\":\"$password\"}".toRequestBody(mediaType)
                isDeleteAccountOperation = true
                myModel.deleteAccount(requestBody)
                dialog.dismiss()
            }
        }
        
        // Request focus on the input field when dialog shows
        dialog.setOnShowListener {
            passwordInput.requestFocus()
            // Show keyboard
            val imm = requireContext().getSystemService(android.content.Context.INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager
            imm.showSoftInput(passwordInput, android.view.inputmethod.InputMethodManager.SHOW_IMPLICIT)
        }
        
        dialog.show()
    }

    private fun performLogout() {
        // Check if third-party login BEFORE clearing SharedPreferences
        val isThirdPartyLogin = SharedPreferenceUtils.getBoolean(requireContext(), "thirdLogin")
        
        // Sign out from all third-party providers
        if (isThirdPartyLogin) {
            signOut()
        }
        
        // Clear only user authentication data
        SharedPreferenceUtils.deleteString(requireContext(), "token")
        SharedPreferenceUtils.deleteString(requireContext(), "autoLogin")
        SharedPreferenceUtils.saveBoolean(requireContext(), "isLogin", false)
        SharedPreferenceUtils.saveBoolean(requireContext(), "thirdLogin", false)
        
        // Clear cookie preferences explicitly
        val cookiePrefs: SharedPreferences = requireContext().getSharedPreferences("cookie_prefs", android.content.Context.MODE_PRIVATE)
        cookiePrefs.edit().clear().apply()
        
        // Navigate to login page
        val intent = Intent(requireContext(), LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        requireActivity().finishAffinity()
    }

    private fun signOut() {
        // Firebase sign out
        FirebaseAuth.getInstance().signOut()
        
        // Google Sign-In sign out
        try {
            @Suppress("DEPRECATION")
            val gso = com.google.android.gms.auth.api.signin.GoogleSignInOptions.Builder(com.google.android.gms.auth.api.signin.GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build()
            @Suppress("DEPRECATION")
            val googleSignInClient = com.google.android.gms.auth.api.signin.GoogleSignIn.getClient(requireContext(), gso)
            googleSignInClient.signOut()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        
        // Facebook LoginManager sign out
        try {
            LoginManager.getInstance().logOut()
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // Clear credential manager state
        val credentialManager = CredentialManager.create(requireContext())
        lifecycleScope.launch {
            try {
                val clearRequest = ClearCredentialStateRequest()
                credentialManager.clearCredentialState(clearRequest)
            } catch (e: ClearCredentialException) {
                e.printStackTrace()
            }
        }
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
