package com.anssy.znewspro.ui.topicmodify

import android.annotation.SuppressLint
import android.os.Bundle
import androidx.activity.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.anssy.znewspro.R
import com.anssy.znewspro.base.BaseActivity
import com.anssy.znewspro.databinding.ActivityAllTopicBinding
import com.anssy.znewspro.entry.TopicListEntry
import com.anssy.znewspro.model.TopicModel
import com.anssy.znewspro.utils.Constants
import com.anssy.znewspro.utils.SharedPreferenceUtils
import com.anssy.znewspro.utils.SystemDialogUtils
import com.zhy.adapter.recyclerview.CommonAdapter
import com.zhy.adapter.recyclerview.base.ViewHolder
import dagger.hilt.android.AndroidEntryPoint
import android.widget.TextView
import android.widget.ImageView

/**
 * @Description 所有话题
 * @Author yulu
 * @CreateTime 2025年07月07日 10:47:43
 */

@AndroidEntryPoint
class TopicAllActivity : BaseActivity() {
    private lateinit var mViewBinding: ActivityAllTopicBinding
    private val topicModel: TopicModel by viewModels()
    private var mList = ArrayList<TopicListEntry.TopicDTO>()
    private lateinit var mAdapter: CommonAdapter<TopicListEntry.TopicDTO>
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mViewBinding = ActivityAllTopicBinding.inflate(layoutInflater)
        setContentView(mViewBinding.root)
        applyStatusBarStyle()
        initView()
        initModel()
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun initModel() {
        // Load user's selected topics first to filter them out
        topicModel.queryMyTopics()
        topicModel.myTopicsEntry.observe(this) { myTopicsResponse ->
            if (myTopicsResponse != null && myTopicsResponse.code == Constants.SUCCESS_CODE) {
                val selectedTopicTags = myTopicsResponse.data?.topics?.map { it.tag } ?: emptyList()
                
                // Now load all topics and filter out selected ones
                topicModel.queryAllTopics()
                topicModel.topicListEntry.observe(this) { allTopicsResponse ->
                    if (allTopicsResponse != null && allTopicsResponse.code == Constants.SUCCESS_CODE) {
                        allTopicsResponse.data?.topics?.let { topics ->
                            // Filter out already selected topics using tag
                            val availableTopics = topics.filter { topic -> !selectedTopicTags.contains(topic.tag) }
                            mList.clear()
                            mList.addAll(availableTopics)
                            mAdapter.notifyDataSetChanged()
                        }
                    }
                }
            } else {
                // If personal topics fail, still load all topics
                topicModel.queryAllTopics()
                topicModel.topicListEntry.observe(this) { allTopicsResponse ->
                    if (allTopicsResponse != null && allTopicsResponse.code == Constants.SUCCESS_CODE) {
                        allTopicsResponse.data?.topics?.let { topics ->
                            mList.clear()
                            mList.addAll(topics)
                            mAdapter.notifyDataSetChanged()
                        }
                    }
                }
            }
        }
        topicModel.commonResponseEntry.observe(this) {
            if (it != null) {
                if (it.code == Constants.SUCCESS_CODE) {
                    setResult(RESULT_OK)
                    SystemDialogUtils.showSuccessMessage(this, getString(R.string.add_success_message))
                    // Find and remove the topic by tag
                    val topicToRemove = mList.find { topic -> topic.tag == it.msg }
                    topicToRemove?.let { topic ->
                        mList.remove(topic)
                    }
                    mAdapter.notifyDataSetChanged()
                } else {
                    if (it.code == 1000) {
                        SystemDialogUtils.showErrorMessage(this, getString(R.string.server_error_message))
                    } else {
                        SystemDialogUtils.showErrorMessage(this, it.msg)
                    }
                }
            } else {
                SystemDialogUtils.dismissLoadingDialog()
            }
        }
    }

    private fun addTopic(topic: TopicListEntry.TopicDTO) {
        SystemDialogUtils.showLoadingDialog(this, getString(R.string.adding_topic_message))
        topicModel.editTopic(Constants.TYPE_TOPIC_ADD, topic.tag)
    }

    private fun initView() {
        // Setup MaterialToolbar with navigation and title
        setupToolbar()
        
        mViewBinding.topicRv.layoutManager = LinearLayoutManager(this, RecyclerView.VERTICAL, false)
        mAdapter = object : CommonAdapter<TopicListEntry.TopicDTO>(this, R.layout.item_topic_recycler, mList) {
            override fun convert(holder: ViewHolder, topic: TopicListEntry.TopicDTO, position: Int) {
                val topicTv: TextView = holder.getView(R.id.topic_tv)
                topicTv.text = topic.displayName
                val addIv: ImageView = holder.getView(R.id.delete_iv)
                addIv.apply {
                    setImageResource(R.drawable.icon_popup_close_default)
                    rotation = 45f
                }
                addIv.setOnClickListener {
                    addTopic(topic)
                }
            }
        }
        mViewBinding.topicRv.adapter = mAdapter
    }

    private fun setupToolbar() {
        val toolbar = mViewBinding.topLayout.toolbar
        toolbar.title = getString(R.string.topic_list_title)
        toolbar.setNavigationOnClickListener { finish() }
    }
}