package com.searcher.zonenews.ui.activity

import android.content.Intent
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
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.searcher.zonenews.R
import com.searcher.zonenews.ui.adapter.LevityArticleAdapter
import com.searcher.zonenews.entry.SearchListEntry
import androidx.activity.viewModels
import com.searcher.zonenews.model.LevityViewModel
import dagger.hilt.android.AndroidEntryPoint
import com.searcher.zonenews.entry.HomeDataListEntry
import com.searcher.zonenews.widget.CatMascotView
import kotlinx.coroutines.launch

import com.searcher.zonenews.base.BaseActivity
import com.searcher.zonenews.utils.SystemDialogUtils
import androidx.activity.addCallback

@AndroidEntryPoint
class LevityFeedActivity : BaseActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var swipeRefresh: SwipeRefreshLayout
    private lateinit var adapter: LevityArticleAdapter
    private lateinit var stateContainer: LinearLayout
    private lateinit var stateMascot: CatMascotView
    private lateinit var stateText: TextView
    private lateinit var retryBtn: Button
    private lateinit var headerMascot: CatMascotView

    private val viewModel: LevityViewModel by viewModels()
    private var isLoadingLocal = false
    private var isLastPage = false
    private var pageNo = 1 // Use pageNo instead of offset for consistency with ViewModel
    private val limit = 15

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Force Light Mode locally for this activity only
        delegate.localNightMode = AppCompatDelegate.MODE_NIGHT_NO
        applyStatusBarStyle()
        setContentView(R.layout.activity_levity_feed)
        
        // Setup Views
        recyclerView = findViewById(R.id.recycler_view)
        swipeRefresh = findViewById(R.id.swipe_refresh)
        stateContainer = findViewById(R.id.state_container)
        stateMascot = findViewById(R.id.state_mascot)
        stateText = findViewById(R.id.state_text)
        retryBtn = findViewById(R.id.retry_btn)
        headerMascot = findViewById(R.id.header_mascot)
        
        // Setup Toolbar
        // Setup Toolbar & Exit Logic
        setupExitConfirmation()
        
        headerMascot.setSentiment(0.35) // Always positive in header
        
        adapter = LevityArticleAdapter(this, mutableListOf()) { article ->
            // Open Detail
            val intent = Intent(this, LevityDetailActivity::class.java)
            intent.putExtra("id", article.getArticleID())
            startActivity(intent)
        }
        
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter
        
        recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                val layoutManager = recyclerView.layoutManager as LinearLayoutManager
                val visibleItemCount = layoutManager.childCount
                val totalItemCount = layoutManager.itemCount
                val firstVisibleItemPosition = layoutManager.findFirstVisibleItemPosition()

                if (!isLoadingLocal && !isLastPage) {
                    if ((visibleItemCount + firstVisibleItemPosition) >= totalItemCount
                        && firstVisibleItemPosition >= 0
                        && totalItemCount >= limit
                    ) {
                        loadData(reset = false)
                    }
                }
            }
        })

        swipeRefresh.setOnRefreshListener {
            loadData(reset = true)
        }
        
        retryBtn.setOnClickListener {
             loadData(reset = true)
        }
        
        observeViewModel()
        loadData(reset = true)
    }
    
    private fun observeViewModel() {
        viewModel.levityFeed.observe(this) { response ->
            val articles = response.data?.articles ?: emptyList() 

            if (articles.isEmpty()) {
                 if (pageNo == 1) {
                        showEmptyState()
                    } else {
                        isLastPage = true
                    }
            } else {
                 if (pageNo == 1) {
                        adapter.updateList(articles)
                    } else {
                        adapter.appendList(articles) 
                    }
                    showContentState()
                    if (articles.size < limit) {
                        isLastPage = true
                    }
            }
            isLoadingLocal = false
            adapter.setLoading(false)
            swipeRefresh.isRefreshing = false
        }
        
        viewModel.isLoading.observe(this) { loading ->
            // handled locally partly, but can use this
        }
        
        viewModel.error.observe(this) { error ->
            if (error != null) {
                if (pageNo == 1) {
                    showErrorState()
                } else {
                    // toast error or show something
                }
                isLoadingLocal = false
                adapter.setLoading(false)
                swipeRefresh.isRefreshing = false
            }
        }
    }

    private fun loadData(reset: Boolean) {
        if (isLoadingLocal) return
        isLoadingLocal = true
        
        if (reset) {
            pageNo = 1
            isLastPage = false
            showLoadingState()
        } else {
            pageNo++
            adapter.setLoading(true)
        }
        
        viewModel.getLevityFeed(pageNo, limit)
    }

    
    private fun showLoadingState() {
        recyclerView.visibility = View.GONE
        stateContainer.visibility = View.VISIBLE
        retryBtn.visibility = View.INVISIBLE // Reserve space to prevent jump on error
        stateMascot.setSentiment(0.35)
        stateText.setText(R.string.levity_loading_message)
    }
    
    private fun showEmptyState() {
        recyclerView.visibility = View.GONE
        stateContainer.visibility = View.VISIBLE
        retryBtn.visibility = View.INVISIBLE // Reserve space
        stateText.text = getString(R.string.levity_empty_title) + "\n" + getString(R.string.levity_empty_subtitle)
        stateMascot.setSentiment(0.35)
    }
    
     private fun showErrorState() {
        recyclerView.visibility = View.GONE
        stateContainer.visibility = View.VISIBLE
        retryBtn.visibility = View.VISIBLE
        stateMascot.setSentiment(0.35)
        stateText.setText(R.string.levity_error_failed)
    }
    
    private fun showContentState() {
        recyclerView.visibility = View.VISIBLE
        stateContainer.visibility = View.GONE
    }

    private fun setupExitConfirmation() {
        val exitAction = {
            SystemDialogUtils.showAlertDialog(
                this,
                getString(R.string.levity_exit_dialog_title),
                getString(R.string.levity_exit_dialog_message),
                getString(R.string.levity_exit_dialog_leave),
                getString(R.string.levity_exit_dialog_stay),
                isDestructive = true,
                onPositiveClick = { finish() }
            )
        }

        // Toolbar Back
        findViewById<androidx.appcompat.widget.Toolbar>(R.id.toolbar).setNavigationOnClickListener {
            exitAction()
        }

        // System Back
        onBackPressedDispatcher.addCallback(this) {
            exitAction()
        }
    }
}
