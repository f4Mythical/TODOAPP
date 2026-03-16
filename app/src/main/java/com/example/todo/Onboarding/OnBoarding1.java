package com.example.todo.Onboarding;

import android.animation.ObjectAnimator;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.example.todo.R;

import org.jetbrains.annotations.Nullable;

public class OnBoarding1 extends Fragment {
    private static final int TYPING_SPEED_MS = 60;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private TextView textViewGretings;
    private Button startButton;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.activity_on_boarding1, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        textViewGretings = view.findViewById(R.id.TextViewGretings);
        startButton = view.findViewById(R.id.buttonStart);

        startButton.setVisibility(View.INVISIBLE);
        startButton.setAlpha(0f);
        startButton.setOnClickListener(v -> goNext());

        String fullText = getString(R.string.greetings);
        typeText(textViewGretings, fullText, 0, () -> showButton());
    }

    private void typeText(TextView tv, String text, int index, Runnable onFinished) {
        if (!isAdded()) return;
        if (index < text.length()) {
            tv.setText(text.substring(0, index + 1));
            handler.postDelayed(() -> typeText(tv, text, index + 1, onFinished), TYPING_SPEED_MS);
        } else {
            handler.post(onFinished);
        }
    }

    private void showButton() {
        if (!isAdded()) return;
        startButton.setVisibility(View.VISIBLE);
        ObjectAnimator fadeIn = ObjectAnimator.ofFloat(startButton, "alpha", 0f, 1f);
        fadeIn.setDuration(600);
        fadeIn.start();
    }

    private void goNext() {
        if (getActivity() instanceof OnBoarding) {
            ((OnBoarding) getActivity()).goToLogin();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        handler.removeCallbacksAndMessages(null);
    }
}