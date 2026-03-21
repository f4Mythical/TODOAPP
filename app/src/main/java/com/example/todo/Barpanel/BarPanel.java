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

import androidx.fragment.app.FragmentActivity;

import com.example.todo.R;

import java.lang.ref.WeakReference;

public class BarPanel {

    public static void showNewMenu(Context context, View anchor, Runnable onPlanClick) {
        if (anchor == null || anchor.getWindowToken() == null) return;

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

        WeakReference<View> anchorRef = new WeakReference<>(anchor);
        ObjectAnimator rotateIn = rotateFab(anchor, 45f);

        popup.setOnDismissListener(() -> {
            View fab = anchorRef.get();
            if (fab != null && fab.isAttachedToWindow()) {
                rotateFab(fab, 0f);
            }
            if (rotateIn != null) rotateIn.cancel();
        });

        LinearLayout optionPlan = popupView.findViewById(R.id.optionPlan);
        optionPlan.setOnClickListener(v -> {
            popup.dismiss();
            if (context instanceof FragmentActivity) {
                FragmentActivity activity = (FragmentActivity) context;
                AddPlanDialog dialog = new AddPlanDialog();
                dialog.setOnPlanAddedListener(() -> {
                    if (onPlanClick != null) onPlanClick.run();
                });
                dialog.show(activity.getSupportFragmentManager(), "AddPlanDialog");
            }
        });

        LinearLayout optionMow = popupView.findViewById(R.id.optionMow);
        optionMow.setOnClickListener(v -> {
            popup.dismiss();
            if (context instanceof FragmentActivity) {
                FragmentActivity activity = (FragmentActivity) context;
                FragmentMow fragment = new FragmentMow();
                activity.getSupportFragmentManager()
                        .beginTransaction()
                        .setCustomAnimations(
                                android.R.anim.fade_in,
                                android.R.anim.fade_out
                        )
                        .replace(R.id.fragmentContainer, fragment)
                        .addToBackStack(null)
                        .commit();
            }
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

    private static ObjectAnimator rotateFab(View fab, float degrees) {
        if (fab == null || !fab.isAttachedToWindow()) return null;
        ObjectAnimator animator = ObjectAnimator.ofFloat(fab, "rotation", fab.getRotation(), degrees);
        animator.setDuration(250);
        animator.setInterpolator(new android.view.animation.OvershootInterpolator());
        animator.start();
        return animator;
    }
}