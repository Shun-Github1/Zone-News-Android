package com.anssy.znewspro.selfview.popup;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;

import com.anssy.znewspro.R;

import razerdp.basepopup.BasePopupWindow;

public class PublisherArticlesSortPopupWindow extends BasePopupWindow {
    
    public enum SortOption {
        PUBLISHER_NAME,
        MEDIA_SIGNIFICANCE,
        PUBLISHER_BIAS,
        PUBLISHER_REGION,
        ARTICLE_TITLE
    }
    
    public interface Callback {
        void onSortSelected(SortOption option);
    }
    
    private Callback callback;
    private SortOption currentOption = SortOption.PUBLISHER_NAME;
    private boolean isAscending = true;
    
    public PublisherArticlesSortPopupWindow(Context context, Callback callback) {
        super(context);
        this.callback = callback;
        
        setContentView(R.layout.publisher_articles_sort_popup);
        setBlurBackgroundEnable(false);
        
        initView();
    }
    
    private void initView() {
        View content = getContentView();
        // Set up sort options
        LinearLayout sortPublisherName = content.findViewById(R.id.sort_publisher_name);
        LinearLayout sortMediaSignificance = content.findViewById(R.id.sort_media_significance);
        LinearLayout sortPublisherBias = content.findViewById(R.id.sort_publisher_bias);
        LinearLayout sortPublisherRegion = content.findViewById(R.id.sort_publisher_region);
        LinearLayout sortArticleTitle = content.findViewById(R.id.sort_article_title);
        // Set click listeners
        sortPublisherName.setOnClickListener(v -> {
            selectOption(SortOption.PUBLISHER_NAME);
            if (callback != null) callback.onSortSelected(SortOption.PUBLISHER_NAME);
            dismiss();
        });
        
        sortMediaSignificance.setOnClickListener(v -> {
            selectOption(SortOption.MEDIA_SIGNIFICANCE);
            if (callback != null) callback.onSortSelected(SortOption.MEDIA_SIGNIFICANCE);
            dismiss();
        });
        
        sortPublisherBias.setOnClickListener(v -> {
            selectOption(SortOption.PUBLISHER_BIAS);
            if (callback != null) callback.onSortSelected(SortOption.PUBLISHER_BIAS);
            dismiss();
        });
        
        sortPublisherRegion.setOnClickListener(v -> {
            selectOption(SortOption.PUBLISHER_REGION);
            if (callback != null) callback.onSortSelected(SortOption.PUBLISHER_REGION);
            dismiss();
        });
        
        sortArticleTitle.setOnClickListener(v -> {
            selectOption(SortOption.ARTICLE_TITLE);
            if (callback != null) callback.onSortSelected(SortOption.ARTICLE_TITLE);
            dismiss();
        });
        
        // Set initial selection
        selectOption(currentOption);
    }
    
    public void setCurrentSort(SortOption option) {
        this.currentOption = option;
        selectOption(option);
    }
    
    public void setCurrentSort(SortOption option, boolean ascending) {
        this.currentOption = option;
        this.isAscending = ascending;
        selectOption(option);
    }
    
    public SortOption getCurrentSort() {
        return currentOption;
    }
    
    public boolean isAscending() {
        return isAscending;
    }
    
    private void selectOption(SortOption option) {
        View content = getContentView();
        if (content == null) return;
        
        // Hide all checkmarks
        content.findViewById(R.id.check_publisher_name).setVisibility(View.GONE);
        content.findViewById(R.id.check_media_significance).setVisibility(View.GONE);
        content.findViewById(R.id.check_publisher_bias).setVisibility(View.GONE);
        content.findViewById(R.id.check_publisher_region).setVisibility(View.GONE);
        content.findViewById(R.id.check_article_title).setVisibility(View.GONE);
        
        // Show checkmark for selected option
        switch (option) {
            case PUBLISHER_NAME:
                content.findViewById(R.id.check_publisher_name).setVisibility(View.VISIBLE);
                break;
            case MEDIA_SIGNIFICANCE:
                content.findViewById(R.id.check_media_significance).setVisibility(View.VISIBLE);
                break;
            case PUBLISHER_BIAS:
                content.findViewById(R.id.check_publisher_bias).setVisibility(View.VISIBLE);
                break;
            case PUBLISHER_REGION:
                content.findViewById(R.id.check_publisher_region).setVisibility(View.VISIBLE);
                break;
            case ARTICLE_TITLE:
                content.findViewById(R.id.check_article_title).setVisibility(View.VISIBLE);
                break;
        }
    }
    
    
    @Override
    public void showPopupWindow(View anchorView) {
        // Align popup's right edge 16dp from screen edge
        float density = anchorView.getContext().getResources().getDisplayMetrics().density;
        int popupWidth = (int) (240 * density);
        int anchorWidth = anchorView.getWidth();
        int rightMargin = (int) (-64 * density); // 16dp margin from screen edge
        
        // Move popup so its right edge is 16dp from screen edge
        setOffsetX(anchorWidth - popupWidth - rightMargin);
        
        // Add 2dp spacing between the menu and button
        setOffsetY((int) (2 * density));
        
        super.showPopupWindow(anchorView);
    }
}
