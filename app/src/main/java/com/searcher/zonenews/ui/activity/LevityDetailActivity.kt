package com.searcher.zonenews.ui.activity

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.searcher.zonenews.R
import com.searcher.zonenews.utils.network.exception.NetworkResponse
import androidx.activity.viewModels
import com.searcher.zonenews.model.NewsDetailModel
import dagger.hilt.android.AndroidEntryPoint
import com.searcher.zonenews.entry.ArticleDetailEntry
import com.searcher.zonenews.widget.CatMascotView
import kotlinx.coroutines.launch

import com.searcher.zonenews.base.BaseActivity

@AndroidEntryPoint
class LevityDetailActivity : BaseActivity() {

    private val viewModel: NewsDetailModel by viewModels()
    private val myModel: com.searcher.zonenews.model.MyModel by viewModels() // Add MyModel for saved articles
    private lateinit var titleTv: TextView
    private lateinit var newsIv: ImageView
    
    private lateinit var summaryMascot: CatMascotView
    private lateinit var implicationsMascot: CatMascotView
    private lateinit var publisherArticlesMascot: CatMascotView
    
    private lateinit var publisherArticlesList: RecyclerView
    private var publisherArticlesAdapter: com.zhy.adapter.recyclerview.CommonAdapter<ArticleDetailEntry.DataDTO.ArticlesDTO>? = null
    
    // Saved article state
    private var savedArticleIds: Set<String> = emptySet()
    private var isArticleSaved: Boolean = false
    private var isProUser: Boolean = false
    private var mArticleDetailEntry: ArticleDetailEntry.DataDTO? = null
    
    private lateinit var synopsisHeader: TextView
    private lateinit var synopsisContent: TextView
    private lateinit var implicationsHeader: TextView
    private lateinit var implicationsContent: TextView
    
    private lateinit var shimmerView: com.facebook.shimmer.ShimmerFrameLayout
    private lateinit var contentScrollView: androidx.core.widget.NestedScrollView
    
    private lateinit var errorContainer: LinearLayout
    private lateinit var errorMascot: CatMascotView
    private lateinit var retryBtn: Button
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        delegate.localNightMode = AppCompatDelegate.MODE_NIGHT_NO // Force Light locally
        applyStatusBarStyle()
        setContentView(R.layout.activity_levity_detail)

        val articleId = intent.getStringExtra("id") ?: return finish()
        
        titleTv = findViewById(R.id.title_tv)
        newsIv = findViewById(R.id.news_iv)
        
        summaryMascot = findViewById(R.id.summary_mascot)
        implicationsMascot = findViewById(R.id.implications_mascot)
        publisherArticlesMascot = findViewById(R.id.publisher_articles_mascot)
        
        publisherArticlesList = findViewById(R.id.levity_publisher_articles_list)
        publisherArticlesList.layoutManager = LinearLayoutManager(this)
        
        synopsisHeader = findViewById(R.id.synopsis_header)
        synopsisContent = findViewById(R.id.synopsis_content)
        implicationsHeader = findViewById(R.id.implications_header)
        implicationsContent = findViewById(R.id.implications_content)
        
        shimmerView = findViewById(R.id.shimmer_layout)
        contentScrollView = findViewById(R.id.content_scroll_view)
        
        errorContainer = findViewById(R.id.error_state_container)
        errorMascot = findViewById(R.id.error_mascot)
        retryBtn = findViewById(R.id.error_retry_btn)
        
        retryBtn.setOnClickListener {
            loadArticle(articleId)
        }
        
        shimmerView.startShimmer()
        
        // Setup Toolbar
        val toolbar = findViewById<androidx.appcompat.widget.Toolbar>(R.id.toolbar)
        toolbar.setNavigationOnClickListener { finish() }
        toolbar.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.action_share -> {
                    val link = "https://zonenews.io/article/$articleId"
                    shareLink(link)
                    true
                }
                R.id.action_save -> {
                    // Use local isArticleSaved state which is determined from saved articles list
                    if (isArticleSaved) {
                        viewModel.deleteCollect(articleId)
                    } else {
                        viewModel.collectHis(articleId)
                    }
                    true
                }
                else -> false
            }
        }
        
        // Initialize menu state
        initSaveButtonAsUnsaved()
        
        loadArticle(articleId)
        loadArticle(articleId)
        observeViewModel()
    }
    
    override fun onResume() {
        super.onResume()
        // Ensure Pro status is fresh every time we return to this screen
        myModel.queryMyFormation()
    }
    
    private fun loadArticle(id: String) {
        showLoadingState()
        viewModel.queryNewsDetail(id)
    }
    
    
    
    private fun observeViewModel() {
        viewModel.newsDetailEntry.observe(this) { article ->
            if (article != null && article.code == 200 && article.data != null) {
                mArticleDetailEntry = article.data // specific for LevityDetailActivity to use in other places
                bindData(article.data!!)
            } else {
                showErrorState()
            }
        }
        
        // Saved articles state observation
        myModel.queryMyCollect()
        myModel.myCollectEntry.observe(this) { response ->
            if (response != null && response.code == com.searcher.zonenews.utils.Constants.SUCCESS_CODE && response.data?.articles != null) {
                savedArticleIds = response.data.articles.mapNotNull { it.articleID }.toSet()
                val articleId = intent.getStringExtra("id")
                if (articleId != null) {
                    isArticleSaved = savedArticleIds.contains(articleId)
                    updateSaveButtonState(isArticleSaved)
                }
            }
        }
        
        // Collect/Uncollect responses
        viewModel.collectEntry.observe(this) {
             if (it!=null){
                if (it.code== com.searcher.zonenews.utils.Constants.SUCCESS_CODE){
                    com.searcher.zonenews.utils.ToastUtils.showShortToast(this, getString(R.string.collect_success_toast))
                    isArticleSaved = true
                    updateSaveButtonState(true)
                } else {
                     if (it.code==1000){
                        com.searcher.zonenews.utils.SystemDialogUtils.showErrorMessage(this, getString(R.string.server_error_message))
                    } else {
                        com.searcher.zonenews.utils.SystemDialogUtils.showErrorMessage(this, it.msg)
                    }
                }
            }
        }
        
        viewModel.deleteCollectEntry.observe(this) {
            if (it!=null){
                if (it.code== com.searcher.zonenews.utils.Constants.SUCCESS_CODE){
                    com.searcher.zonenews.utils.ToastUtils.showShortToast(this, getString(R.string.uncollect_success_toast))
                    isArticleSaved = false
                    updateSaveButtonState(false)
                } else {
                     if (it.code==1000){
                        com.searcher.zonenews.utils.SystemDialogUtils.showErrorMessage(this, getString(R.string.server_error_message))
                    } else {
                        com.searcher.zonenews.utils.SystemDialogUtils.showErrorMessage(this, it.msg)
                    }
                }
            }
        }


        
        // Observe Pro status
        myModel.myEntry.observe(this) { response ->
            if (response != null && response.code == com.searcher.zonenews.utils.Constants.SUCCESS_CODE) {
                isProUser = response.data?.isPro == true
                // If specific UI logic for Pro/Free users is needed in Levity, apply it here.
                // For now, we are ensuring the status is fresh and available in the variable.
            }
        }
    }
    
    private fun bindData(article: ArticleDetailEntry.DataDTO) {
        // Stop shimmer and show content
        shimmerView.stopShimmer()
        shimmerView.visibility = View.GONE
        errorContainer.visibility = View.GONE
        contentScrollView.visibility = View.VISIBLE

        titleTv.text = article.title
        
        // Simplified content (Synopsis + Implications)
        // Simplified content (Synopsis + Implications)
        val desc = article.description
        if (desc != null) {
            if (!desc.synopsis.isNullOrEmpty()) {
                 synopsisHeader.visibility = View.VISIBLE
                 synopsisContent.visibility = View.VISIBLE
                 synopsisContent.text = desc.synopsis
            } else {
                 synopsisHeader.visibility = View.GONE
                 synopsisContent.visibility = View.GONE
            }
            
            if (!desc.implications.isNullOrEmpty()) {
                 implicationsHeader.visibility = View.VISIBLE
                 implicationsContent.visibility = View.VISIBLE
                 implicationsContent.text = desc.implications
            } else {
                 implicationsHeader.visibility = View.GONE
                 implicationsContent.visibility = View.GONE
            }
        } else {
             // Fallback if no description
             synopsisHeader.visibility = View.GONE
             synopsisContent.visibility = View.GONE
             implicationsHeader.visibility = View.GONE
             implicationsContent.visibility = View.GONE
        }
        
        Glide.with(this)
            .load(article.pictureURL)
            .placeholder(R.drawable.ic_image_not_supported_24)
            .error(R.drawable.ic_image_not_supported_24)
            .into(newsIv)
        
        // Set mascot sentiment (uplifting positive sentiment)
        val sentiment = article.metrics?.sentiment ?: 0.35
        summaryMascot.setSentiment(sentiment)
        implicationsMascot.setSentiment(sentiment)
        publisherArticlesMascot.setSentiment(sentiment)
        
        // Setup publisher articles
        setupPublisherArticles(article)
        

    }

    private fun showLoadingState() {
        shimmerView.visibility = View.VISIBLE
        shimmerView.startShimmer()
        contentScrollView.visibility = View.GONE
        errorContainer.visibility = View.GONE
    }

    private fun showErrorState() {
        shimmerView.stopShimmer()
        shimmerView.visibility = View.GONE
        contentScrollView.visibility = View.GONE
        errorContainer.visibility = View.VISIBLE
        errorMascot.setSentiment(0.35)
    }
    
    private fun setupPublisherArticles(data: ArticleDetailEntry.DataDTO) {
        val articles = data.articles ?: emptyList()
        
        if (articles.isEmpty()) {
            findViewById<View>(R.id.publisher_articles_card)?.visibility = View.GONE
            return
        }
        
        findViewById<View>(R.id.publisher_articles_card)?.visibility = View.VISIBLE
        
        publisherArticlesAdapter = object : com.zhy.adapter.recyclerview.CommonAdapter<ArticleDetailEntry.DataDTO.ArticlesDTO>(
            this, R.layout.item_levity_publisher_article, articles
        ) {
            override fun convert(holder: com.zhy.adapter.recyclerview.base.ViewHolder, article: ArticleDetailEntry.DataDTO.ArticlesDTO, position: Int) {
                val iconIv = holder.getView<ImageView>(R.id.article_publisher_icon)
                val nameTv = holder.getView<TextView>(R.id.article_publisher_name)
                val titleTv = holder.getView<TextView>(R.id.article_title)
                val divider = holder.getView<View>(R.id.divider)
                
                Glide.with(this@LevityDetailActivity)
                    .load(article.publisherIcon)
                    .error(R.drawable.ic_image_not_supported_24)
                    .into(iconIv)
                    
                nameTv.text = article.publisherName ?: getString(R.string.about)
                titleTv.text = article.description ?: article.title
                
                // Hide divider for last item
                divider.visibility = if (position == articles.size - 1) View.GONE else View.VISIBLE
                
                holder.itemView.setOnClickListener {
                    val linkRaw = article.articleURL ?: ""
                    val link = ensureHttpUrl(linkRaw)
                    if (link.isNotEmpty()) {
                        openArticleLink(link, article.publisherIcon, article.publisherName)
                    } else {
                        com.searcher.zonenews.utils.ToastUtils.showShortToast(
                            this@LevityDetailActivity, 
                            getString(R.string.open_in_browser)
                        )
                    }
                }
            }
        }
        
        publisherArticlesList.adapter = publisherArticlesAdapter
    }
    
    private fun openArticleLink(url: String, publisherIcon: String?, publisherName: String?) {
        val articleOpeningMethod = com.searcher.zonenews.utils.SharedPreferenceUtils.getString(this, "article_opening_method")
        
        if (articleOpeningMethod == "external") {
            // Open in external browser or app
            try {
                val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(url))
                startActivity(intent)
            } catch (e: Exception) {
                com.searcher.zonenews.utils.ToastUtils.showShortToast(this, getString(R.string.open_in_browser))
            }
        } else {
            // Default: Open in in-app browser (WebActivity)
            val intent = android.content.Intent(this, com.searcher.zonenews.ui.web.WebActivity::class.java)
            intent.putExtra(getString(R.string.url_key), url)
            intent.putExtra(getString(R.string.type_key), getString(R.string.news_type))
            intent.putExtra(getString(R.string.publisher_icon_key), publisherIcon)
            intent.putExtra(getString(R.string.publisher_name_key), publisherName)
            startActivity(intent)
        }
    }
    
    private fun ensureHttpUrl(url: String): String {
        if (url.isEmpty()) return url
        val trimmed = url.trim()
        return if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) trimmed else "${getString(R.string.http_prefix)}$trimmed"
    }

    private fun shareLink(link:String) {
        val shareIntent = android.content.Intent(android.content.Intent.ACTION_SEND)
        shareIntent.type = getString(R.string.text_plain)
        shareIntent.putExtra(android.content.Intent.EXTRA_TEXT, link)
        startActivity(android.content.Intent.createChooser(shareIntent, getString(R.string.app_name)))
    }

    private fun initSaveButtonAsUnsaved() {
        val toolbar = findViewById<androidx.appcompat.widget.Toolbar>(R.id.toolbar)
        val saveItem = toolbar.menu.findItem(R.id.action_save)
        if (saveItem != null) {
            saveItem.setIcon(R.drawable.ic_bookmark_outline_24)
            saveItem.iconTintList = android.content.res.ColorStateList.valueOf(androidx.core.content.ContextCompat.getColor(this, R.color.colorTextDeep))
        }
    }
    
    private fun updateSaveButtonState(isSaved: Boolean) {
        val toolbar = findViewById<androidx.appcompat.widget.Toolbar>(R.id.toolbar)
        val saveItem = toolbar.menu.findItem(R.id.action_save)
        if (saveItem != null) {
            if (isSaved) {
                saveItem.setIcon(R.drawable.ic_bookmark_filled_24)
                saveItem.iconTintList = android.content.res.ColorStateList.valueOf(androidx.core.content.ContextCompat.getColor(this, R.color.brand_primary))
            } else {
                saveItem.setIcon(R.drawable.ic_bookmark_outline_24)
                saveItem.iconTintList = android.content.res.ColorStateList.valueOf(androidx.core.content.ContextCompat.getColor(this, R.color.colorTextDeep))
            }
        }
    }

}
