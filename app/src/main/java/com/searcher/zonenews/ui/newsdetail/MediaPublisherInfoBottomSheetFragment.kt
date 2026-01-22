package com.searcher.zonenews.ui.newsdetail

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.bumptech.glide.Glide
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.searcher.zonenews.R
import com.searcher.zonenews.databinding.FragmentMediaPublisherInfoBinding
import com.searcher.zonenews.entry.PublisherInfoEntry
import com.searcher.zonenews.model.NewsDetailModel
import com.searcher.zonenews.utils.ThemeManager
import com.searcher.zonenews.utils.Utils
import android.content.Intent
import android.text.Html
import android.net.Uri
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import dagger.hilt.android.AndroidEntryPoint
import com.facebook.shimmer.ShimmerFrameLayout

@AndroidEntryPoint
class MediaPublisherInfoBottomSheetFragment : BottomSheetDialogFragment() {

    private var _binding: FragmentMediaPublisherInfoBinding? = null
    private val binding get() = _binding!!
    private lateinit var newsDetailModel: NewsDetailModel
    private var publisherId: Int = -1
    private var initialName: String? = null
    private var initialIcon: String? = null
    private var initialBiasTag: String? = null
    private var isProUser: Boolean = false

    companion object {
        private const val ARG_PUBLISHER_ID = "arg_publisher_id"
        private const val ARG_NAME = "arg_name"
        private const val ARG_ICON = "arg_icon"
        private const val ARG_BIAS_TAG = "arg_bias_tag"
        private const val ARG_IS_PRO = "arg_is_pro"

        fun newInstance(
            publisherId: Int,
            name: String,
            iconUrl: String,
            biasTag: String?,
            isProUser: Boolean
        ): MediaPublisherInfoBottomSheetFragment {
            val fragment = MediaPublisherInfoBottomSheetFragment()
            val args = Bundle().apply {
                putInt(ARG_PUBLISHER_ID, publisherId)
                putString(ARG_NAME, name)
                putString(ARG_ICON, iconUrl)
                putString(ARG_BIAS_TAG, biasTag)
                putBoolean(ARG_IS_PRO, isProUser)
            }
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            publisherId = it.getInt(ARG_PUBLISHER_ID)
            initialName = it.getString(ARG_NAME)
            initialIcon = it.getString(ARG_ICON)
            initialBiasTag = it.getString(ARG_BIAS_TAG)
            isProUser = it.getBoolean(ARG_IS_PRO)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMediaPublisherInfoBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): android.app.Dialog {
        val dialog = super.onCreateDialog(savedInstanceState) as com.google.android.material.bottomsheet.BottomSheetDialog
        dialog.setOnShowListener {
            val bottomSheet = dialog.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)
            bottomSheet?.let { sheet ->
                val behavior = com.google.android.material.bottomsheet.BottomSheetBehavior.from(sheet)
                val displayMetrics = requireContext().resources.displayMetrics
                val screenHeight = displayMetrics.heightPixels
                
                // Allow dragging to specific maximized height (85% of screen)
                behavior.isFitToContents = false
                behavior.expandedOffset = (screenHeight * 0.15).toInt()
                
                // Set default state (60% of screen)
                behavior.halfExpandedRatio = 0.6f
                behavior.state = com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_HALF_EXPANDED
                
                behavior.skipCollapsed = true

                // Apply background to the bottom sheet container to ensure it expands correctly
                sheet.setBackgroundResource(R.drawable.bottom_sheet_background)

                // Force height to match parent to ensure background fills the expanded space
                sheet.layoutParams.height = ViewGroup.LayoutParams.MATCH_PARENT
            }
        }
        return dialog
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        binding.doneButton.setOnClickListener {
            dismiss()
        }
        
        // Use activity scoped ViewModel to share data/state if needed, or just standard fetching
        // Assuming NewsDetailActivity has the Hilt ViewModel we need
        // If Model is simpler, we can just get it from provider. 
        // Note: NewsDetailModel is Activity-scoped usually in this app structure? 
        // Let's safe-cast parent activity's ViewModel or create new if unrelated.
        // Given implementation in NewsDetailActivity uses `newsDetailModel` via delegation or injection, 
        // we should probably grab the Activity's ViewModel instance to share caching.
        newsDetailModel = ViewModelProvider(requireActivity())[NewsDetailModel::class.java]

        setupInitialViews()
        setupObservers()
        fetchData()
    }

    private fun setupInitialViews() {
        // Set basic info we already have
        binding.publisherNameTv.text = initialName
        
        Glide.with(this)
            .load(initialIcon)
            .placeholder(R.drawable.shape_circle_placeholder)
            .into(binding.publisherLogoIv)

        // Handle Dark Mode for Logo Background
        if (ThemeManager.isDarkModeActive(requireContext())) {
            binding.publisherLogoIv.setBackgroundResource(R.drawable.bg_white_circle)
             val padding = Utils.dip2px(2f, requireContext())
            binding.publisherLogoIv.setPadding(padding, padding, padding, padding)
        }

        // Setup Bias Tag
        val reportPatternsEnabled = com.searcher.zonenews.utils.SharedPreferenceUtils.getBoolean(requireContext(), "report_patterns_enabled")
        
        if (!initialBiasTag.isNullOrEmpty() && (reportPatternsEnabled || !isProUser)) {
             binding.biasTagTv.visibility = View.VISIBLE
             
             if (!isProUser) {
                 // Free User: Show Blurred Preview
                 binding.biasTagTv.text = getString(R.string.preview)
                 binding.biasTagTv.setTextColor(androidx.core.content.ContextCompat.getColor(requireContext(), R.color.publisher_bias_progressive_text))
                 binding.biasTagTv.setBackgroundResource(R.drawable.publisher_bias_tag_progressive_background)
                 binding.biasTagTv.setLayerType(View.LAYER_TYPE_SOFTWARE, null)
                 binding.biasTagTv.paint.maskFilter = android.graphics.BlurMaskFilter(8f, android.graphics.BlurMaskFilter.Blur.NORMAL)
                 
                 binding.biasLockIcon.visibility = View.VISIBLE
                 
                 val showUpsell = View.OnClickListener { 
                     val subscriptionFragment = com.searcher.zonenews.ui.newsdetail.SubscriptionBottomSheetFragment.newInstance(isProUser)
                     subscriptionFragment.show(parentFragmentManager, "Subscription")
                 }
                 binding.biasTagTv.setOnClickListener(showUpsell)
                 binding.biasLockIcon.setOnClickListener(showUpsell)
             } else {
                 // Pro User: Show Actual Tag
                 val tag = initialBiasTag!!
                 binding.biasTagTv.text = getPublisherBiasText(tag)
                 binding.biasTagTv.setTextColor(getPublisherBiasTextColor(tag))
                 binding.biasTagTv.setBackgroundResource(getPublisherBiasBackground(tag))
                 binding.biasTagTv.setLayerType(View.LAYER_TYPE_HARDWARE, null)
                 binding.biasTagTv.paint.maskFilter = null
                 
                 binding.biasLockIcon.visibility = View.GONE
                 binding.biasTagTv.setOnClickListener(null)
                 binding.biasLockIcon.setOnClickListener(null)
             }
        } else {
             binding.biasTagTv.visibility = View.GONE
             binding.biasLockIcon.visibility = View.GONE
        }
        
        // Show loading state
        binding.shimmerViewContainer.visibility = View.VISIBLE
        binding.shimmerViewContainer.startShimmer()
        binding.contentLayout.visibility = View.GONE
    }

    /**
     * Get the localized text for publisher bias using tag-based system
     */
    private fun getPublisherBiasText(stanceTag: String): String {
        return when (stanceTag.trim().lowercase()) {
            "c" -> getString(R.string.conservative)
            "p" -> getString(R.string.liberal)
            // Legacy support for full names
            "conservative", "centric" -> getString(R.string.conservative)
            "progressive" -> getString(R.string.liberal)
            else -> stanceTag
        }
    }

    /**
     * Get the text color for publisher bias tag
     */
    private fun getPublisherBiasTextColor(stanceTag: String): Int {
        val context = requireContext()
        return when (stanceTag.trim().lowercase()) {
            "c" -> ContextCompat.getColor(context, R.color.publisher_bias_conservative_text)
            "p" -> ContextCompat.getColor(context, R.color.publisher_bias_progressive_text)
            // Legacy support for full names
            "conservative", "centric" -> ContextCompat.getColor(context, R.color.publisher_bias_conservative_text)
            "progressive" -> ContextCompat.getColor(context, R.color.publisher_bias_progressive_text)
            else -> ContextCompat.getColor(context, R.color.colorTextMiddle)
        }
    }

    /**
     * Get the background resource for publisher bias tag
     */
    private fun getPublisherBiasBackground(stanceTag: String): Int {
        return when (stanceTag.trim().lowercase()) {
            "c" -> R.drawable.publisher_bias_tag_background
            "p" -> R.drawable.publisher_bias_tag_progressive_background
            // Legacy support for full names
            "conservative", "centric" -> R.drawable.publisher_bias_tag_background
            "progressive" -> R.drawable.publisher_bias_tag_progressive_background
            else -> R.drawable.publisher_bias_tag_background
        }
    }

    private fun setupObservers() {
        newsDetailModel.publisherInfoEntry.observe(viewLifecycleOwner) { entry ->
            binding.shimmerViewContainer.stopShimmer()
            binding.shimmerViewContainer.visibility = View.GONE
            binding.contentLayout.visibility = View.VISIBLE

            if (entry != null && entry.code == 200 && entry.data != null) {
                bindData(entry.data)
            } else {
                // Handle error or empty state
                // Keep initial data visible at least
            }
        }
    }

    private fun bindData(data: PublisherInfoEntry.DataDTO) {
        // Update Name/Logo if richer data provided (usually same)
        binding.publisherNameTv.text = data.name

        // Banner/Intro
        if (!data.intro.isNullOrEmpty()) {
            binding.introTv.text = Html.fromHtml(data.intro, Html.FROM_HTML_MODE_LEGACY)
            binding.introTv.visibility = View.VISIBLE
        } else {
            binding.introTv.visibility = View.GONE
        }

        // Region
        if (!data.region.isNullOrEmpty()) {
            binding.regionTv.text = data.region
            // Simple emoji mapping or just show text. Implementation plan didn't specify emoji logic.
            // keeping simple text for now.
            binding.regionEmojiTv.visibility = View.GONE 
        } else {
             binding.regionTv.text = "-"
        }

        // Type
        binding.typeTv.text = data.type ?: "-"

        // Conglomerate
        binding.conglomerateTv.text = data.conglomerate ?: "-"

        // Controller
        binding.controllerTv.text = data.controller ?: "-"

        // Website
        if (!data.website.isNullOrEmpty()) {
            binding.websiteBtn.visibility = View.VISIBLE
            binding.websiteBtn.setOnClickListener {
                var url = data.website
                if (url.isNullOrEmpty()) return@setOnClickListener
                
                // Ensure URL has scheme
                val trimmed = url.trim()
                if (!trimmed.startsWith("http://") && !trimmed.startsWith("https://")) {
                    url = "https://$trimmed"
                } else {
                    url = trimmed
                }

                val articleOpeningMethod = com.searcher.zonenews.utils.SharedPreferenceUtils.getString(requireContext(), "article_opening_method")
                
                if (articleOpeningMethod == "external") {
                    try {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                        startActivity(intent)
                    } catch (e: Exception) {
                        e.printStackTrace()
                        // Use explicit string or resource if available. logic in NewsDetail uses R.string.open_in_browser which is 'Open in browser' or 'Error...'? 
                        // Checked strings.xml: open_in_browser -> "Open in browser" (or similar). NewsDetail uses it as error toast? 
                        // Actually in NewsDetail: ToastUtils.showShortToast(this, getString(R.string.open_in_browser))
                        // Let's use standard error toast or just generic error logic. 
                        // I will use a simple Toast.
                        android.widget.Toast.makeText(requireContext(), R.string.web_error_loading, android.widget.Toast.LENGTH_SHORT).show()
                    }
                } else {
                    // Default: Open in in-app browser (WebActivity)
                    val intent = Intent(requireContext(), com.searcher.zonenews.ui.web.WebActivity::class.java)
                    intent.putExtra(getString(R.string.url_key), url)
                    intent.putExtra(getString(R.string.type_key), getString(R.string.news_type))
                    intent.putExtra("publisherName", data.name)
                    intent.putExtra("publisherIcon", initialIcon)
                    startActivity(intent)
                }
            }
        } else {
            binding.websiteBtn.visibility = View.GONE
        }

        // Pro Feature Check
        if (!isProUser) {
             binding.paywallBlurOverlay.visibility = View.VISIBLE
             binding.biasLockIcon.visibility = View.VISIBLE
             
             // Setup Blur to cover entire screen (including top bar)
             val rootView = binding.root as ViewGroup
             binding.paywallBlurView.setupWith(rootView, eightbitlab.com.blurview.RenderScriptBlur(requireContext()))
                .setBlurRadius(20f)
                .setBlurAutoUpdate(true)
                
             binding.paywallBlurOverlay.setOnClickListener {
                 val subscriptionFragment = com.searcher.zonenews.ui.newsdetail.SubscriptionBottomSheetFragment.newInstance(isProUser)
                 subscriptionFragment.show(parentFragmentManager, "Subscription")
             }
             
             // Center lock icon relative to actual content
             binding.contentLayout.post {
                 val contentHeight = binding.contentLayout.height
                 val topBarHeight = (56 * resources.displayMetrics.density).toInt()
                 val iconHeight = binding.paywallLockIcon.height
                 
                 // Position icon at center of content (offset by top bar)
                 val targetY = topBarHeight + (contentHeight / 2f) - (iconHeight / 2f)
                 binding.paywallLockIcon.y = targetY
             }
        } else {
            binding.paywallBlurOverlay.visibility = View.GONE
            binding.biasLockIcon.visibility = View.GONE
        }
    }

    private fun fetchData() {
        if (publisherId != -1) {
            newsDetailModel.queryPublisherInfo(publisherId)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
