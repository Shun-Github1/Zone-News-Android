package com.anssy.znewspro.ui.his

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.anssy.znewspro.utils.Constants
import com.anssy.znewspro.R
import com.anssy.znewspro.base.BaseActivity
import com.anssy.znewspro.databinding.ActivityBrownHisBinding
import com.anssy.znewspro.entry.ViewHisEntry
import com.anssy.znewspro.model.MyModel
import com.anssy.znewspro.ui.newsdetail.NewsDetailActivity
import com.anssy.znewspro.utils.ToastUtils
import com.anssy.znewspro.utils.glide.GlideApp
import com.jaeger.library.StatusBarUtil
import com.zhy.adapter.recyclerview.CommonAdapter
import com.zhy.adapter.recyclerview.base.ViewHolder

/**
 * @Description 浏览历史
 * @Author yulu
 * @CreateTime 2025年07月03日 16:53:39
 */

class BrownHisActivity:BaseActivity() {
    private lateinit var mViewBinding:ActivityBrownHisBinding
    private val myModel:MyModel by viewModels()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mViewBinding = ActivityBrownHisBinding.inflate(layoutInflater)
        setContentView(mViewBinding.root)
        applyStatusBarStyle()
        initView()
        initModel()
    }
    private var mList = ArrayList<ViewHisEntry.DataDTO.ArticlesDTO>()
    private lateinit var mAdapter:CommonAdapter<ViewHisEntry.DataDTO.ArticlesDTO>
    private fun initView() {
        mViewBinding.topLayout.titleTv.text = "${getString(R.string.history)}"
        mViewBinding.hisRecycler.layoutManager = LinearLayoutManager(mContext,RecyclerView.VERTICAL,false)
        mAdapter = object : CommonAdapter<ViewHisEntry.DataDTO.ArticlesDTO>(this, R.layout.item_brown_his,mList){
            override fun convert(holder: ViewHolder, t: ViewHisEntry.DataDTO.ArticlesDTO, position: Int) {
                    val newsIv:ImageView = holder.getView(R.id.news_iv)
                    val titleTv:TextView = holder.getView(R.id.news_title_tv)
                    val timeTv:TextView = holder.getView(R.id.news_time_tv)
                    GlideApp.with(mContext).load(t.pictureURL).error(R.drawable.ease_default_image).into(newsIv)
                    titleTv.text = t.title
                    timeTv.text = t.date
                    holder.convertView.setOnClickListener {
                        val intent = Intent(mContext,NewsDetailActivity::class.java)
                        intent.putExtra("id",t.articleID)
                        startActivity(intent)
                    }
            }

        }
        mViewBinding.hisRecycler.adapter = mAdapter
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun initModel(){
        myModel.queryMyViewHis()
        myModel.myViewHisEntry.observe(this){
            if (it!=null){
                if (it.code== Constants.SUCCESS_CODE){
                    mList.clear()
                    mList.addAll(it.data.articles)
                    mAdapter.notifyDataSetChanged()
                    if (mList.isEmpty()){
                        mViewBinding.noDataLayout.root.visibility = View.VISIBLE
                    }else{
                        mViewBinding.noDataLayout.root.visibility = View.GONE
                    }
                }else{
                    if (it.code==1000){
                        ToastUtils.showShortToast(mContext!!,getString(R.string.server_error_message))
                    }else{
                        ToastUtils.showShortToast(mContext!!,it.msg)
                    }
                }
            }
        }
    }
}