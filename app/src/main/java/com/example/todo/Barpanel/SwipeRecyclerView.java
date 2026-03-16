package com.example.todo.Barpanel;

import android.content.Context;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.MotionEvent;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

public class SwipeRecyclerView extends RecyclerView {

    public interface OnSwipeListener {
        void onSwipeLeft();
        void onSwipeRight();
    }

    private GestureDetector gestureDetector;
    private OnSwipeListener swipeListener;

    public SwipeRecyclerView(@NonNull Context context) {
        super(context); init(context);
    }

    public SwipeRecyclerView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs); init(context);
    }

    public SwipeRecyclerView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle); init(context);
    }

    private void init(Context context) {
        gestureDetector = new GestureDetector(context, new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onFling(MotionEvent e1, MotionEvent e2, float vX, float vY) {
                if (e1 == null || e2 == null || swipeListener == null) return false;
                float dX = e2.getX() - e1.getX();
                float dY = e2.getY() - e1.getY();
                if (Math.abs(dY) > 200) return false;
                if (Math.abs(dX) > 80) {
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
    public boolean onTouchEvent(MotionEvent e) {
        gestureDetector.onTouchEvent(e);
        return super.onTouchEvent(e);
    }
}