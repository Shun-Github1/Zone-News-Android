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
        // Align popup's right edge with anchor's right edge, then add 16dp margin from screen edge
        float density = anchorView.getContext().getResources().getDisplayMetrics().density;
        int popupWidth = (int) (216 * density);
        int anchorWidth = anchorView.getWidth();
        int rightMargin = (int) (4 * density);
        
        // Move popup left so its right edge is 16dp from screen edge
        setOffsetX(anchorWidth - popupWidth - rightMargin);
        
        // Add 2dp spacing between the menu and button
        setOffsetY((int) (2 * density));
        
        super.showPopupWindow(anchorView);
    }
}
