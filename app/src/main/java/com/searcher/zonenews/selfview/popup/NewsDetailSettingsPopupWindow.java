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

        // Prevent BasePopupWindow from constraining popup height to available space
        setFitSize(false);

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
            if (callback != null)
                callback.onOptionSelected(SettingOption.STRAIGHTFORWARD);
            dismiss();
        });

        optionNuanced.setOnClickListener(v -> {
            if (callback != null)
                callback.onOptionSelected(SettingOption.NUANCED);
            dismiss();
        });

        optionEnglish.setOnClickListener(v -> {
            if (callback != null)
                callback.onOptionSelected(SettingOption.ENGLISH);
            dismiss();
        });

        optionTraditionalChinese.setOnClickListener(v -> {
            if (callback != null)
                callback.onOptionSelected(SettingOption.TRADITIONAL_CHINESE);
            dismiss();
        });

        optionSimplifiedChinese.setOnClickListener(v -> {
            if (callback != null)
                callback.onOptionSelected(SettingOption.SIMPLIFIED_CHINESE);
            dismiss();
        });

        // Update selection display
        updateLanguageSelection();
    }

    private void updateLanguageSelection() {
        View content = getContentView();
        if (content == null)
            return;

        ImageView checkEnglish = content.findViewById(R.id.check_english);
        ImageView checkTraditionalChinese = content.findViewById(R.id.check_traditional_chinese);
        ImageView checkSimplifiedChinese = content.findViewById(R.id.check_simplified_chinese);

        // Hide all checkmarks first
        if (checkEnglish != null)
            checkEnglish.setVisibility(View.GONE);
        if (checkTraditionalChinese != null)
            checkTraditionalChinese.setVisibility(View.GONE);
        if (checkSimplifiedChinese != null)
            checkSimplifiedChinese.setVisibility(View.GONE);

        // Show checkmark for selected language
        switch (currentLanguageOption) {
            case ENGLISH:
                if (checkEnglish != null)
                    checkEnglish.setVisibility(View.VISIBLE);
                break;
            case TRADITIONAL_CHINESE:
                if (checkTraditionalChinese != null)
                    checkTraditionalChinese.setVisibility(View.VISIBLE);
                break;
            case SIMPLIFIED_CHINESE:
                if (checkSimplifiedChinese != null)
                    checkSimplifiedChinese.setVisibility(View.VISIBLE);
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

        float density = anchorView.getContext().getResources().getDisplayMetrics().density;
        int anchorHeight = anchorView.getHeight();
        int popupWidth = (int) (240 * density);
        int anchorWidth = anchorView.getWidth();

        // Align right edges
        setOffsetX(anchorWidth - popupWidth);

        // Get anchor position on screen
        int[] anchorLocation = new int[2];
        anchorView.getLocationOnScreen(anchorLocation);
        int anchorTop = anchorLocation[1];
        int anchorBottom = anchorTop + anchorHeight;

        // Get screen height
        int screenHeight = anchorView.getContext().getResources().getDisplayMetrics().heightPixels;
        int screenMidpoint = screenHeight / 2;

        // Measure popup height
        View contentView = getContentView();
        contentView.measure(
                android.view.View.MeasureSpec.makeMeasureSpec(popupWidth, android.view.View.MeasureSpec.AT_MOST),
                android.view.View.MeasureSpec.makeMeasureSpec(0, android.view.View.MeasureSpec.UNSPECIFIED));
        int popupHeight = contentView.getMeasuredHeight();

        if (anchorBottom > screenMidpoint) {
            // Button is below midpoint: align popup bottom with button bottom (overlap)
            // BasePopupWindow default: popup top at anchor bottom
            // Move up by popupHeight so popup bottom = anchor bottom
            setOffsetY(-popupHeight);
        } else {
            // Button is above midpoint: align popup top with button top
            setOffsetY(-anchorHeight);
        }

        super.showPopupWindow(anchorView);
    }
}
