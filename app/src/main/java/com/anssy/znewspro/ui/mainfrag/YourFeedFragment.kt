package com.anssy.znewspro.ui.mainfrag

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.SpannableString
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.anssy.znewspro.R
import com.anssy.znewspro.databinding.FragYourFeedBinding
import com.anssy.znewspro.entry.SearchListEntry
import com.anssy.znewspro.entry.TopicListEntry
import com.anssy.znewspro.model.PersonRecommendModel
import com.anssy.znewspro.model.TopicModel
import com.anssy.znewspro.selfview.NewNestedScrollView
import com.anssy.znewspro.selfview.popup.SortPopupWindow
import com.anssy.znewspro.ui.MainActivity
import com.anssy.znewspro.ui.newsdetail.NewsDetailActivity
import com.anssy.znewspro.ui.topicmodify.TopicSelectionActivity
import com.anssy.znewspro.utils.CalculateUtil
import com.anssy.znewspro.utils.Constants
import com.anssy.znewspro.utils.ToastUtils
import com.anssy.znewspro.utils.Utils
import com.bumptech.glide.Glide
import com.scwang.smartrefresh.layout.api.RefreshLayout
import com.scwang.smartrefresh.layout.listener.OnRefreshLoadMoreListener
import com.zhy.adapter.recyclerview.CommonAdapter
import com.zhy.adapter.recyclerview.base.ViewHolder
import java.text.SimpleDateFormat
import java.util.Locale

/**
 * Fragment displaying the "Your Feed" page with personalized news recommendations
 */
class YourFeedFragment : Fragment() {
    
    private var _binding: FragYourFeedBinding? = null
    private val binding get() = _binding!!
    
    private val personRecommendModel: PersonRecommendModel by activityViewModels()
    private val topicModel: TopicModel by activityViewModels()
    
    private var pageNo = 1
    private val pageSize = 10
    private var isRefresh = true
    
    private lateinit var mAdapter: CommonAdapter<SearchListEntry.DataDTO.ArticlesDTO>
    private var mNewsList = ArrayList<SearchListEntry.DataDTO.ArticlesDTO>()
    private var currentSort: SortPopupWindow.SortOption = SortPopupWindow.SortOption.LATEST
    private var allTopics = ArrayList<TopicListEntry.TopicDTO>()
    private var userSelectedTopics = ArrayList<TopicListEntry.TopicDTO>()
    
    // Animation timing
    private var refreshStartTime: Long = 0
    private val minimumRefreshDuration = 800L
    private var isButtonRefresh = false
    
    // Debounce timing
    private var lastRefreshTime: Long = 0
    private val minimumTimeBetweenRefreshes = 1500L
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragYourFeedBinding.inflate(inflater, container, false)
        return binding.root
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        setupSortAndTags()
        observeData()
        loadData()
    }
    
    private fun setupRecyclerView() {
        binding.homeRecycler.layoutManager = LinearLayoutManager(requireContext(), RecyclerView.VERTICAL, false)
        mAdapter = object : CommonAdapter<SearchListEntry.DataDTO.ArticlesDTO>(requireContext(), R.layout.item_recommended_news, mNewsList) {
            override fun convert(holder: ViewHolder, t: SearchListEntry.DataDTO.ArticlesDTO, position: Int) {
                val placeTv: TextView = holder.getView(R.id.place_tv)
                val tagTv: TextView = holder.getView(R.id.tag_tv)
                val titleTv: TextView = holder.getView(R.id.news_title_tv)
                val newsIv: ImageView = holder.getView(R.id.news_iv)
                val trackView: View = holder.getView(R.id.progress_track)
                val highlightView: View = holder.getView(R.id.progress_highlight)
                val timeTv: TextView = holder.getView(R.id.news_time_tv)
                val countTv: TextView = holder.getView(R.id.news_count_tv)
                val transScoreTv: TextView = holder.getView(R.id.trans_score_tv)
                val aiIcon: ImageView = holder.getView(R.id.ai_icon)
                val recommendedText: TextView = holder.getView(R.id.recommended_text)
                
                placeTv.text = t.region ?: ""
                tagTv.text = t.sector ?: ""
                
                aiIcon.visibility = View.VISIBLE
                recommendedText.visibility = View.VISIBLE
                
                titleTv.text = t.title
                countTv.text = getString(R.string.reports_count, t.nSources)
                
                val subjectivity = t.metrics?.subjectivity ?: 0.0
                val sentimentText = getString(CalculateUtil.getSentimentLabelResId(subjectivity))
                
                if (subjectivity > 0.1 || subjectivity < -0.1) {
                    val spannableString = SpannableString(sentimentText)
                    val colorResId = resources.getIdentifier(CalculateUtil.getSentimentColorName(subjectivity), "color", context?.packageName)
                    val sentimentColor = ContextCompat.getColor(requireContext(), colorResId)
                    spannableString.setSpan(ForegroundColorSpan(sentimentColor), 0, sentimentText.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                    transScoreTv.text = spannableString
                } else {
                    transScoreTv.text = sentimentText
                }
                
                Glide.with(requireContext())
                    .load(t.pictureURL)
                    .placeholder(R.drawable.ease_default_image)
                    .error(R.drawable.ease_default_image)
                    .into(newsIv)

                trackView.post {
                    val totalWidth = trackView.width
                    val half = totalWidth / 2
                    val score = subjectivity
                    val distance = (kotlin.math.abs(score) * half).toInt()
                    val lp = highlightView.layoutParams as ConstraintLayout.LayoutParams
                    if (distance <= 0) {
                        highlightView.visibility = View.INVISIBLE
                        lp.width = 1
                        lp.marginStart = half
                    } else {
                        highlightView.visibility = View.VISIBLE
                        lp.width = distance
                        lp.marginStart = if (score > 0) half else (half - distance)
                    }
                    highlightView.layoutParams = lp
                    highlightView.setBackgroundResource(
                        if (score > 0) R.drawable.bg_progress_positive else R.drawable.bg_progress_negative
                    )
                }

                try {
                    val dateFormat = SimpleDateFormat(getString(R.string.date_format_pattern), Locale.getDefault())
                    val parse = dateFormat.parse(t.date)
                    timeTv.text = Utils.getMultilingualSpaceTime(requireContext(), parse!!.time)
                } catch (e: Exception) {
                    e.printStackTrace()
                    timeTv.text = t.date
                }

                holder.itemView.setOnClickListener {
                    val intent = Intent(requireContext(), NewsDetailActivity::class.java)
                    intent.putExtra("id", t.articleID)
                    startActivity(intent)
                }
                
                holder.itemView.setOnLongClickListener {
                    holder.itemView.animate()
                        .scaleX(0.95f)
                        .scaleY(0.95f)
                        .setDuration(150)
                        .withEndAction {
                            holder.itemView.animate()
                                .scaleX(1.0f)
                                .scaleY(1.0f)
                                .setDuration(300)
                                .start()
                        }
                        .start()
                    
                    shareArticle(t)
                    true
                }
            }
        }
        binding.homeRecycler.adapter = mAdapter
        
        // Add RecyclerView scroll listener
        binding.homeRecycler.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                val activity = requireActivity() as? MainActivity
                if (dy > 0) {
                    activity?.hideBottomBar()
                } else if (dy < 0) {
                    activity?.showBottomBar()
                }
            }

            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                super.onScrollStateChanged(recyclerView, newState)
                val activity = requireActivity() as? MainActivity
                if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                    activity?.scheduleBottomBarAutoShow()
                } else {
                    activity?.cancelBottomBarAutoShow()
                }
            }
        })
        
        // Add NestedScrollView scroll listener for better scroll detection
        binding.nestedScrollView.addScrollChangeListener(object : NewNestedScrollView.AddScrollChangeListener {
            override fun onScrollChange(scrollX: Int, scrollY: Int, oldScrollX: Int, oldScrollY: Int) {
                val activity = requireActivity() as? MainActivity
                if (scrollY > oldScrollY) {
                    // Scrolling down
                    activity?.hideBottomBar()
                } else if (scrollY < oldScrollY) {
                    // Scrolling up
                    activity?.showBottomBar()
                }
            }
            
            override fun onScrollState(state: NewNestedScrollView.ScrollState?) {
                val activity = requireActivity() as? MainActivity
                when (state) {
                    NewNestedScrollView.ScrollState.IDLE -> {
                        // When scroll stops, schedule auto-show
                        activity?.scheduleBottomBarAutoShow()
                    }
                    NewNestedScrollView.ScrollState.DRAG, NewNestedScrollView.ScrollState.SCROLLING -> {
                        // While scrolling, cancel auto-show
                        activity?.cancelBottomBarAutoShow()
                    }
                    else -> {}
                }
            }
        })
        
        binding.smartRefresh.setOnRefreshLoadMoreListener(object : OnRefreshLoadMoreListener {
            override fun onRefresh(refreshLayout: RefreshLayout) {
                if (!isButtonRefresh) {
                    val currentTime = System.currentTimeMillis()
                    val timeSinceLastRefresh = currentTime - lastRefreshTime
                    if (timeSinceLastRefresh < minimumTimeBetweenRefreshes) {
                        refreshLayout.finishRefresh()
                        return
                    }
                    
                    lastRefreshTime = currentTime
                    refreshStartTime = System.currentTimeMillis()
                    isRefresh = true
                    pageNo = 1
                    personRecommendModel.queryRecommendList(pageNo, pageSize)
                }
            }

            override fun onLoadMore(refreshLayout: RefreshLayout) {
                isRefresh = false
                pageNo++
                personRecommendModel.queryRecommendList(pageNo, pageSize)
            }
        })
    }
    
    private fun setupSortAndTags() {
        updateSortIndicator()
        updateActiveTags()
        
        binding.sortChip.setOnClickListener { showSortPopup() }
    }
    
    @SuppressLint("NotifyDataSetChanged")
    private fun observeData() {
        topicModel.myTopicsEntry.observe(viewLifecycleOwner) { response ->
            if (response != null && response.code == Constants.SUCCESS_CODE) {
                userSelectedTopics.clear()
                response.data?.topics?.let { topics ->
                    userSelectedTopics.addAll(topics)
                }
                updateActiveTags()
            }
        }
        
        topicModel.topicListEntry.observe(viewLifecycleOwner) { response ->
            if (response != null && response.code == Constants.SUCCESS_CODE) {
                allTopics.clear()
                response.data?.topics?.let { topics ->
                    allTopics.addAll(topics)
                }
            }
        }
        
        topicModel.commonResponseEntry.observe(viewLifecycleOwner) { response ->
            if (response != null && response.code == Constants.SUCCESS_CODE) {
                updateActiveTags()
            }
        }
        
        personRecommendModel.recommendListEntry.observe(viewLifecycleOwner) {
            if (it != null) {
                if (it.code == Constants.SUCCESS_CODE) {
                    if (isRefresh) {
                        mNewsList.clear()
                        finishRefreshWithMinimumDuration(true)
                    } else {
                        binding.smartRefresh.finishLoadMore(true)
                    }
                    mNewsList.addAll(it.data.articles)
                    applyCurrentSort()
                    mAdapter.notifyDataSetChanged()
                } else {
                    if (isRefresh) {
                        finishRefreshWithMinimumDuration(false)
                    } else {
                        binding.smartRefresh.finishLoadMore(false)
                    }
                    if (it.code == 1000) {
                        ToastUtils.showShortToast(requireContext(), getString(R.string.server_error_message))
                    } else {
                        ToastUtils.showShortToast(requireContext(), it.msg)
                    }
                }
            }
        }
    }
    
    private fun loadData() {
        topicModel.queryMyTopics()
        topicModel.queryAllTopics()
        personRecommendModel.queryRecommendList(pageNo, pageSize)
    }
    
    private fun showSortPopup() {
        val popup = SortPopupWindow(requireContext(), object : SortPopupWindow.Callback {
            override fun onSortSelected(option: SortPopupWindow.SortOption) {
                currentSort = option
                updateSortIndicator()
                applyCurrentSort()
                mAdapter.notifyDataSetChanged()
            }
        })
        popup.setCurrentSort(currentSort)
        popup.showPopupWindow(binding.sortChip)
    }
    
    private fun updateSortIndicator() {
        val sortValue = when (currentSort) {
            SortPopupWindow.SortOption.LATEST -> getString(R.string.latest_sort_option)
            SortPopupWindow.SortOption.POPULAR -> getString(R.string.popular_sort_option)
            SortPopupWindow.SortOption.RELEVANT -> "Relevant"
        }
        binding.sortValueTv.text = sortValue
        
        val sortIcon = when (currentSort) {
            SortPopupWindow.SortOption.LATEST -> R.drawable.ic_clock_24
            SortPopupWindow.SortOption.POPULAR -> R.drawable.ic_trending_24
            SortPopupWindow.SortOption.RELEVANT -> R.drawable.ic_star_24
        }
        binding.sortInfoIcon.setImageResource(sortIcon)
    }
    
    private fun updateActiveTags() {
        binding.activeTagsChipsContainer.removeAllViews()
        
        val activeTags = userSelectedTopics
        
        if (activeTags.isEmpty()) {
            val noneText = TextView(requireContext())
            noneText.text = "None"
            noneText.setTextColor(requireContext().getColor(R.color.colorTextDeep))
            noneText.textSize = 16f
            noneText.typeface = requireContext().resources.getFont(R.font.inter_regular)
            noneText.setTypeface(noneText.typeface, android.graphics.Typeface.BOLD)
            
            binding.activeTagsChipsContainer.addView(noneText)
        } else {
            activeTags.forEach { tag ->
                val chipView = LayoutInflater.from(requireContext()).inflate(
                    R.layout.item_tag_chip,
                    binding.activeTagsChipsContainer,
                    false
                )
                val tagIcon = chipView.findViewById<ImageView>(R.id.tag_icon)
                val tagText = chipView.findViewById<TextView>(R.id.tag_text)
                
                tagText.text = tag.displayName
                tagIcon.setImageResource(getTagIcon(tag.tag))
                
                chipView.setOnClickListener {
                    removeActiveTag(tag.tag)
                }
                
                binding.activeTagsChipsContainer.addView(chipView)
            }
            
            val manageView = LayoutInflater.from(requireContext()).inflate(
                R.layout.item_tag_chip,
                binding.activeTagsChipsContainer,
                false
            )
            val manageIcon = manageView.findViewById<ImageView>(R.id.tag_icon)
            val manageText = manageView.findViewById<TextView>(R.id.tag_text)
            manageIcon.setImageResource(R.drawable.ic_chevron_right_24)
            manageText.visibility = View.GONE
            (manageIcon.layoutParams as? android.widget.LinearLayout.LayoutParams)?.let { lp ->
                lp.marginEnd = 0
                manageIcon.layoutParams = lp
            }
            val padV = Utils.dpToPx(4f, resources)
            val padH = Utils.dpToPx(8f, resources)
            manageView.setPadding(padH, padV, padH, padV)
            manageView.setOnClickListener { showTopicSelection() }
            binding.activeTagsChipsContainer.addView(manageView)
        }
    }
    
    private fun getTagIcon(tag: String): Int {
        return when (tag.lowercase()) {
            "conflict" -> R.drawable.ic_security_24
            "culture" -> R.drawable.ic_palette_24
            "diplomacy" -> R.drawable.ic_public_24
            "economics" -> R.drawable.ic_trending_up_24
            "entertainment" -> R.drawable.ic_live_tv_24
            "politics" -> R.drawable.ic_account_balance_24
            "science" -> R.drawable.ic_science_24
            "sports" -> R.drawable.ic_sports_soccer_24
            "technology" -> R.drawable.ic_memory_24
            else -> R.drawable.ic_security_24
        }
    }
    
    private fun removeActiveTag(tag: String) {
        topicModel.editTopic(Constants.TYPE_TOPIC_DELETE, tag)
        
        isRefresh = true
        pageNo = 1
        personRecommendModel.queryRecommendList(pageNo, pageSize)
    }
    
    private fun showTopicSelection() {
        val intent = Intent(requireContext(), TopicSelectionActivity::class.java)
        val selectedTopicTags = userSelectedTopics.map { it.tag }
        intent.putStringArrayListExtra(TopicSelectionActivity.EXTRA_SELECTED_TOPICS, ArrayList(selectedTopicTags))
        (parentFragment as? AdviceFrag)?.topicSelectionLauncher?.launch(intent)
    }
    
    private fun applyCurrentSort() {
        when (currentSort) {
            SortPopupWindow.SortOption.LATEST -> mNewsList.sortByDescending { it.date }
            SortPopupWindow.SortOption.POPULAR -> mNewsList.sortByDescending { it.nSources }
            SortPopupWindow.SortOption.RELEVANT -> {
                mNewsList.sortByDescending { article ->
                    val sentiment = article.metrics?.sentiment ?: 0.0
                    val subjectivity = article.metrics?.subjectivity ?: 0.0
                    kotlin.math.abs(sentiment) + kotlin.math.abs(subjectivity)
                }
            }
        }
    }
    
    private fun finishRefreshWithMinimumDuration(success: Boolean) {
        val elapsed = System.currentTimeMillis() - refreshStartTime
        val remainingTime = minimumRefreshDuration - elapsed
        
        if (remainingTime > 0) {
            Handler(Looper.getMainLooper()).postDelayed({
                binding.smartRefresh.finishRefresh(success)
                isButtonRefresh = false
            }, remainingTime)
        } else {
            binding.smartRefresh.finishRefresh(success)
            isButtonRefresh = false
        }
    }
    
    private fun shareArticle(article: SearchListEntry.DataDTO.ArticlesDTO) {
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
    
    fun refreshData() {
        if (!isAdded || isDetached || activity == null) {
            return
        }
        
        val currentTime = System.currentTimeMillis()
        val timeSinceLastRefresh = currentTime - lastRefreshTime
        if (timeSinceLastRefresh < minimumTimeBetweenRefreshes) {
            return
        }
        
        lastRefreshTime = currentTime
        refreshStartTime = System.currentTimeMillis()
        isButtonRefresh = true
        binding.smartRefresh.autoRefresh()
        
        binding.homeRecycler.post {
            binding.homeRecycler.scrollToPosition(0)
        }
        
        isRefresh = true
        pageNo = 1
        personRecommendModel.queryRecommendList(pageNo, pageSize)
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}


