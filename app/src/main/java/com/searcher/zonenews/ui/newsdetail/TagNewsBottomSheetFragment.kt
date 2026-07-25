package com.searcher.zonenews.ui.newsdetail

import android.content.Intent
import android.os.Bundle
import android.text.Html
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.searcher.zonenews.R
import com.searcher.zonenews.databinding.FragmentTagNewsBottomSheetBinding
import com.searcher.zonenews.entry.HomeDataListEntry
import com.searcher.zonenews.model.HomeModel
import com.searcher.zonenews.model.TopicModel
import com.searcher.zonenews.utils.Constants
import com.searcher.zonenews.utils.ToastUtils
import com.searcher.zonenews.utils.Utils
import com.searcher.zonenews.utils.glide.GlideApp
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.zhy.adapter.recyclerview.CommonAdapter
import com.zhy.adapter.recyclerview.base.ViewHolder
import android.content.res.ColorStateList
import androidx.core.content.ContextCompat
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class TagNewsBottomSheetFragment : BottomSheetDialogFragment() {
    
    private var _binding: FragmentTagNewsBottomSheetBinding? = null
    private val binding get() = _binding!!
    
    private val homeModel: HomeModel by viewModels()
    private lateinit var topicModel: TopicModel
    private var articlesList = ArrayList<HomeDataListEntry.DataDTO.ArticlesDTO>()
    private lateinit var articlesAdapter: CommonAdapter<HomeDataListEntry.DataDTO.ArticlesDTO>
    
    private var tagName: String = ""
    private var tagApiId: String = ""
    private var isFollowing: Boolean = true
    private var sourceFragment: String = "your_feed"
    
    companion object {
        private const val ARG_TAG_NAME = "tag_name"
        private const val ARG_TAG_API_ID = "tag_api_id"
        private const val ARG_IS_FOLLOWING = "is_following"
        private const val ARG_SOURCE_FRAGMENT = "source_fragment"
        
        fun newInstance(tagName: String, tagApiId: String, isFollowing: Boolean = true, sourceFragment: String = "your_feed"): TagNewsBottomSheetFragment {
            return TagNewsBottomSheetFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_TAG_NAME, tagName)
                    putString(ARG_TAG_API_ID, tagApiId)
                    putBoolean(ARG_IS_FOLLOWING, isFollowing)
                    putString(ARG_SOURCE_FRAGMENT, sourceFragment)
                }
            }
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            tagName = it.getString(ARG_TAG_NAME, "")
            tagApiId = it.getString(ARG_TAG_API_ID, "")
            isFollowing = it.getBoolean(ARG_IS_FOLLOWING, true)
            sourceFragment = it.getString(ARG_SOURCE_FRAGMENT, "your_feed")
        }
    }
    
    override fun onCreateDialog(savedInstanceState: Bundle?): android.app.Dialog {
        val dialog = super.onCreateDialog(savedInstanceState)
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
        _binding = FragmentTagNewsBottomSheetBinding.inflate(inflater, container, false)
        return binding.root
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupViewModel()
        setupViews()
        
        // Initial check of following status based on current ViewModel state
        checkFollowingStatus()
        
        loadData()
    }
    

    


    private fun setupViewModel() {
        topicModel = ViewModelProvider(requireActivity())[TopicModel::class.java]
        
        // Observe home data (news by tag)
        homeModel.homeDataList.observe(viewLifecycleOwner) { response ->
            if (response != null) {
                if (response.code == Constants.SUCCESS_CODE) {
                    articlesList.clear()
                    response.data?.articles?.let { articles ->
                        articlesList.addAll(articles)
                    }
                    articlesAdapter.notifyDataSetChanged()
                    updateUI()
                } else {
                    if (response.code == 1000) {
                        ToastUtils.showShortToast(requireContext(), getString(R.string.server_error_message))
                    } else {
                        ToastUtils.showShortToast(requireContext(), response.msg)
                    }
                    updateUI()
                }
            }
        }
        
        // Observe my followed topics to ensure "reality" is reflected
        topicModel.myTopicsEntry.observe(viewLifecycleOwner) { response ->
            if (response != null && response.code == Constants.SUCCESS_CODE) {
                checkFollowingStatus()
            }
        }

        // Observe topic edit response to handle toggling
        topicModel.commonResponseEntry.observe(viewLifecycleOwner) { response ->
            if (response != null) {
                if (response.code == Constants.SUCCESS_CODE) {
                    // Update the status again after a successful server response
                    // The optimistic update already happened in toggleFollow, 
                    // but this provides a definitive sync.
                    checkFollowingStatus()
                } else {
                    if (response.code == 1000) {
                        ToastUtils.showShortToast(requireContext(), getString(R.string.server_error_message))
                    } else {
                        ToastUtils.showShortToast(requireContext(), response.msg)
                    }
                }
            }
        }
    }

    private fun checkFollowingStatus() {
        val myTopics = topicModel.myTopicsEntry.value?.data?.topics
        val currentlyFollowed = myTopics?.any { it.tag.equals(tagApiId, ignoreCase = true) } ?: false
        
        if (isFollowing != currentlyFollowed) {
            isFollowing = currentlyFollowed
            updateFollowButton()
        }
    }
    
    private fun setupViews() {
        // Set header title
        binding.headerTitle.text = tagName
        
        // Update follow button state
        updateFollowButton()
        
        // Close button
        binding.closeButton.setOnClickListener {
            dismiss()
        }
        
        // Follow button
        binding.followButton.setOnClickListener {
            toggleFollow()
        }
        
        // Setup RecyclerView
        setupRecyclerView()
    }
    
    private fun updateFollowButton() {
        if (isFollowing) {
            binding.followButton.text = getString(R.string.unfollow)
            val redColor = ContextCompat.getColor(requireContext(), R.color.colorRed)
            binding.followButton.setTextColor(redColor)
            binding.followButton.rippleColor = ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.ripple_red_soft))
        } else {
            binding.followButton.text = getString(R.string.follow)
            val brandColor = ContextCompat.getColor(requireContext(), R.color.main_color)
            binding.followButton.setTextColor(brandColor)
            binding.followButton.rippleColor = ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.ripple_brand_soft))
        }
    }
    
    private fun toggleFollow() {
        val action = if (isFollowing) {
            Constants.TYPE_TOPIC_DELETE
        } else {
            Constants.TYPE_TOPIC_ADD
        }
        
        // Optimistically update the shared ViewModel state so YourFeedFragment (the parent/background)
        // reflects the change in its "Active Tags" row immediately.
        topicModel.updateMyTopicsOptimistically(action, tagApiId, tagName)
        
        topicModel.editTopic(action, tagApiId)
    }
    
    private fun setupRecyclerView() {
        articlesAdapter = object : CommonAdapter<HomeDataListEntry.DataDTO.ArticlesDTO>(
            requireContext(),
            R.layout.item_tag_news,
            articlesList
        ) {
            override fun convert(holder: ViewHolder, article: HomeDataListEntry.DataDTO.ArticlesDTO, position: Int) {
                val dateTimeText = holder.getView<android.widget.TextView>(R.id.dateTimeText)
                val newsTitleText = holder.getView<android.widget.TextView>(R.id.newsTitleText)
                val newsImage = holder.getView<android.widget.ImageView>(R.id.newsImage)
                val cardContent = holder.getView<View>(R.id.cardContent)
                
                // Format date with HTML formatting
                dateTimeText.text = Html.fromHtml(Utils.formatBackendDateWithTime(article.date), Html.FROM_HTML_MODE_COMPACT)
                
                // Set title
                newsTitleText.text = article.title
                
                // Load image
                GlideApp.with(requireContext())
                    .load(article.pictureURL)
                    .error(R.drawable.ic_image_not_supported_24)
                    .into(newsImage)
                
                // Click to open news detail
                cardContent.setOnClickListener {
                    openNewsDetail(article.articleID)
                }
                
                // Long press to share
                cardContent.setOnLongClickListener {
                    cardContent.animate()
                        .scaleX(0.95f)
                        .scaleY(0.95f)
                        .setDuration(150)
                        .withEndAction {
                            cardContent.animate()
                                .scaleX(1.0f)
                                .scaleY(1.0f)
                                .setDuration(300)
                                .start()
                        }
                        .start()
                    
                    shareArticle(article)
                    true
                }
            }
        }
        
        binding.newsRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.newsRecyclerView.adapter = articlesAdapter
    }
    
    private fun loadData() {
        showLoading()
        
        // Ensure we have the latest followed topics to accurately show Follow/Unfollow status
        if (topicModel.myTopicsEntry.value?.data?.topics == null) {
            topicModel.queryMyTopics()
        }
        
        // Use HomeModel to fetch articles by topic tag (passes lowercase tag directly)
        homeModel.getDataByTopicTag(tagApiId, 1, 50)
    }
    
    private fun updateUI() {
        val hasItems = articlesList.isNotEmpty()
        
        binding.loadingView.stopShimmer()
        binding.loadingView.isVisible = false
        
        if (hasItems) {
            binding.emptyView.isVisible = false
            binding.newsRecyclerView.isVisible = true
        } else {
            binding.newsRecyclerView.isVisible = false
            binding.emptyView.isVisible = true
        }
    }
    
    private fun showLoading() {
        binding.loadingView.isVisible = true
        binding.loadingView.startShimmer()
        binding.emptyView.isVisible = false
        binding.newsRecyclerView.isVisible = false
    }
    
    private fun openNewsDetail(articleId: String) {
        val intent = Intent(requireContext(), NewsDetailActivity::class.java)
        intent.putExtra("id", articleId)
        intent.putExtra("source_fragment", sourceFragment)
        startActivity(intent)
    }
    
    private fun shareArticle(article: HomeDataListEntry.DataDTO.ArticlesDTO) {
        val shareText = buildString {
            append(article.title)
            if (!article.articleURL.isNullOrEmpty()) {
                append("\n\n")
                append(article.articleURL)
            }
        }
        
        val shareIntent = Intent(Intent.ACTION_SEND)
        shareIntent.type = "text/plain"
        shareIntent.putExtra(Intent.EXTRA_TEXT, shareText)
        startActivity(Intent.createChooser(shareIntent, getString(R.string.app_name)))
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
