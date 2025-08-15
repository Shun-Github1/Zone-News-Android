package com.anssy.znewspro.selfview;

import android.graphics.Rect;

import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

/**
 * Copyright 2017, Smart Haier. All rights reserved.
 * Description:
 * Author: hanhongliang@smart-haier.com (Han Holy)
 * Date: 2017/10/23
 * ModifyBy:
 * ModifyDate:
 * ModifyDes :
 */

public class TestItemDecoration extends RecyclerView.ItemDecoration {

    private int spanCount;
    private int spacing;
    private int spanSize;


    public TestItemDecoration(int spanCount, int spanSize, int spacing) {
        this.spanCount = spanCount;
        this.spacing = spacing;
        this.spanSize = spanSize;
    }

    @Override
    public void getItemOffsets(@NonNull Rect outRect, @NonNull View view, RecyclerView parent, RecyclerView.State state) {
        int position = parent.getChildAdapterPosition(view); // item position
        int rows = position / spanCount;
        float totalItemCount = parent.getAdapter() == null ? 0.0f : parent.getAdapter().getItemCount();
        float v = totalItemCount / spanCount;
        int extraCount = (int) (totalItemCount % spanCount);
        int totalRows = (int) Math.ceil(v);
        int column = position % spanCount; // item c
        int width = parent.getMeasuredWidth();
        int childWidth = spanSize - spacing;
        Log.e("xxx","xx"+width+"\tchild:"+childWidth+"\tspanSize:"+spanSize);
        int tmp = (width - spacing * (spanCount - 1) - childWidth * spanCount) / 2;
        int extraTmp = 0;
        if (extraCount != 0) {
            extraTmp = (width - spacing * (extraCount - 1) - childWidth * extraCount) / 2;
        }
        Log.e("xxx", "xx" + tmp + "\textraTmp" + extraTmp);
        if (spanCount == 1) {
            outRect.left = tmp; //
            outRect.right = tmp; //
        } else {
            if (extraCount != 0) {
                if (column == 0) {
                    if (rows == totalRows - 1) {
                        outRect.left = extraTmp;
                        outRect.right = spacing / 2 + extraTmp;
                    } else {
                        outRect.left = tmp;
                        outRect.right = spacing / 2;
                    }

                } else if (column == (spanCount - 1)) {
                    outRect.left = spacing / 2; //
                    outRect.right = tmp;
                } else {
                    if (rows == totalRows - 1 && column == extraCount - 1) {;
                        outRect.right = extraTmp;
                    } else {
                        outRect.right = spacing / 2;
                    }
                    outRect.left = spacing / 2; //

                }
            } else {
                if (column == 0) {
                    outRect.left = tmp; //
                    outRect.right = spacing / 2;
                } else if (column == (spanCount - 1)) {
                    outRect.left = spacing / 2; //
                    outRect.right = tmp;
                } else {
                    outRect.left = spacing / 2; //
                    outRect.right = spacing / 2;
                }
            }
        }

        if (position >= spanCount) {
            outRect.top = spacing / 2; // item top
        }
    }
}
