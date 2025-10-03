package com.anssy.znewspro.selfview.popup;

import android.content.Context;
import android.view.View;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;

import com.anssy.znewspro.R;

import razerdp.basepopup.BasePopupWindow;
import razerdp.util.animation.AnimationHelper;
import razerdp.util.animation.TranslationConfig;

public class PersonalizationPopupWindow extends BasePopupWindow {

    public interface Callback {
        void onSortSelected(SortOption option);
        void onManageTopics();
    }

    public enum SortOption { DEFAULT, SIG_DESC, SIG_ASC, LATEST, EARLIEST }

    private final Callback callback;

    public PersonalizationPopupWindow(Context context, Callback callback) {
        super(context);
        setContentView(R.layout.personalization_popup);
        this.callback = callback;
        initView();
    }

    private void initView() {
        View content = getContentView();
        content.findViewById(R.id.close_iv).setOnClickListener(v -> dismiss());
        RadioGroup sortRg = content.findViewById(R.id.sort_rg);
        sortRg.setOnCheckedChangeListener((group, checkedId) -> {
            if (callback == null) return;
            SortOption option = SortOption.DEFAULT;
            if (checkedId == R.id.sort_significance_desc) option = SortOption.SIG_DESC;
            else if (checkedId == R.id.sort_significance_asc) option = SortOption.SIG_ASC;
            else if (checkedId == R.id.sort_latest) option = SortOption.LATEST;
            else if (checkedId == R.id.sort_earliest) option = SortOption.EARLIEST;
            callback.onSortSelected(option);
        });

        View manage = content.findViewById(R.id.manage_topics_tv);
        manage.setOnClickListener(v -> { if (callback != null) callback.onManageTopics(); });
    }

    public void setTopicCount(int count) {
        TextView tv = getContentView().findViewById(R.id.topic_count_tv);
        if (tv != null) tv.setText(String.valueOf(count));
    }

    @Override
    protected android.view.animation.Animation onCreateShowAnimation() {
        return AnimationHelper.asAnimation().withTranslation(TranslationConfig.FROM_BOTTOM).toShow();
    }

    @Override
    protected android.view.animation.Animation onCreateDismissAnimation() {
        return AnimationHelper.asAnimation().withTranslation(TranslationConfig.TO_BOTTOM).toDismiss();
    }
}


