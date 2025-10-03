package com.anssy.znewspro.ui.topicmodify

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.activity.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.anssy.znewspro.R
import com.anssy.znewspro.base.BaseActivity
import com.anssy.znewspro.databinding.ActivityTopicModifyBinding
import com.anssy.znewspro.entry.TopicListEntry
import com.anssy.znewspro.model.TopicModel
import com.anssy.znewspro.utils.Constants
import com.anssy.znewspro.utils.ToastUtils
import com.anssy.znewspro.utils.SystemDialogUtils
import com.zhy.adapter.recyclerview.CommonAdapter
import com.zhy.adapter.recyclerview.base.ViewHolder
import dagger.hilt.android.AndroidEntryPoint
import android.widget.TextView
import android.widget.ImageView

/**
 * @Description TODO
 * @Author yulu
 * @CreateTime 2025年07月07日 11:31:20
 */
@AndroidEntryPoint
class TopicModifyActivity : BaseActivity() {
    private val topicModel: TopicModel by viewModels()
    private lateinit var mViewBinding: ActivityTopicModifyBinding
    private var mList = ArrayList<TopicListEntry.TopicDTO>()
    private lateinit var mAdapter: CommonAdapter<TopicListEntry.TopicDTO>
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mViewBinding = ActivityTopicModifyBinding.inflate(layoutInflater)
        setContentView(mViewBinding.root)
        applyStatusBarStyle()
        initView()
        initModel()
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun initModel() {
        topicModel.queryMyTopics()
        topicModel.myTopicsEntry.observe(this) {
            if (it != null) {
                if (it.code == Constants.SUCCESS_CODE) {
                    mList.clear()
                    it.data?.topics?.let { topics ->
                        mList.addAll(topics)
                    }
                    // Topics are now managed through API, no need for SharedPreferences
                    mAdapter.notifyDataSetChanged()
                    if (mList.isEmpty()) {
                        mViewBinding.noDataLayout.root.visibility = View.VISIBLE
                    } else {
                        mViewBinding.noDataLayout.root.visibility = View.GONE
                    }
                } else {
                    if (it.code == 1000) {
                        ToastUtils.showShortToast(mContext!!, getString(R.string.topic_modify_server_error))
                    } else {
                        ToastUtils.showShortToast(mContext!!, it.msg)
                    }
                }
            }
        }
        topicModel.commonResponseEntry.observe(this) {
            if (it != null) {
                if (it.code == Constants.SUCCESS_CODE) {
                    setResult(RESULT_OK)
                    SystemDialogUtils.showSuccessMessage(this, getString(R.string.topic_modify_delete_success))
                    // Find and remove the topic by tag
                    val topicToRemove = mList.find { topic -> topic.tag == it.msg }
                    topicToRemove?.let { topic ->
                        mList.remove(topic)
                    }
                    mAdapter.notifyDataSetChanged()
                } else {
                    if (it.code == 1000) {
                        SystemDialogUtils.showErrorMessage(this, getString(R.string.topic_modify_server_error_dialog))
                    } else {
                        SystemDialogUtils.showErrorMessage(this, it.msg)
                    }
                }
            } else {
                SystemDialogUtils.dismissLoadingDialog()
            }
        }
    }

    private fun initView() {
        mViewBinding.topicRecycler.layoutManager = LinearLayoutManager(this, RecyclerView.VERTICAL, false)
        mAdapter = object : CommonAdapter<TopicListEntry.TopicDTO>(this, R.layout.item_topic_recycler, mList) {
            override fun convert(holder: ViewHolder, topic: TopicListEntry.TopicDTO, position: Int) {
                val topicTv: TextView = holder.getView(R.id.topic_tv)
                topicTv.text = topic.displayName
                val deleteIv: ImageView = holder.getView(R.id.delete_iv)
                deleteIv.setOnClickListener {
                    SystemDialogUtils.showAlertDialog(
                        this@TopicModifyActivity,
                        getString(R.string.topic_modify_delete),
                        "Are you sure you want to delete this topic?",
                        getString(R.string.topic_modify_delete),
                        onPositiveClick = {
                            SystemDialogUtils.showLoadingDialog(this@TopicModifyActivity, getString(R.string.topic_modify_submitting))
                            topicModel.editTopic(Constants.TYPE_TOPIC_DELETE, topic.tag)
                        }
                    )
                }
            }
        }
        mViewBinding.topicRecycler.adapter = mAdapter
        mViewBinding.addIv.setOnClickListener {
            startActivityForResult(Intent(this, TopicAllActivity::class.java)) { code: Int, data: Intent? ->
                // code = resultCode
                if (code == RESULT_OK) {
                    topicModel.queryMyTopics()
                }
            }
        }
    }
}