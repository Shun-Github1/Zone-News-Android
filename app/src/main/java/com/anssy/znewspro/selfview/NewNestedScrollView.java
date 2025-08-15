package com.anssy.znewspro.selfview;

import android.content.Context;
import android.os.Handler;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.widget.NestedScrollView;

import java.util.logging.Logger;


public class NewNestedScrollView extends NestedScrollView implements NestedScrollView.OnScrollChangeListener {


    /**
     *
     */
    private AddScrollChangeListener addScrollChangeListener;


    /**
     * 滚动状态
     */
    public enum ScrollState{
        DRAG,      // 拖拽中
        SCROLLING, // 正在滚动
        IDLE       // 已停止
    }

    /**
     *
     * 记录上一次滑动
     *
     */
    private int lastScrollY ;
    /**
     */
    private boolean isCheckingIdle = false ;
    /**
     * 上一次记录的时间
     */
    private long lastTime ;


    private Handler handler;
    /**
     * 整個滾動内容高度
     *
     */
    public int totalHeight = 0 ;

    /**
     *
     * 当前view的高度
     *
     */
    public int viewHeight = 0 ;

    /**
     * 是否滚动到底了
     */
    private boolean bottom = false;

    /**
     * 是否滚动在顶部
     *
     * @param context
     */
    private boolean top = false ;


    public NewNestedScrollView(@NonNull Context context) {
        this(context,null);
    }

    public NewNestedScrollView(@NonNull Context context, @Nullable AttributeSet attrs) {
        this(context, attrs,0);
    }

    public NewNestedScrollView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        setOnScrollChangeListener(this);
        handler = new Handler(context.getMainLooper());
    }


    @Override
    public void onScrollChange(NestedScrollView v, int scrollX, int scrollY, int oldScrollX, int oldScrollY) {
        /*实时滚动回调*/
        if (addScrollChangeListener!=null){
            addScrollChangeListener.onScrollChange( scrollX,  scrollY,  oldScrollX,  oldScrollY);
        }

        if (totalHeight>viewHeight && (totalHeight - viewHeight) == scrollY){
           Log.e("xxx","[NewNestedScrollView]->onScrollChange = bottom");
            bottom = true ;
        }else {
            bottom = false ;
        }

        if (getScrollY()<=0){
            Log.e("xxx","[NewNestedScrollView]->onScrollChange = top");
            top = true ;
        }else {
            top = false ;
        }
    }



    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        totalHeight = 0 ;
        int count = getChildCount();
        for (int i =0 ;i < count ;i++){
            View view = getChildAt(i);
            totalHeight += view.getMeasuredHeight();
        }
        viewHeight = getHeight() ;
    }




    /**
     * 是否动到底
     * @return
     */
    public boolean isBottom(){
        return bottom;
    }



    /**
     * 是否滚动到了 顶部
     *
     * @return
     */
    public boolean isTop(){
        return top;
    }



    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        switch (ev.getAction()){
            case MotionEvent.ACTION_DOWN:
            case MotionEvent.ACTION_MOVE:
                cancelIdleCheck();
                Log.e("xxx","[NewNestedScrollView]->DRAG 拖拽中");
                if (addScrollChangeListener!=null){
                    addScrollChangeListener.onScrollState(ScrollState.DRAG);
                }
                break;

            case MotionEvent.ACTION_CANCEL:
            case MotionEvent.ACTION_OUTSIDE:
            case MotionEvent.ACTION_UP:
                scheduleIdleCheck();
                break;
        }
        return super.onTouchEvent(ev);
    }


    private final Runnable idleCheckRunnable = new Runnable() {
        @Override
        public void run() {
            if (!isCheckingIdle) return;
            final int newScrollY = getScrollY();
            if (newScrollY == lastScrollY) {
                isCheckingIdle = false;
                Log.e("xxx","[NewNestedScrollView]->IDLE 停止滚动");
                if (addScrollChangeListener != null) {
                    addScrollChangeListener.onScrollState(ScrollState.IDLE);
                }
            } else {
                lastScrollY = newScrollY;
                Log.e("xxx","[NewNestedScrollView]->SCROLLING 正在滚动中");
                if (addScrollChangeListener != null) {
                    addScrollChangeListener.onScrollState(ScrollState.SCROLLING);
                }
                handler.postDelayed(this, 60);
            }
        }
    };

    private void scheduleIdleCheck() {
        lastScrollY = getScrollY();
        isCheckingIdle = true;
        handler.removeCallbacks(idleCheckRunnable);
        handler.postDelayed(idleCheckRunnable, 60);
    }

    private void cancelIdleCheck() {
        isCheckingIdle = false;
        handler.removeCallbacks(idleCheckRunnable);
    }



    /**
     * 设置监听
     *
     * @param addScrollChangeListener
     * @return
     */
    public NewNestedScrollView addScrollChangeListener(AddScrollChangeListener addScrollChangeListener) {
        this.addScrollChangeListener =  addScrollChangeListener;
        return this ;
    }


    public interface AddScrollChangeListener{
        /**
         * 滚动监听
         * @param scrollX
         * @param scrollY
         * @param oldScrollX
         * @param oldScrollY
         */
        void onScrollChange( int scrollX, int scrollY, int oldScrollX, int oldScrollY);


        /**
         * 滚动状态
         *
         * @param state
         */
        void onScrollState(ScrollState state);
    }
}

