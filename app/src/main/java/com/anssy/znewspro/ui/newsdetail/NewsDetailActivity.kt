package com.anssy.znewspro.ui.newsdetail

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import android.view.Gravity
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.AccelerateInterpolator
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.view.animation.Interpolator
import android.view.animation.Transformation
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.activity.viewModels
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.animation.PathInterpolatorCompat
import com.anssy.znewspro.utils.Constants
import com.anssy.znewspro.R
import com.anssy.znewspro.base.BaseActivity
import com.anssy.znewspro.databinding.ActivityNewsDetailBinding
import com.anssy.znewspro.entry.ArticleDetailEntry
import com.anssy.znewspro.model.NewsDetailModel
import com.anssy.znewspro.selfview.popup.FeedBackPopupWindow
import com.anssy.znewspro.utils.ToastUtils
import com.bumptech.glide.Glide
import com.hjq.shape.view.ShapeButton
import com.kongzue.dialogx.dialogs.TipDialog
import com.kongzue.dialogx.dialogs.WaitDialog
import kotlinx.coroutines.launch


/**
 * @Description 新闻详情
 * @Author yulu
 * @CreateTime 2025年07月01日 16:58:28
 */
class NewsDetailActivity : BaseActivity() {
    private val newsDetailModel:NewsDetailModel by viewModels ()
    private lateinit var mViewBinding: ActivityNewsDetailBinding
    private lateinit var mFeedBackPopupWindow: FeedBackPopupWindow
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mViewBinding = ActivityNewsDetailBinding.inflate(layoutInflater)
        setContentView(mViewBinding.root)
        applyStatusBarStyle()
        initView()
    }


    private var articleId = ""
    private fun initView() {
        articleId = intent.getStringExtra("id").toString()
        mFeedBackPopupWindow = FeedBackPopupWindow(this)
		setupToolbar()
        initModel()
        mViewBinding.feedBackLayout.setOnClickListener {
                showFeedBackWindow()
        }
		mViewBinding.generateContextBtn.setOnClickListener {
			// Placeholder for future context generation
		}
	}

	private fun setupToolbar() {
		val toolbar = mViewBinding.topAppBar
		toolbar.setNavigationOnClickListener { finish() }
		toolbar.setOnMenuItemClickListener { item: MenuItem ->
			when (item.itemId) {
				R.id.action_share -> {
					if (mArticleDetailEntry == null) return@setOnMenuItemClickListener true
            val link = when {
                !mArticleDetailEntry!!.shareURL.isNullOrEmpty() -> mArticleDetailEntry!!.shareURL
                !mArticleDetailEntry!!.articleURL.isNullOrEmpty() -> mArticleDetailEntry!!.articleURL
                else -> mArticleDetailEntry!!.pictureURL
            }
            shareLink(link)
					addHis()
					true
				}
				R.id.action_save -> {
					if (mArticleDetailEntry == null) return@setOnMenuItemClickListener true
					WaitDialog.show(getString(R.string.submitting_message))
					if (mArticleDetailEntry!!.liked) {
						newsDetailModel.deleteCollect(articleId)
					} else {
						newsDetailModel.collectHis(articleId)
					}
					true
				}
				else -> false
			}
        }
    }

    /**
     * 分享链接
     */

    private fun shareLink(link:String) {
        val shareIntent = Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_TEXT, link);
        startActivity(Intent.createChooser(shareIntent, getString(R.string.app_name)));
    }
    /**
     *获取数据
     */
    private fun initModel(){
        newsDetailModel.queryNewsDetail(articleId)
        newsDetailModel.newsDetailEntry.observe(this) {
            if (it.code == Constants.SUCCESS_CODE) {
                completeView(it.data)
                addHis()
            } else {
                ToastUtils.showShortToast(mContext!!, it.msg)
            }
        }
        newsDetailModel.feeBackResponseEntry.observe(this){
            if (it!=null){
                if (it.code== Constants.SUCCESS_CODE){
                    TipDialog.show(getString(R.string.success_message),WaitDialog.TYPE.SUCCESS)
                }else{
                    if (it.code==1000){
                        TipDialog.show(getString(R.string.server_error_message),WaitDialog.TYPE.ERROR)
                    }else{
                        TipDialog.show(it.msg,WaitDialog.TYPE.ERROR)
                    }
                }
            }else{
               WaitDialog.dismiss()
            }
        }
        newsDetailModel.addHisEntry.observe(this){
            if (it!=null){
                if (it.code== Constants.SUCCESS_CODE){
                    Log.e("xxx","添加成功");
                }else{
                    Log.e("xxx","添加失败")
                }
            }
        }
        newsDetailModel.collectEntry.observe(this){
            if (it!=null){
                if (it.code== Constants.SUCCESS_CODE){
                    WaitDialog.dismiss()
                    ToastUtils.showShortToast(mContext!!,getString(R.string.collect_success_toast))
                    mArticleDetailEntry!!.liked = true
					val saveItem = mViewBinding.topAppBar.menu.findItem(R.id.action_save)
					saveItem.setIcon(R.drawable.pg_tab_special_click)
                }else{
                    if (it.code==1000){
                        TipDialog.show(getString(R.string.server_error_message),WaitDialog.TYPE.ERROR)
                    }else{
                        TipDialog.show(it.msg,WaitDialog.TYPE.ERROR)
                    }
                }
            }else{
                WaitDialog.dismiss()
            }
        }
        newsDetailModel.deleteCollectEntry.observe(this){
            if (it!=null){
                if (it.code== Constants.SUCCESS_CODE){
                    WaitDialog.dismiss()
                    ToastUtils.showShortToast(mContext!!,getString(R.string.uncollect_success_toast))
                    mArticleDetailEntry!!.liked = false
					val saveItem = mViewBinding.topAppBar.menu.findItem(R.id.action_save)
					saveItem.setIcon(R.drawable.pg_detail_collect)
                }else{
                    if (it.code==1000){
                        TipDialog.show(getString(R.string.server_error_message),WaitDialog.TYPE.ERROR)
                    }else{
                        TipDialog.show(it.msg,WaitDialog.TYPE.ERROR)
                    }
                }
            }else{
                WaitDialog.dismiss()
            }
        }

    }
    private var mArticleDetailEntry: ArticleDetailEntry.DataDTO?=null
    @SuppressLint("NotifyDataSetChanged")
    private fun completeView(articleDetailEntry: ArticleDetailEntry.DataDTO){
        mArticleDetailEntry = articleDetailEntry
        Glide.with(mContext!!).load(articleDetailEntry.pictureURL).error(R.drawable.ease_default_image)
            .into(mViewBinding.newsIv)
        mViewBinding.newsTitleTv.text = articleDetailEntry.title
        mViewBinding.newsDescTv.text = articleDetailEntry.description
		// Update toolbar save icon state
		val saveItem = mViewBinding.topAppBar.menu.findItem(R.id.action_save)
		if (articleDetailEntry.liked){
			saveItem.setIcon(R.drawable.pg_tab_special_click)
		}else{
			saveItem.setIcon(R.drawable.pg_detail_collect)
		}
		// Sentiment meter
		mViewBinding.sentimentMeter.setSentiment(articleDetailEntry.metrics.sentiment)
		// Subjectivity score
		findViewById<SubjectivityScoreView>(R.id.subjectivity_score).setSubjectivity(articleDetailEntry.metrics.subjectivity)
		// Publisher distribution
		mViewBinding.publisherDistribution.setData(
			PublisherDistributionView.Data(
				centricPercent = articleDetailEntry.coverage.percentage.centric,
				centricIcons = articleDetailEntry.coverage.icons.centric.map { icon ->
					PublisherDistributionView.Icon(
						size = icon.size,
						rx = icon.rx,
						ry = icon.ry,
						logo = icon.logo
					)
				},
				progressiveIcons = articleDetailEntry.coverage.icons.progressive.map { icon ->
					PublisherDistributionView.Icon(
						size = icon.size,
						rx = icon.rx,
						ry = icon.ry,
						logo = icon.logo
					)
				}
			)
		)

        setupPublisherArticles(articleDetailEntry)
	}

    private fun setupPublisherArticles(data: ArticleDetailEntry.DataDTO) {
        val listRv = findViewById<RecyclerView>(R.id.publisher_articles_list)
        listRv.layoutManager = object : LinearLayoutManager(this, RecyclerView.VERTICAL, false) {
            override fun canScrollVertically(): Boolean = false
        }
        val items = data.articles
        listRv.adapter = object : com.zhy.adapter.recyclerview.CommonAdapter<ArticleDetailEntry.DataDTO.ArticlesDTO>(
            this, R.layout.item_publisher_article, items
        ) {
            override fun convert(holder: com.zhy.adapter.recyclerview.base.ViewHolder, t: ArticleDetailEntry.DataDTO.ArticlesDTO, position: Int) {
                val iconIv = holder.getView<ImageView>(R.id.article_publisher_icon)
                val nameTv = holder.getView<TextView>(R.id.article_publisher_name)
                val titleTv = holder.getView<TextView>(R.id.article_title)
                com.bumptech.glide.Glide.with(this@NewsDetailActivity).load(t.publisherIcon).error(R.drawable.ease_default_image).into(iconIv)
                nameTv.text = if (t.publisherName.isNullOrEmpty()) getString(R.string.about) else t.publisherName
                val title = t.title
                titleTv.text = when {
                    !title.isNullOrEmpty() -> title
                    !t.description.isNullOrEmpty() -> t.description
                    else -> getString(R.string.details)
                }
                holder.convertView.setOnClickListener {
                    val linkRaw = when {
                        !t.articleURL.isNullOrEmpty() -> t.articleURL
                        !mArticleDetailEntry?.articleURL.isNullOrEmpty() -> mArticleDetailEntry?.articleURL
                        !mArticleDetailEntry?.shareURL.isNullOrEmpty() -> mArticleDetailEntry?.shareURL
                        else -> ""
                    }
                    val link = ensureHttpUrl(linkRaw ?: "")
                    if (link.isNotEmpty()) {
                        val intent = Intent(this@NewsDetailActivity, com.anssy.znewspro.ui.web.WebActivity::class.java)
                        intent.putExtra("url", link)
                        intent.putExtra("type","news")
                        startActivity(intent)
                    } else {
                        ToastUtils.showShortToast(this@NewsDetailActivity, getString(R.string.open_in_browser))
                    }
                }
            }
        }
        listRv.addItemDecoration(object : RecyclerView.ItemDecoration() {
            override fun getItemOffsets(outRect: android.graphics.Rect, view: View, parent: RecyclerView, state: RecyclerView.State) {
                super.getItemOffsets(outRect, view, parent, state)
                val pos = parent.getChildAdapterPosition(view)
                // Add top divider for all but the first, left aligned with text by adding left margin of 52dp (40 icon + 12 spacing)
                if (pos > 0) {
                    outRect.top = 1
                }
            }

            override fun onDraw(c: android.graphics.Canvas, parent: RecyclerView, state: RecyclerView.State) {
                val paint = android.graphics.Paint().apply { color = resources.getColor(R.color.line_color); strokeWidth = resources.displayMetrics.density }
                val left = parent.paddingLeft + (resources.displayMetrics.density * (40 + 12)).toInt()
                val right = parent.width - parent.paddingRight
                for (i in 0 until parent.childCount) {
                    val child = parent.getChildAt(i)
                    val pos = parent.getChildAdapterPosition(child)
                    if (pos > 0) {
                        val y = child.top.toFloat()
                        c.drawLine(left.toFloat(), y, right.toFloat(), y, paint)
                    }
                }
            }
        })
    }

    private fun ensureHttpUrl(url: String): String {
        if (url.isEmpty()) return url
        val trimmed = url.trim()
        return if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) trimmed else "https://$trimmed"
    }

    /**
     * 反馈弹窗
     */
    private fun showFeedBackWindow(){
        mFeedBackPopupWindow.apply {
            popupGravity = Gravity.BOTTOM
            showPopupWindow()
        }
        mFeedBackPopupWindow.findViewById<ImageView>(R.id.close_iv).setOnClickListener {
            mFeedBackPopupWindow.dismiss()
        }
        val feedEt:EditText = mFeedBackPopupWindow.findViewById(R.id.feed_et)
        mFeedBackPopupWindow.findViewById<ShapeButton>(R.id.yes_btn).setOnClickListener {
            if (TextUtils.isEmpty(feedEt.text)){
                ToastUtils.showShortToast(mContext!!,getString(R.string.feedback_prompt_toast))
                return@setOnClickListener
            }
            mFeedBackPopupWindow.dismiss()
            addFeedBack(feedEt.text.toString())
        }

    }


    /**
     * 添加反馈
     */
    private fun addFeedBack(content:String){
        WaitDialog.show(getString(R.string.submitting_message))
        newsDetailModel.addFeedBack(articleId,content)
    }

    /**
     * 添加历史
     */
    private fun addHis(){
        newsDetailModel.addNewsHis(articleId)
    }

   private val easeInOutQuart: Interpolator = PathInterpolatorCompat.create(0.77f, 0f, 0.175f, 1f)

    /**
     * 展开动画
     */
    private fun expand(view: View, which: Int) {
        view.measure(
            View.MeasureSpec.makeMeasureSpec(
                (view.parent as View).width,
                View.MeasureSpec.EXACTLY
            ), View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
        )
        val targetHeight = view.measuredHeight
        view.visibility = View.VISIBLE
		// Legacy expand arrows removed in new design
        val animation: Animation = object : Animation() {
            override fun applyTransformation(interpolatedTime: Float, t: Transformation?) {
                view.layoutParams.height = if (interpolatedTime == 1f)
                    ViewGroup.LayoutParams.WRAP_CONTENT

                else (targetHeight * interpolatedTime).toInt()
                view.requestLayout()
            }

            override fun willChangeBounds(): Boolean {
                return true
            }
        }
        animation.interpolator = this.easeInOutQuart
        animation.setDuration(computeDurationFromHeight(view).toLong())
        view.startAnimation(animation)
    }

    private fun collapse(view: View, which: Int) {
        val initialHeight = view.measuredHeight
        val a: Animation = object : Animation() {
            override fun applyTransformation(interpolatedTime: Float, t: Transformation?) {
                if (interpolatedTime == 1.0f) {
					// Legacy collapse arrows removed in new design
                    view.visibility = View.GONE
                    return
                }
                val layoutParams = view.layoutParams
                layoutParams.height =
                    initialHeight - (((initialHeight.toFloat()) * interpolatedTime).toInt())
                view.requestLayout()
            }

            override fun willChangeBounds(): Boolean {
                return true
            }
        }
        a.interpolator = this.easeInOutQuart
        a.setDuration(computeDurationFromHeight(view).toLong())
        view.startAnimation(a)
    }

    private fun computeDurationFromHeight(view: View): Int {
        return ((view.measuredHeight.toFloat()) / view.context.resources.displayMetrics.density).toInt()
    }

	// Legacy lists removed

}