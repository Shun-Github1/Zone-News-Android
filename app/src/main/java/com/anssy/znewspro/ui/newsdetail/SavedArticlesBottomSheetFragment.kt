package com.anssy.znewspro.ui.newsdetail

import android.content.Intent
import android.os.Bundle
import android.text.Html
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.anssy.znewspro.R
import com.anssy.znewspro.databinding.FragmentSavedArticlesBottomSheetBinding
import com.anssy.znewspro.entry.ViewHisEntry
import com.anssy.znewspro.model.MyModel
import com.anssy.znewspro.ui.newsdetail.NewsDetailActivity
import com.anssy.znewspro.utils.Constants
import com.anssy.znewspro.utils.ToastUtils
import com.anssy.znewspro.utils.glide.GlideApp
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.zhy.adapter.recyclerview.CommonAdapter
import com.zhy.adapter.recyclerview.base.ViewHolder
import java.text.SimpleDateFormat
import java.util.*

class SavedArticlesBottomSheetFragment : BottomSheetDialogFragment() {
    
    private var _binding: FragmentSavedArticlesBottomSheetBinding? = null
    private val binding get() = _binding!!
    
    private lateinit var myModel: MyModel
    private var articlesList = ArrayList<ViewHisEntry.DataDTO.ArticlesDTO>()
    private lateinit var articlesAdapter: CommonAdapter<ViewHisEntry.DataDTO.ArticlesDTO>
    
    companion object {
        fun newInstance(): SavedArticlesBottomSheetFragment {
            return SavedArticlesBottomSheetFragment()
        }
    }
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSavedArticlesBottomSheetBinding.inflate(inflater, container, false)
        return binding.root
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupViewModel()
        setupViews()
        loadData()
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
        
        // Observe saved articles data
        myModel.myCollectEntry.observe(viewLifecycleOwner) { response ->
            if (response != null) {
                if (response.code == Constants.SUCCESS_CODE) {
                    articlesList.clear()
                    articlesList.addAll(response.data.articles)
                    articlesAdapter.notifyDataSetChanged()
                    updateUI()
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
    
    private fun setupViews() {
        // Setup close button
        binding.closeButton.setOnClickListener {
            dismiss()
        }
        
        // Setup clear all button
        binding.clearAllButton.setOnClickListener {
            clearAllItems()
        }
        
        // Setup recycler views
        setupRecyclerViews()
    }
    
    private fun setupRecyclerViews() {
        // Single adapter for articles
        articlesAdapter = object : CommonAdapter<ViewHisEntry.DataDTO.ArticlesDTO>(
            requireContext(),
            R.layout.item_reading_history_news,
            articlesList
        ) {
            override fun convert(holder: ViewHolder, article: ViewHisEntry.DataDTO.ArticlesDTO, position: Int) {
                val dateTimeText = holder.getView<android.widget.TextView>(R.id.dateTimeText)
                val newsTitleText = holder.getView<android.widget.TextView>(R.id.newsTitleText)
                val newsImage = holder.getView<android.widget.ImageView>(R.id.newsImage)
                val chevronButton = holder.getView<android.widget.ImageView>(R.id.chevronButton)
                
                // Format date and time with HTML formatting
                dateTimeText.text = Html.fromHtml(formatDateTime(article.date), Html.FROM_HTML_MODE_COMPACT)
                
                // Set title
                newsTitleText.text = article.title
                
                // Load image
                GlideApp.with(requireContext())
                    .load(article.pictureURL)
                    .error(R.drawable.ease_default_image)
                    .into(newsImage)
                
                // Set click listeners
                holder.convertView.setOnClickListener {
                    openNewsDetail(article.articleID)
                }
                
                chevronButton.setOnClickListener {
                    openNewsDetail(article.articleID)
                }
                
                // Add long press listener for sharing with shrink animation
                holder.convertView.setOnLongClickListener {
                    // Animate shrink effect
                    holder.convertView.animate()
                        .scaleX(0.95f)
                        .scaleY(0.95f)
                        .setDuration(150)
                        .withEndAction {
                            // Restore original size after animation
                            holder.convertView.animate()
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
        
        // Set layout manager
        binding.newsRecyclerView.layoutManager = LinearLayoutManager(requireContext())
    }
    
    private fun loadData() {
        showLoading()
        myModel.queryMyCollect()
    }
    
    private fun updateUI() {
        val hasItems = articlesList.isNotEmpty()
        
        // Update clear all button visibility
        binding.clearAllButton.isVisible = hasItems
        
        // Update content visibility
        if (hasItems) {
            binding.loadingView.isVisible = false
            binding.emptyView.isVisible = false
            binding.newsRecyclerView.isVisible = true
            
            // Set adapter
            binding.newsRecyclerView.adapter = articlesAdapter
        } else {
            binding.loadingView.isVisible = false
            binding.newsRecyclerView.isVisible = false
            binding.emptyView.isVisible = true
        }
    }
    
    private fun showLoading() {
        binding.loadingView.isVisible = true
        binding.emptyView.isVisible = false
        binding.newsRecyclerView.isVisible = false
    }
    
    private fun clearAllItems() {
        // Clear all items
        articlesList.clear()
        articlesAdapter.notifyDataSetChanged()
        updateUI()
        
        ToastUtils.showShortToast(requireContext(), getString(R.string.cleared_all_items))
    }
    
    private fun openNewsDetail(articleId: String) {
        val intent = Intent(requireContext(), NewsDetailActivity::class.java)
        intent.putExtra("id", articleId)
        startActivity(intent)
        dismiss()
    }
    
    private fun formatDateTime(backendDate: String): String {
        return try {
            val formatter = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
            val date = formatter.parse(backendDate)
            
            if (date != null) {
                val dateFormatter = SimpleDateFormat("MMMM d", Locale.US)
                val monthDay = dateFormatter.format(date)
                
                // Add ordinal suffix
                val day = Calendar.getInstance().apply { time = date }.get(Calendar.DAY_OF_MONTH)
                val ordinalSuffix = when (day) {
                    1, 21, 31 -> "st"
                    2, 22 -> "nd"
                    3, 23 -> "rd"
                    else -> "th"
                }
                
                // Format year
                val yearFormatter = SimpleDateFormat("yyyy", Locale.US)
                val year = yearFormatter.format(date)
                
                // Format time as 24-hour
                val timeFormatter = SimpleDateFormat("HH:mm", Locale.US)
                val time = timeFormatter.format(date)
                
                "<b>$monthDay$ordinalSuffix, $year</b> $time"
            } else {
                backendDate
            }
        } catch (e: Exception) {
            backendDate
        }
    }
    
    /**
     * Share article functionality
     */
    private fun shareArticle(article: ViewHisEntry.DataDTO.ArticlesDTO) {
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

