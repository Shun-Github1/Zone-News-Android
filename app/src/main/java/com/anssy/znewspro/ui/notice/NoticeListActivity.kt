package com.anssy.znewspro.ui.notice

import android.annotation.SuppressLint
import android.os.Bundle
import com.anssy.znewspro.R
import com.anssy.znewspro.base.BaseActivity
import com.anssy.znewspro.databinding.ActivityNoticeListBinding
import com.jaeger.library.StatusBarUtil

/**
 * @Description 通知
 * @Author yulu
 * @CreateTime 2025年07月04日 10:31:57
 */

class NoticeListActivity:BaseActivity() {
    private lateinit var mViewBinding: ActivityNoticeListBinding
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mViewBinding = ActivityNoticeListBinding.inflate(layoutInflater)
        setContentView(mViewBinding.root)
        applyStatusBarStyle()
        initView()
    }
    
    @SuppressLint("SetTextI18n")
    private fun initView() {
        // Setup MaterialToolbar with navigation click listener
        setupToolbar()
    }

    private fun setupToolbar() {
        val toolbar = mViewBinding.toolbar
        toolbar.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.action_close -> {
                    finish()
                    true
                }
                else -> false
            }
        }
    }
}