package com.anssy.znewspro.ui.mainfrag.homechild

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.anssy.znewspro.utils.Constants
import com.anssy.znewspro.R
import com.anssy.znewspro.base.BaseFragment
import com.anssy.znewspro.databinding.FragChildHomeBinding
import com.anssy.znewspro.entry.HomeDataListEntry
import com.anssy.znewspro.model.HomeModel
import com.anssy.znewspro.selfview.NewNestedScrollView
import com.anssy.znewspro.ui.MainActivity
import com.anssy.znewspro.ui.newsdetail.NewsDetailActivity
import com.anssy.znewspro.utils.CalculateUtil
import com.anssy.znewspro.utils.ToastUtils
import com.anssy.znewspro.utils.Utils
import com.bumptech.glide.Glide
import com.scwang.smartrefresh.layout.api.RefreshLayout
import com.scwang.smartrefresh.layout.listener.OnRefreshLoadMoreListener
import com.zhpan.bannerview.BaseBannerAdapter
import com.zhpan.bannerview.BaseViewHolder
import com.zhy.adapter.recyclerview.CommonAdapter
import com.zhy.adapter.recyclerview.base.ViewHolder
import java.text.SimpleDateFormat
import java.util.Locale


/**
 * @Description Today/HongKong/China
 * @Author yulu
 * @CreateTime 2025年06月30日 11:22:01
 */

class HomeChildFrag : BaseFragment() {
    private lateinit var mViewBinding: FragChildHomeBinding
    private val mHomeModel: HomeModel by viewModels()

    companion object {
        private const val TYPE_NEWS = "type"
        fun getInstance(tag: String): HomeChildFrag {
            val bundle = Bundle()
            bundle.putString(TYPE_NEWS, tag)
            val childFrag = HomeChildFrag()
            childFrag.arguments = bundle
            return childFrag
        }
    }

    private lateinit var mAdapter: CommonAdapter<HomeDataListEntry.DataDTO.ArticlesDTO>
    private var mNewsList = ArrayList<HomeDataListEntry.DataDTO.ArticlesDTO>()
    private var mBannerList = ArrayList<HomeDataListEntry.DataDTO.HeadlinesDTO>()
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        mViewBinding = FragChildHomeBinding.inflate(inflater)
        return mViewBinding.root
    }

    override fun onStart() {
        super.onStart()
        if (mViewBinding.homeBanner.visibility == View.VISIBLE) {
            mViewBinding.homeBanner.startLoop()
        }
    }

    private lateinit var mBannerAdapter: HomeAdapter
    private var pageNo = 1
    private val pageSize = 10
    private var mCurrentType = ""
    override fun initData() {
        mCurrentType = arguments?.getString(TYPE_NEWS).toString()
        initView()
        initModel()
    }

    private fun initView() {
        mBannerAdapter = HomeAdapter()
        mViewBinding.homeBanner.setAdapter(mBannerAdapter)
        // Show banner only on Today tab
        val showBanner = shouldShowBanner()
        mViewBinding.homeBanner.visibility = if (showBanner) View.VISIBLE else View.GONE
        mViewBinding.homeBanner.setOnPageClickListener {
            val headlinesDTO = mBannerList.get(it)
            val intent = Intent(mContext, NewsDetailActivity::class.java)
            intent.putExtra("id",headlinesDTO.articleID)
            startActivity(intent)
        }
        if (showBanner) {
            mViewBinding.homeBanner.create()
        }

        mViewBinding.homeRecycler.layoutManager =
            object : LinearLayoutManager(mContext, RecyclerView.VERTICAL, false) {
                override fun canScrollVertically(): Boolean {
                    return false
                }
            }
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        mAdapter = object :
            CommonAdapter<HomeDataListEntry.DataDTO.ArticlesDTO>(
                mContext,
                R.layout.item_home_recycler,
                mNewsList
            ) {
            @SuppressLint("SetTextI18n")
            override fun convert(
                holder: ViewHolder?,
                t: HomeDataListEntry.DataDTO.ArticlesDTO,
                position: Int
            ) {
                val placeTv: TextView = holder!!.getView(R.id.place_tv)
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
                    intent.putExtra("id",t.articleID)
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
                // layout highlight based on score in range [-1, 1]
                trackView.post {
                    val totalWidth = trackView.width
                    val half = totalWidth / 2
                    val score = CalculateUtil.round(t.metrics.sentiment, 2)
                    val distance = (kotlin.math.abs(score) * half).toInt()
                    val lp = highlightView.layoutParams as ConstraintLayout.LayoutParams
                    if (distance <= 0) {
                        // Avoid width==0 which equals MATCH_CONSTRAINT in ConstraintLayout
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
        mViewBinding.scrollView.addScrollChangeListener(object :
            NewNestedScrollView.AddScrollChangeListener {
            override fun onScrollChange(
                scrollX: Int,
                scrollY: Int,
                oldScrollX: Int,
                oldScrollY: Int
            ) {
                val activity = requireActivity() as MainActivity
                if (scrollY > oldScrollY) {
                    activity.hideBottomBar()
                } else if (scrollY < oldScrollY) {
                    activity.showBottomBar()
                }
            }

            override fun onScrollState(state: NewNestedScrollView.ScrollState?) {
                val activity = requireActivity() as MainActivity
                when (state) {
                    NewNestedScrollView.ScrollState.IDLE -> activity.scheduleBottomBarAutoShow()
                    NewNestedScrollView.ScrollState.DRAG, NewNestedScrollView.ScrollState.SCROLLING -> activity.cancelBottomBarAutoShow()
                    else -> {}
                }
            }

        })
        mViewBinding.smartRefresh.setOnRefreshLoadMoreListener(object : OnRefreshLoadMoreListener {
            override fun onRefresh(refreshLayout: RefreshLayout) {
                refresh = true
                pageNo = 1
                mHomeModel.getHomeDataList(mCurrentType, pageNo, pageSize)
            }

            override fun onLoadMore(refreshLayout: RefreshLayout) {
                if (isLastPage) {
                    refreshLayout.finishLoadMore(true)
                    return
                }
                refresh = false
                pageNo++
                mHomeModel.getHomeDataList(mCurrentType, pageNo, pageSize)
            }

        })
    }

    private var refresh = true
    private var isBannerLoad = false
    private var isLastPage = false

    @SuppressLint("NotifyDataSetChanged")
    private fun initModel() {
        mHomeModel.getHomeDataList(mCurrentType, pageNo, pageSize)
        mHomeModel.homeDataList.observe(viewLifecycleOwner) {
            if (it.code == Constants.SUCCESS_CODE) {
                if (refresh) {
                    isBannerLoad = false
                    mNewsList.clear()
                    mBannerList.clear()
                    mViewBinding.smartRefresh.finishRefresh(true)
                } else {
                    mViewBinding.smartRefresh.finishLoadMore(true)
                }
                isLastPage = it.data.articles.isEmpty()
                mNewsList.addAll(it.data.articles)
                val lastPosition = mNewsList.size
                if (!isBannerLoad && shouldShowBanner()) {
                    mBannerList.addAll(it.data.headlines)
                    mViewBinding.homeBanner.refreshData(mBannerList)
                    isBannerLoad = true
                }
                if (refresh){
                    mAdapter.notifyDataSetChanged()
                }else{
                    mAdapter.notifyItemRangeInserted(lastPosition,it.data.articles.size)
                }

            } else {
                if (refresh) {
                    mViewBinding.smartRefresh.finishRefresh(false)
                } else {
                    mViewBinding.smartRefresh.finishLoadMore(false)
                }
                ToastUtils.showShortToast(mContext!!, it.msg)
            }

        }
    }


    override fun onStop() {
        super.onStop()
        if (mViewBinding.homeBanner.visibility == View.VISIBLE) {
            mViewBinding.homeBanner.stopLoop()
        }
    }

    private fun shouldShowBanner(): Boolean {
        // Only show banner on Today; hide on Hong Kong and China
        return mCurrentType == getString(R.string.today)
    }


    inner class NetViewHolder(itemView: View) :
        BaseViewHolder<HomeDataListEntry.DataDTO.HeadlinesDTO>(itemView) {
        private val mBannerIv: ImageView = itemView.findViewById(R.id.banner_image)
        private val mTitleTv: TextView = itemView.findViewById(R.id.banner_title_tv)
        private val mTransTv: TextView = itemView.findViewById(R.id.banner_desc_tv)
        override fun bindData(
            data: HomeDataListEntry.DataDTO.HeadlinesDTO,
            position: Int,
            pageSize: Int
        ) {
            Glide.with(mContext!!).load(data.pictureURL)
                .centerCrop().error(R.drawable.ease_default_image).into(mBannerIv)
            mTitleTv.text = data.title
            mTransTv.text = data.description
        }
    }

    /**
     * banner 适配器
     */
    inner class HomeAdapter :
        BaseBannerAdapter<HomeDataListEntry.DataDTO.HeadlinesDTO, NetViewHolder>() {
        override fun onBind(
            holder: NetViewHolder,
            data: HomeDataListEntry.DataDTO.HeadlinesDTO,
            position: Int,
            pageSize: Int
        ) {
            holder.bindData(data, position, pageSize)
        }

        override fun createViewHolder(itemView: View, viewType: Int): NetViewHolder {
            return NetViewHolder(itemView)
        }

        override fun getLayoutId(viewType: Int): Int {
            return R.layout.item_banner
        }
    }

}