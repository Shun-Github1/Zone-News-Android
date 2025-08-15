package com.anssy.znewspro.ui

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.KeyEvent
import android.view.ViewGroup
import android.view.View
import android.widget.RadioButton
import android.widget.RadioGroup
import com.google.android.material.card.MaterialCardView
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import com.anssy.znewspro.R
import com.anssy.znewspro.base.BaseActivity
import com.anssy.znewspro.databinding.ActivityMainBinding
import com.anssy.znewspro.entry.MainSettingEntry
import com.anssy.znewspro.selfview.popup.SettingPopupWindow
import com.anssy.znewspro.ui.mainfrag.AdviceFrag
import com.anssy.znewspro.ui.mainfrag.HomeFrag
import com.anssy.znewspro.ui.mainfrag.MyFrag
import com.anssy.znewspro.ui.mainfrag.SearchFrag
import com.anssy.znewspro.utils.KLog
import com.anssy.znewspro.utils.MVUtils
import com.anssy.znewspro.utils.SharedPreferenceUtils
import com.anssy.znewspro.utils.ToastUtils
import com.anssy.znewspro.utils.WeakHandler
import com.hjq.language.LocaleContract
import com.hjq.language.MultiLanguages
import com.hjq.shape.view.ShapeButton
import com.jaeger.library.StatusBarUtil
import razerdp.basepopup.BasePopupWindow


class MainActivity : BaseActivity() {
    private lateinit var mViewBinding: ActivityMainBinding
    private var fragmentList: ArrayList<Fragment> = ArrayList()
    private var isExit: Boolean = false
    private val mHandler: WeakHandler = WeakHandler(Handler.Callback { msg ->
        if (msg.what != 0) {
            return@Callback true
        }
        isExit = false
        true
    })
    var mBottomView: MaterialCardView? = null
    private var isBottomBarHidden: Boolean = false
    private val autoShowRunnable = Runnable { showBottomBar() }
    var lastFragment: Fragment? = null
    private var homeFrag: HomeFrag? = null
    private var adviceFrag: AdviceFrag? = null
    private var searchFrag: SearchFrag? = null
    private var myFrag: MyFrag? = null
    private var mFragmentMgr: FragmentManager? = null
    private lateinit var mSettingPopupWindow: SettingPopupWindow
    override fun onCreate(savedInstanceState: Bundle?) {
        // Force the app theme before any view creation
        setTheme(R.style.AppTheme)
        super.onCreate(savedInstanceState)
        mViewBinding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(mViewBinding.root)
        // Consistent status bar style
        applyStatusBarStyle()
        initView()
        initClick()
        addEventListener()
    }

    private fun initView() {
        mBottomView = mViewBinding.mainBottomCard
        mFragmentMgr = this.supportFragmentManager
        homeFrag = HomeFrag.getInstance()
        fragmentList.add(homeFrag!!)
        adviceFrag = AdviceFrag.getInstance()
        fragmentList.add(adviceFrag!!)
        myFrag = MyFrag.getInstance()
        fragmentList.add(myFrag!!)
        searchFrag = SearchFrag.getInstance()
        fragmentList.add(searchFrag!!)
        lastFragment = homeFrag
        mSettingPopupWindow = SettingPopupWindow(this)
        mSettingPopupWindow.setOnDismissListener(object : BasePopupWindow.OnDismissListener() {
            override fun onDismiss() {
                Log.e("xxx", "设置弹框消失");
            }

        })
    }

    fun showSettingPop(view: View) {
        mSettingPopupWindow
            .setAlignBackground(true)
            .setOffsetY(10)
            .showPopupWindow(view)
    }

    private fun initClick() {
        mViewBinding.mainRg.check(R.id.home_rb)
        this.mFragmentMgr?.beginTransaction()?.add(
            R.id.main_container,
            fragmentList[0], "A"
        )?.commit()
    }

    private fun setFragment(fragment: Fragment, tag: String?) {
        if (!fragment.isAdded && null == mFragmentMgr!!.findFragmentByTag(tag)) {
            mFragmentMgr!!.beginTransaction().hide(lastFragment!!)
                .add(R.id.main_container, fragment, tag).commitAllowingStateLoss()
        } else {
            mFragmentMgr!!.beginTransaction().hide(lastFragment!!).show(fragment)
                .commitAllowingStateLoss()
        }
        this.lastFragment = fragment
    }

    fun hideBottomBar() {
        val bar = mBottomView ?: return
        if (isBottomBarHidden) return
        bar.clearAnimation()
        cancelBottomBarAutoShow()
        val params = bar.layoutParams
        val bottomMargin = if (params is ViewGroup.MarginLayoutParams) params.bottomMargin else 0
        val translationDistance = (bar.height + bottomMargin).toFloat()
        bar.animate().cancel()
        bar.animate()
            .translationY(translationDistance)
            .alpha(0f)
            .setDuration(200)
            .start()
        isBottomBarHidden = true
    }

    fun showBottomBar() {
        val bar = mBottomView ?: return
        if (!isBottomBarHidden) return
        bar.clearAnimation()
        bar.animate().cancel()
        bar.animate()
            .translationY(0f)
            .alpha(1f)
            .setDuration(200)
            .start()
        isBottomBarHidden = false
    }

    fun scheduleBottomBarAutoShow(delayMs: Long = 2500) {
        val bar = mBottomView ?: return
        bar.removeCallbacks(autoShowRunnable)
        bar.postDelayed(autoShowRunnable, delayMs)
    }

    fun cancelBottomBarAutoShow() {
        mBottomView?.removeCallbacks(autoShowRunnable)
    }

    private fun addEventListener() {
        mViewBinding.mainRg.setOnCheckedChangeListener { _, checkedId ->
            when (checkedId) {
                R.id.my_rb -> {
                    setFragment(fragmentList[2], "E")
                }

                R.id.search_rb -> {
                    setFragment(fragmentList[3], "C")
                }

                R.id.special_rb -> {
                    setFragment(fragmentList[1], "B")
                }

                else -> {
                    setFragment(fragmentList[0], "A")
                }
            }
        }
    }


    // androidx.appcompat.app.AppCompatActivity
    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode != 4) {
            return super.onKeyDown(keyCode, event)
        }
        exit()
        return false
    }

    private fun exit() {
        if (!this.isExit) {
            this.isExit = true
            ToastUtils.showShortToast(this, "再按一次，退出App")
            mHandler.sendEmptyMessageDelayed(0, 2000)
            return
        }
        mHandler.postDelayed({
            finishAffinity()
        }, 1000)
    }

}