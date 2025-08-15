package com.anssy.znewspro.selfview.popup;

import android.content.Context;
import android.view.animation.Animation;

import com.anssy.znewspro.R;

import razerdp.basepopup.BasePopupWindow;
import razerdp.util.animation.AnimationHelper;
import razerdp.util.animation.TranslationConfig;

/**
 * @Description 反馈
 * @Author yulu
 * @CreateTime 2025年07月05日 09:38:00
 */

public class FeedBackPopupWindow extends BasePopupWindow {
    public FeedBackPopupWindow(Context context) {
        super(context);
        setContentView(R.layout.feedback_popu);
    }
    @Override
    protected Animation onCreateShowAnimation() {

        return AnimationHelper.asAnimation().withTranslation(TranslationConfig.FROM_BOTTOM).toShow();
    }

    @Override
    protected Animation onCreateDismissAnimation() {
        return AnimationHelper.asAnimation().withTranslation(TranslationConfig.TO_BOTTOM).toDismiss();
    }
}
