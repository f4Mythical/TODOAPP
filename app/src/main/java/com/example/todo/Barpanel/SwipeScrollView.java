package com.example.todo.Barpanel;

import android.content.Context;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.widget.ScrollView;

public class SwipeScrollView extends ScrollView {

    public interface OnSwipeListener {
        void onSwipeLeft();
        void onSwipeRight();
    }

    private GestureDetector gestureDetector;
    private OnSwipeListener swipeListener;

    public SwipeScrollView(Context context) {
        super(context);
        init(context);
    }

    public SwipeScrollView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public SwipeScrollView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(context);
    }

    private void init(Context context) {
        gestureDetector = new GestureDetector(context, new GestureDetector.SimpleOnGestureListener() {
            private static final int SWIPE_MIN_DISTANCE = 80;
            private static final int SWIPE_MAX_OFF_PATH = 250;

            @Override
            public boolean onFling(MotionEvent e1, MotionEvent e2, float vX, float vY) {
                if (e1 == null || e2 == null || swipeListener == null) return false;
                float dX = e2.getX() - e1.getX();
                float dY = e2.getY() - e1.getY();
                if (Math.abs(dY) > SWIPE_MAX_OFF_PATH) return false;
                if (Math.abs(dX) > SWIPE_MIN_DISTANCE) {
                    if (dX < 0) swipeListener.onSwipeLeft();
                    else swipeListener.onSwipeRight();
                    return true;
                }
                return false;
            }
        });
    }

    public void setOnSwipeListener(OnSwipeListener listener) {
        this.swipeListener = listener;
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        gestureDetector.onTouchEvent(ev);
        return super.onTouchEvent(ev);
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        gestureDetector.onTouchEvent(ev);
        return super.onInterceptTouchEvent(ev);
    }
}