package com.anssy.znewspro.ui.mainfrag

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.exceptions.ClearCredentialException
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.anssy.znewspro.utils.Constants
import com.anssy.znewspro.R
import com.anssy.znewspro.base.BaseFragment
import com.anssy.znewspro.databinding.FragMyBinding
import com.anssy.znewspro.model.LoginModel
import com.anssy.znewspro.model.MyModel
import com.anssy.znewspro.ui.MainActivity
import com.anssy.znewspro.ui.collect.MyCollectListActivity
import com.anssy.znewspro.ui.his.BrownHisActivity
import com.anssy.znewspro.ui.login.LoginActivity
import com.anssy.znewspro.ui.login.LoginActivity.Companion.TAG
import com.anssy.znewspro.ui.about.AboutActivity
import com.anssy.znewspro.selfview.popup.FeedBackPopupWindow
import android.widget.EditText
import com.hjq.shape.view.ShapeButton
import android.text.TextUtils
import android.view.Gravity
import android.widget.ImageView
import com.anssy.znewspro.utils.SharedPreferenceUtils
import com.anssy.znewspro.utils.ToastUtils
import com.anssy.znewspro.utils.Utils
import com.bumptech.glide.Glide
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.kongzue.dialogx.dialogs.MessageDialog
import kotlinx.coroutines.launch
import razerdp.util.PopupUtils.getString

/**
 * @Description 我的
 * @Author yulu
 * @CreateTime 2025年06月30日 09:28:07
 */

class MyFrag : BaseFragment() {
    private lateinit var  mViewBinding:FragMyBinding
    private val loginModel:LoginModel by viewModels()
    private val myModel:MyModel by viewModels()
    private lateinit var auth: FirebaseAuth
    private lateinit var credentialManager: CredentialManager
    companion object{
        fun  getInstance():MyFrag{
            return MyFrag()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        mViewBinding = FragMyBinding.inflate(layoutInflater)
        return mViewBinding.root
    }

    @SuppressLint("SetTextI18n")
    override fun initData() {
        auth = Firebase.auth
        credentialManager = CredentialManager.Companion.create(mContext!!)
        // Header simplified per new design
        mViewBinding.hisLayout.setOnClickListener {
            val intent = Intent(mContext,BrownHisActivity::class.java)
            startActivity(intent)
        }
        mViewBinding.collectLayout.setOnClickListener {
            val intent = Intent(mContext,MyCollectListActivity::class.java)
            startActivity(intent)
        }

        mViewBinding.aboutLayout.setOnClickListener {
            val intent = Intent(mContext, AboutActivity::class.java)
            startActivity(intent)
        }
        mViewBinding.languageLayout.setOnClickListener {
            (requireActivity() as MainActivity).showSettingPop(it)
        }
        mViewBinding.feedbackLayout.setOnClickListener { showFeedbackPopup() }
        iniModel()
        mViewBinding.logoutLayout.setOnClickListener {
            MessageDialog.show(getString(R.string.dialog_title_reminder),getString(R.string.logout_confirmation_message),getString(R.string.confirm),getString(R.string.dialog_button_cancel))
                .setOkButtonClickListener { dialog, v ->
                    dialog.dismiss()
                    loginModel.outLoginApp()
                    true
                }
                .setCancelButtonClickListener { dialog, v ->
                    dialog.dismiss()
                    true
                }
        }
    }



    @SuppressLint("SetTextI18n")
    private fun iniModel(){
        myModel.queryMyFormation()
        myModel.myEntry.observe(viewLifecycleOwner){
            if (it!=null){
                if (it.code== Constants.SUCCESS_CODE){
                    Glide.with(mContext!!).load(it.data.profileIcon).error(R.drawable.ease_default_image)
                        .into(mViewBinding.avatarIv)
                    // No name/email in current API; show ID as name and leave email blank for now
                    mViewBinding.userNameTv.text = "${getString(R.string.user_id_label)}${it.data.profileID}"
                    mViewBinding.userEmailTv.text = ""
                }else{
                    if (it.code==1000){
                        ToastUtils.showShortToast(mContext!!,getString(R.string.server_error_message))
                    }else{
                        ToastUtils.showShortToast(mContext!!,it.msg)
                    }
                }
            }
        }
        // Old web-based About page removed; no network observer needed
        loginModel.outLoginEntry.observe(viewLifecycleOwner){
            SharedPreferenceUtils.clear(mContext)
            SharedPreferenceUtils.saveBoolean(mContext,"isLogin",false)
            if (SharedPreferenceUtils.getBoolean(mContext,"thirdLogin")){
                signOut()
            }
            val intent =  Intent(mContext,LoginActivity::class.java)
            startActivity(intent)
            requireActivity().finishAffinity()
        }
    }

    private fun signOut() {
        // Firebase sign out
        auth.signOut()

        lifecycleScope.launch {
            try {
                val clearRequest = ClearCredentialStateRequest()
                credentialManager.clearCredentialState(clearRequest)
            } catch (e: ClearCredentialException) {
                Log.e(TAG, "Couldn't clear user credentials: ${e.localizedMessage}")
            }
        }

    }

    private fun showFeedbackPopup() {
        val popup = FeedBackPopupWindow(requireContext())
        popup.apply {
            popupGravity = Gravity.BOTTOM
            showPopupWindow()
        }
        popup.findViewById<ImageView>(R.id.close_iv).setOnClickListener { popup.dismiss() }
        val feedEt: EditText = popup.findViewById(R.id.feed_et)
        popup.findViewById<ShapeButton>(R.id.yes_btn).setOnClickListener {
            if (TextUtils.isEmpty(feedEt.text)) {
                ToastUtils.showShortToast(mContext!!, getString(R.string.feedback_prompt_toast))
                return@setOnClickListener
            }
            popup.dismiss()
            ToastUtils.showShortToast(mContext!!, getString(R.string.success_message))
        }
    }
}
