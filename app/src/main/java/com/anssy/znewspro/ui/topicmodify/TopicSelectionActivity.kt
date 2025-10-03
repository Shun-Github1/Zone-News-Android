package com.anssy.znewspro.ui.topicmodify

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.OnBackPressedCallback
import androidx.activity.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.anssy.znewspro.R
import com.anssy.znewspro.base.BaseActivity
import com.anssy.znewspro.databinding.ActivityTopicSelectionBinding
import com.anssy.znewspro.entry.TopicListEntry
import com.anssy.znewspro.model.TopicModel
import com.anssy.znewspro.utils.Constants
import com.anssy.znewspro.utils.ToastUtils
import com.anssy.znewspro.utils.network.exception.GenericResponse
import com.zhy.adapter.recyclerview.CommonAdapter
import com.zhy.adapter.recyclerview.base.ViewHolder

/**
 * Topic Selection Activity - matches iOS design
 * Allows users to select/deselect topics for personalized recommendations
 */
class TopicSelectionActivity : BaseActivity() {
    private val topicModel: TopicModel by viewModels()
    private lateinit var mViewBinding: ActivityTopicSelectionBinding
    private var allTopics = ArrayList<TopicListEntry.TopicDTO>()
    private var selectedTopics = ArrayList<String>() // Keep as tags for API calls
    private lateinit var mAdapter: CommonAdapter<TopicListEntry.TopicDTO>
    private var hasChanges = false

    companion object {
        const val EXTRA_SELECTED_TOPICS = "selected_topics"
        const val RESULT_TOPICS_UPDATED = 1001
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mViewBinding = ActivityTopicSelectionBinding.inflate(layoutInflater)
        setContentView(mViewBinding.root)
        applyStatusBarStyle()
        
        // Get currently selected topics from intent
        selectedTopics = intent.getStringArrayListExtra(EXTRA_SELECTED_TOPICS) ?: ArrayList()
        
        setupBackPressedHandler()
        initView()
        initModel()
    }

    private fun initView() {
        // Set up toolbar
        mViewBinding.toolbar.setNavigationOnClickListener { 
            if (hasChanges) {
                setResult(RESULT_TOPICS_UPDATED)
            }
            finish()
        }
        mViewBinding.toolbar.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.action_done -> {
                    if (hasChanges) {
                        setResult(RESULT_TOPICS_UPDATED)
                    }
                    finish()
                    true
                }
                else -> false
            }
        }
        
        // Set up the custom Done button click listener
        mViewBinding.toolbar.post {
            val doneMenuItem = mViewBinding.toolbar.menu.findItem(R.id.action_done)
            doneMenuItem?.let { menuItem ->
                val customActionView = menuItem.actionView as? android.widget.TextView
                customActionView?.setOnClickListener {
                    if (hasChanges) {
                        setResult(RESULT_TOPICS_UPDATED)
                    }
                    finish()
                }
            }
        }

        // Set up RecyclerView
        mViewBinding.topicsRecycler.layoutManager = LinearLayoutManager(this, RecyclerView.VERTICAL, false)
        mAdapter = object : CommonAdapter<TopicListEntry.TopicDTO>(this, R.layout.item_topic_selection, allTopics) {
            override fun convert(holder: ViewHolder, topic: TopicListEntry.TopicDTO, position: Int) {
                val topicTv = holder.getView<android.widget.TextView>(R.id.topic_tv)
                val topicIcon = holder.getView<android.widget.ImageView>(R.id.topic_icon)
                val checkmarkIv = holder.getView<android.widget.ImageView>(R.id.checkmark_iv)
                val divider = holder.getView<View>(R.id.divider)
                
                topicTv.text = topic.displayName
                
                // Set topic icon based on tag
                topicIcon.setImageResource(getTopicIcon(topic.tag))
                
                // Hide divider for last item
                if (position == allTopics.size - 1) {
                    divider.visibility = View.GONE
                } else {
                    divider.visibility = View.VISIBLE
                }
                
                // Set selection state
                val isSelected = selectedTopics.contains(topic.tag)
                checkmarkIv.setImageResource(
                    if (isSelected) R.drawable.ic_check_circle_filled 
                    else R.drawable.ic_circle_outline
                )
                checkmarkIv.setColorFilter(
                    if (isSelected) getColor(R.color.global_color) 
                    else getColor(R.color.colorTextHint)
                )
                
                // Set click listener on the topic row specifically
                val topicRow = holder.getView<android.widget.LinearLayout>(R.id.topic_row)
                topicRow.setOnClickListener {
                    toggleTopic(topic)
                }
            }
        }
        mViewBinding.topicsRecycler.adapter = mAdapter
    }

    private fun initModel() {
        // Load user's selected topics first
        topicModel.queryMyTopics()
        topicModel.myTopicsEntry.observe(this) { response ->
            if (response != null) {
                if (response.code == Constants.SUCCESS_CODE) {
                    // Update selected topics from server response
                    selectedTopics.clear()
                    response.data?.topics?.let { topics ->
                        selectedTopics.addAll(topics.map { it.tag })
                    }
                    
                    // Now load all available topics
                    topicModel.queryAllTopics()
                } else {
                    val errorMessage = when (response.code) {
                        1000 -> getString(R.string.server_error_message)
                        else -> response.msg ?: getString(R.string.topics_error_loading)
                    }
                    ToastUtils.showShortToast(this, errorMessage)
                    // Still try to load all topics even if personal topics fail
                    topicModel.queryAllTopics()
                }
            } else {
                ToastUtils.showShortToast(this, getString(R.string.topics_error_loading))
                // Still try to load all topics even if personal topics fail
                topicModel.queryAllTopics()
            }
        }
        
        // Observe all topics response
        topicModel.topicListEntry.observe(this) { response ->
            if (response != null) {
                if (response.code == Constants.SUCCESS_CODE) {
                    allTopics.clear()
                    response.data?.topics?.let { topics ->
                        allTopics.addAll(topics)
                    }
                    mAdapter.notifyDataSetChanged()
                } else {
                    val errorMessage = when (response.code) {
                        1000 -> getString(R.string.server_error_message)
                        else -> response.msg ?: getString(R.string.topics_error_loading)
                    }
                    ToastUtils.showShortToast(this, errorMessage)
                }
            } else {
                ToastUtils.showShortToast(this, getString(R.string.topics_error_loading))
            }
        }
    }

    private fun toggleTopic(topic: TopicListEntry.TopicDTO) {
        if (selectedTopics.contains(topic.tag)) {
            // Remove topic
            selectedTopics.remove(topic.tag)
            updateTopicOnServer(topic.tag, Constants.TYPE_TOPIC_DELETE)
        } else {
            // Add topic
            selectedTopics.add(topic.tag)
            updateTopicOnServer(topic.tag, Constants.TYPE_TOPIC_ADD)
        }
        
        hasChanges = true
        mAdapter.notifyDataSetChanged()
    }

    private fun updateTopicOnServer(topicTag: String, action: String) {
        topicModel.editTopic(action, topicTag)
        topicModel.commonResponseEntry.observe(this) { response ->
            if (response != null) {
                if (response.code != Constants.SUCCESS_CODE) {
                    // Revert the change if server update failed
                    if (action == Constants.TYPE_TOPIC_ADD) {
                        selectedTopics.remove(topicTag)
                    } else {
                        selectedTopics.add(topicTag)
                    }
                    mAdapter.notifyDataSetChanged()
                    
                    val errorMessage = when (response.code) {
                        1000 -> getString(R.string.server_error_message)
                        else -> response.msg ?: getString(R.string.unknown_error)
                    }
                    ToastUtils.showShortToast(this, errorMessage)
                }
            }
        }
    }

    private fun setupBackPressedHandler() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (hasChanges) {
                    setResult(RESULT_TOPICS_UPDATED)
                }
                finish()
            }
        })
    }

    private fun getTopicIcon(topicTag: String): Int {
        return when (topicTag.lowercase()) {
            "conflict" -> R.drawable.ic_security_24
            "culture" -> R.drawable.ic_palette_24
            "diplomacy" -> R.drawable.ic_public_24
            "economics" -> R.drawable.ic_trending_up_24
            "entertainment" -> R.drawable.ic_live_tv_24
            "politics" -> R.drawable.ic_account_balance_24
            "science" -> R.drawable.ic_science_24
            "sports" -> R.drawable.ic_sports_soccer_24
            "technology" -> R.drawable.ic_memory_24
            else -> R.drawable.ic_security_24 // Default icon
        }
    }
}
