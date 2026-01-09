package com.anssy.znewspro.selfview.popup;

import android.content.Context;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;

import com.anssy.znewspro.R;

import razerdp.basepopup.BasePopupWindow;
import razerdp.util.animation.AnimationHelper;
import razerdp.util.animation.TranslationConfig;

public class SortPopupWindow extends BasePopupWindow {

    public interface Callback {
        void onSortSelected(SortOption option);
    }

    public enum SortOption { 
        LATEST, 
        POPULAR, 
        RELEVANT 
    }

    private final Callback callback;
    private SortOption currentOption = SortOption.LATEST;

    public SortPopupWindow(Context context, Callback callback) {
        super(context);
        setContentView(R.layout.sort_popup);
        this.callback = callback;
        
        // Remove background dimming
        setBlurBackgroundEnable(false);
        
        initView();
    }

    private void initView() {
        View content = getContentView();
        
        // Set up sort options
        LinearLayout sortLatest = content.findViewById(R.id.sort_latest);
        LinearLayout sortPopular = content.findViewById(R.id.sort_popular);
        LinearLayout sortRelevant = content.findViewById(R.id.sort_relevant);

        // Set click listeners
        sortLatest.setOnClickListener(v -> {
            selectOption(SortOption.LATEST);
            if (callback != null) callback.onSortSelected(SortOption.LATEST);
            dismiss();
        });

        sortPopular.setOnClickListener(v -> {
            selectOption(SortOption.POPULAR);
            if (callback != null) callback.onSortSelected(SortOption.POPULAR);
            dismiss();
        });

        sortRelevant.setOnClickListener(v -> {
            selectOption(SortOption.RELEVANT);
            if (callback != null) callback.onSortSelected(SortOption.RELEVANT);
            dismiss();
        });

        // Set initial selection
        selectOption(currentOption);
    }

    public void setCurrentSort(SortOption option) {
        this.currentOption = option;
        selectOption(option);
    }

    private void selectOption(SortOption option) {
        View content = getContentView();
        
        // Reset all checkmarks to invisible (preserves space for alignment)
        content.findViewById(R.id.checkmark_latest).setVisibility(View.INVISIBLE);
        content.findViewById(R.id.checkmark_popular).setVisibility(View.INVISIBLE);
        content.findViewById(R.id.checkmark_relevant).setVisibility(View.INVISIBLE);
        
        // Show checkmark for selected option
        switch (option) {
            case LATEST:
                content.findViewById(R.id.checkmark_latest).setVisibility(View.VISIBLE);
                break;
            case POPULAR:
                content.findViewById(R.id.checkmark_popular).setVisibility(View.VISIBLE);
                break;
            case RELEVANT:
                content.findViewById(R.id.checkmark_relevant).setVisibility(View.VISIBLE);
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
        // Align popup's top with anchor's top (overlap)
        // Set horizontal offset to 0
        setOffsetX(0);
        
        // Calculate vertical offset to align top of popup with top of anchor
        // BasePopupWindow by default aligns bottom of popup with bottom of anchor
        // We need to move it up by the height of the anchor
        float density = anchorView.getContext().getResources().getDisplayMetrics().density;
        int anchorHeight = anchorView.getHeight();
        
        // Measure popup height
        View contentView = getContentView();
        contentView.measure(
            android.view.View.MeasureSpec.makeMeasureSpec(0, android.view.View.MeasureSpec.UNSPECIFIED),
            android.view.View.MeasureSpec.makeMeasureSpec(0, android.view.View.MeasureSpec.UNSPECIFIED)
        );
        int popupHeight = contentView.getMeasuredHeight();
        
        // Offset Y: move up by anchor height to align tops
        // Negative value moves up
        setOffsetY(-anchorHeight);
        
        // Make popup width match the anchor width
        int anchorWidth = anchorView.getWidth();
        if (anchorWidth > 0) {
            int contentWidth = contentView.getMeasuredWidth();
            
            // Set popup width to be at least as wide as the anchor
            if (contentWidth < anchorWidth) {
                android.view.ViewGroup.LayoutParams params = contentView.getLayoutParams();
                if (params != null) {
                    params.width = anchorWidth;
                    contentView.setLayoutParams(params);
                }
            }
        }
        
        super.showPopupWindow(anchorView);
    }
}
