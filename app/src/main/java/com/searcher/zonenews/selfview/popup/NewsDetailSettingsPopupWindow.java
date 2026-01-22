package com.searcher.zonenews.selfview.popup;

import android.content.Context;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;

import com.searcher.zonenews.R;

import razerdp.basepopup.BasePopupWindow;
import razerdp.util.animation.AnimationHelper;

public class NewsDetailSettingsPopupWindow extends BasePopupWindow {

    public interface Callback {
        void onOptionSelected(SettingOption option);
    }

    public enum SettingOption {
        STRAIGHTFORWARD,
        NUANCED,
        ENGLISH,
        TRADITIONAL_CHINESE,
        SIMPLIFIED_CHINESE
    }

    private final Callback callback;
    private SettingOption currentLanguageOption = SettingOption.ENGLISH;

    public NewsDetailSettingsPopupWindow(Context context, Callback callback) {
        super(context);
        setContentView(R.layout.news_detail_settings_popup);
        this.callback = callback;
        
        // Remove background dimming
        setBlurBackgroundEnable(false);
        
        initView();
    }

    public void setCurrentLanguage(SettingOption languageOption) {
        this.currentLanguageOption = languageOption;
        // Update selection if view is already initialized
        View content = getContentView();
        if (content != null) {
            updateLanguageSelection();
        }
    }

    private void initView() {
        View content = getContentView();
        
        // Set up options
        LinearLayout optionStraightforward = content.findViewById(R.id.option_straightforward);
        LinearLayout optionNuanced = content.findViewById(R.id.option_nuanced);
        LinearLayout optionEnglish = content.findViewById(R.id.option_english);
        LinearLayout optionTraditionalChinese = content.findViewById(R.id.option_traditional_chinese);
        LinearLayout optionSimplifiedChinese = content.findViewById(R.id.option_simplified_chinese);

        // Set click listeners
        optionStraightforward.setOnClickListener(v -> {
            if (callback != null) callback.onOptionSelected(SettingOption.STRAIGHTFORWARD);
            dismiss();
        });

        optionNuanced.setOnClickListener(v -> {
            if (callback != null) callback.onOptionSelected(SettingOption.NUANCED);
            dismiss();
        });

        optionEnglish.setOnClickListener(v -> {
            if (callback != null) callback.onOptionSelected(SettingOption.ENGLISH);
            dismiss();
        });

        optionTraditionalChinese.setOnClickListener(v -> {
            if (callback != null) callback.onOptionSelected(SettingOption.TRADITIONAL_CHINESE);
            dismiss();
        });

        optionSimplifiedChinese.setOnClickListener(v -> {
            if (callback != null) callback.onOptionSelected(SettingOption.SIMPLIFIED_CHINESE);
            dismiss();
        });
        
        // Update selection display
        updateLanguageSelection();
    }

    private void updateLanguageSelection() {
        View content = getContentView();
        if (content == null) return;
        
        ImageView checkEnglish = content.findViewById(R.id.check_english);
        ImageView checkTraditionalChinese = content.findViewById(R.id.check_traditional_chinese);
        ImageView checkSimplifiedChinese = content.findViewById(R.id.check_simplified_chinese);
        
        // Hide all checkmarks first
        if (checkEnglish != null) checkEnglish.setVisibility(View.GONE);
        if (checkTraditionalChinese != null) checkTraditionalChinese.setVisibility(View.GONE);
        if (checkSimplifiedChinese != null) checkSimplifiedChinese.setVisibility(View.GONE);
        
        // Show checkmark for selected language
        switch (currentLanguageOption) {
            case ENGLISH:
                if (checkEnglish != null) checkEnglish.setVisibility(View.VISIBLE);
                break;
            case TRADITIONAL_CHINESE:
                if (checkTraditionalChinese != null) checkTraditionalChinese.setVisibility(View.VISIBLE);
                break;
            case SIMPLIFIED_CHINESE:
                if (checkSimplifiedChinese != null) checkSimplifiedChinese.setVisibility(View.VISIBLE);
                break;
            default:
                break;
        }
    }

    @Override
    protected android.view.animation.Animation onCreateShowAnimation() {
        return AnimationHelper.asAnimation()
                .withAlpha(razerdp.util.animation.AlphaConfig.IN)
                .toShow();
    }

    @Override
    protected android.view.animation.Animation onCreateDismissAnimation() {
        return AnimationHelper.asAnimation()
                .withAlpha(razerdp.util.animation.AlphaConfig.OUT)
                .toDismiss();
    }

    @Override
    public void showPopupWindow(View anchorView) {
        // Update language selection before showing
        updateLanguageSelection();
        
        // Align popup's top-right corner with button's top-right corner (overlap entirely)
        // BasePopupWindow by default aligns bottom of popup with bottom of anchor
        // We need to move it up by the height of the anchor to align tops
        float density = anchorView.getContext().getResources().getDisplayMetrics().density;
        int anchorHeight = anchorView.getHeight();
        int popupWidth = (int) (240 * density);
        int anchorWidth = anchorView.getWidth();
        
        // Offset Y: move up by anchor height to align tops (overlap)
        // Negative value moves up
        setOffsetY(-anchorHeight);
        
        // Offset X: align right edges by moving left by (popupWidth - anchorWidth)
        // If popup is wider than anchor, move left. If anchor is wider, move right.
        setOffsetX(anchorWidth - popupWidth);
        
        super.showPopupWindow(anchorView);
    }
}
