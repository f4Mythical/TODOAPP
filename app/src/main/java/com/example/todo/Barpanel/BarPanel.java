package com.example.todo.Barpanel;

import android.animation.ObjectAnimator;
import android.content.Context;
import android.graphics.drawable.ColorDrawable;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.PopupWindow;

import com.example.todo.R;

public class BarPanel {

    public static void showNewMenu(Context context, View anchor, Runnable onPlanClick) {
        View popupView = LayoutInflater.from(context).inflate(R.layout.popup_new, null);

        PopupWindow popup = new PopupWindow(
                popupView,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                true
        );

        popup.setBackgroundDrawable(new ColorDrawable(android.graphics.Color.TRANSPARENT));
        popup.setElevation(24f);
        popup.setOutsideTouchable(true);
        popup.setAnimationStyle(R.style.PopupAnim);

        rotateFab(anchor, 45f);
        popup.setOnDismissListener(() -> rotateFab(anchor, 0f));

        LinearLayout optionPlan = popupView.findViewById(R.id.optionPlan);
        optionPlan.setOnClickListener(v -> {
            popup.dismiss();
            if (onPlanClick != null) onPlanClick.run();
        });

        popupView.measure(
                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
        );

        int popupHeight = popupView.getMeasuredHeight();
        int panelHeight = (int) (25 * context.getResources().getDisplayMetrics().density);
        int yOffset = panelHeight + popupHeight;

        popup.showAtLocation(anchor.getRootView(), Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL, 0, yOffset);
    }

    private static void rotateFab(View fab, float degrees) {
        ObjectAnimator animator = ObjectAnimator.ofFloat(fab, "rotation", fab.getRotation(), degrees);
        animator.setDuration(250);
        animator.setInterpolator(new android.view.animation.OvershootInterpolator());
        animator.start();
    }
}