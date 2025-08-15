package com.anssy.znewspro.ui.mainfrag

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.ImageView
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.anssy.znewspro.utils.Constants
import com.anssy.znewspro.R
import com.anssy.znewspro.base.BaseFragment
import com.anssy.znewspro.databinding.FragAdviceBinding
import com.anssy.znewspro.entry.SearchListEntry
import com.anssy.znewspro.model.PersonRecommendModel
import com.anssy.znewspro.ui.MainActivity
import com.anssy.znewspro.ui.newsdetail.NewsDetailActivity
import com.anssy.znewspro.ui.topicmodify.TopicModifyActivity
import com.anssy.znewspro.utils.CalculateUtil
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
 * @Description 推荐
 * @Author yulu
 * @CreateTime 2025年06月30日 09:27:40
 */

class AdviceFrag : BaseFragment() {
    private lateinit var mViewBinding: FragAdviceBinding
    private var pageNo = 1
    private val pageSize = 10

    companion object {
        fun getInstance(): AdviceFrag {
            return AdviceFrag()
        }
    }

    private val personRecommendModel: PersonRecommendModel by viewModels()
    private var mProgressWidth = 0
    private lateinit var mAdapter: CommonAdapter<SearchListEntry.DataDTO.ArticlesDTO>
    private var mNewsList = ArrayList<SearchListEntry.DataDTO.ArticlesDTO>()
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        mViewBinding = FragAdviceBinding.inflate(layoutInflater)
        return mViewBinding.root
    }

    private var isRefresh = true

    override fun initData() {
        mProgressWidth = resources.displayMetrics.widthPixels - Utils.dpToPx(36f, resources)
        mViewBinding.searchEt.setOnEditorActionListener { v, actionId, event ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                Log.e("xxx", "search")

            }
            false
        }
        mViewBinding.homeRecycler.layoutManager =
            LinearLayoutManager(mContext, RecyclerView.VERTICAL, false)
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        mAdapter = object :
            CommonAdapter<SearchListEntry.DataDTO.ArticlesDTO>(
                mContext,
                R.layout.item_home_recycler,
                mNewsList
            ) {
            @SuppressLint("SetTextI18n")
            override fun convert(
                holder: ViewHolder,
                t: SearchListEntry.DataDTO.ArticlesDTO,
                position: Int
            ) {
                val placeTv: TextView = holder.getView(R.id.place_tv)
                val tagTv: TextView = holder.getView(R.id.tag_tv)
                val titleTv: TextView = holder.getView(R.id.news_title_tv)
                val newsIv: ImageView = holder.getView(R.id.news_iv)
                val trackView: View = holder.getView(R.id.progress_track)
                val highlightView: View = holder.getView(R.id.progress_highlight)
                val timeTv: TextView = holder.getView(R.id.news_time_tv)
                val countTv: TextView = holder.getView(R.id.news_count_tv)
                countTv.text = "${t.nSources}个报道"
                val transScoreTv: TextView = holder.getView(R.id.trans_score_tv)
                holder.convertView.setOnClickListener {
                    val intent = Intent(mContext, NewsDetailActivity::class.java)
                    intent.putExtra("id", t.articleID)
                    startActivity(intent)
                }
                placeTv.text = t.region
                transScoreTv.text = "Subjectivity: " + CalculateUtil.round(t.metrics.sentiment, 2)
                tagTv.text = t.sector
                titleTv.text = t.title
                Glide.with(mContext).load(t.pictureURL).error(R.drawable.ease_default_image)
                    .into(newsIv)

                try {
                    val parse = dateFormat.parse(t.date)
                    timeTv.text = Utils.getSpaceTime(parse!!.time)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                // progress highlight from zero
                trackView.post {
                    val totalWidth = trackView.width
                    val half = totalWidth / 2
                    val score = CalculateUtil.round(t.metrics.sentiment, 2)
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

            }
        }
        mViewBinding.homeRecycler.adapter = mAdapter
        mViewBinding.homeRecycler.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                val activity = requireActivity() as MainActivity
                if (dy > 0) {
                    activity.hideBottomBar()
                } else if (dy < 0) {
                    activity.showBottomBar()
                }
            }

            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                super.onScrollStateChanged(recyclerView, newState)
                val activity = requireActivity() as MainActivity
                if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                    activity.scheduleBottomBarAutoShow()
                } else {
                    activity.cancelBottomBarAutoShow()
                }
            }
        })
        mViewBinding.settingIv.setOnClickListener {
            val intent = Intent(mContext, TopicModifyActivity::class.java)
            startActivity(intent)
        }
        initModel()
        personRecommendModel.queryRecommendList(pageNo, pageSize)
        mViewBinding.smartRefresh.setOnRefreshLoadMoreListener(object : OnRefreshLoadMoreListener {
            override fun onRefresh(refreshLayout: RefreshLayout) {
                isRefresh = true
                pageNo = 1
                personRecommendModel.queryRecommendList(pageNo, pageSize)
            }

            override fun onLoadMore(refreshLayout: RefreshLayout) {
                isRefresh = false
                pageNo++
                personRecommendModel.queryRecommendList(pageNo, pageSize)
            }

        })
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun initModel() {
        personRecommendModel.recommendListEntry.observe(viewLifecycleOwner) {
            if (it != null) {
                if (it.code == Constants.SUCCESS_CODE) {
                    if (isRefresh) {
                        mNewsList.clear()
                        mViewBinding.smartRefresh.finishRefresh(true)
                    } else {
                        mViewBinding.smartRefresh.finishLoadMore(true)
                    }
                    mNewsList.addAll(it.data.articles)
                    mAdapter.notifyDataSetChanged()
                } else {
                    if (isRefresh) {
                        mViewBinding.smartRefresh.finishRefresh(false)
                    } else {
                        mViewBinding.smartRefresh.finishLoadMore(false)
                    }
                    if (it.code == 1000) {
                        ToastUtils.showShortToast(mContext!!, "Server Error")
                    } else {
                        ToastUtils.showShortToast(mContext!!, it.msg)
                    }
                }
            }
        }
    }


}