package com.example.todo.Onboarding;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.example.todo.MainActivity;
import com.example.todo.R;

import org.jetbrains.annotations.Nullable;

public class Onboarding3 extends Fragment {

    private final Handler handler = new Handler(Looper.getMainLooper());

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.activity_onboarding3, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        // do wywalenia potem, po tym jak wpadne jak ma wygladac aplikacja
        handler.postDelayed(() -> {
            if (isAdded() && getActivity() != null) {
                SharedPreferences prefs = requireActivity()
                        .getSharedPreferences("app_prefs", android.content.Context.MODE_PRIVATE);
                prefs.edit().putBoolean("onboarding_ukonczone", true).apply();

                Intent intent = new Intent(requireActivity(), MainActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
            }
        }, 5000);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        handler.removeCallbacksAndMessages(null);
    }
}