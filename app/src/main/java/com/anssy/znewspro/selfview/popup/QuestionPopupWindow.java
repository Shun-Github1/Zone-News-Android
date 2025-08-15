package com.anssy.znewspro.selfview.popup;

import android.content.Context;
import android.view.animation.Animation;

import com.anssy.znewspro.R;

import razerdp.basepopup.BasePopupWindow;
import razerdp.util.animation.AnimationHelper;
import razerdp.util.animation.TranslationConfig;

/**
 * @Description TODO
 * @Author yulu
 * @CreateTime 2025年07月08日 09:27:36
 */

public class QuestionPopupWindow  extends BasePopupWindow {
    public QuestionPopupWindow(Context context) {
        super(context);
        setContentView(R.layout.ques_pop);
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
