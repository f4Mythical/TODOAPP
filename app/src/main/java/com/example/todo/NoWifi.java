package com.example.todo;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

import com.example.todo.Onboarding.OnBoarding;

public class NoWifi extends AppCompatActivity {

    private ConnectivityManager.NetworkCallback networkCallback;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    private TextView tvSubtitle;
    private final Handler dotsHandler = new Handler(Looper.getMainLooper());
    private int dotsCount = 0;

    private final Runnable dotsRunnable = new Runnable() {
        @Override
        public void run() {
            if (tvSubtitle == null) return;
            int state = dotsCount % 4;
            String dots;
            if (state == 0) dots = ".";
            else if (state == 1) dots = "..";
            else if (state == 2) dots = "...";
            else dots = "";
            tvSubtitle.setText(getString(R.string.waiting) + dots);
            dotsCount++;
            dotsHandler.postDelayed(this, 500);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.nowifi);
        tvSubtitle = findViewById(R.id.tv_subtitle);
        dotsHandler.post(dotsRunnable);
    }

    @Override
    protected void onResume() {
        super.onResume();
        registerNetworkCallback();
        checkInternet();
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterNetworkCallback();
        dotsHandler.removeCallbacks(dotsRunnable);
    }

    private void registerNetworkCallback() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
        NetworkRequest request = new NetworkRequest.Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .build();

        networkCallback = new ConnectivityManager.NetworkCallback() {
            @Override
            public void onCapabilitiesChanged(Network network, NetworkCapabilities capabilities) {
                if (capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)) {
                    mainHandler.post(() -> goToApp());
                }
            }

            @Override
            public void onAvailable(Network network) {
                mainHandler.post(() -> checkInternet());
            }
        };

        cm.registerNetworkCallback(request, networkCallback);
    }

    private void unregisterNetworkCallback() {
        if (networkCallback != null) {
            ConnectivityManager cm = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
            cm.unregisterNetworkCallback(networkCallback);
            networkCallback = null;
        }
    }

    private void checkInternet() {
        if (isInternetAvailable()) {
            goToApp();
        }
    }

    private boolean isInternetAvailable() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
        if (cm == null) return false;
        android.net.Network activeNetwork = cm.getActiveNetwork();
        if (activeNetwork == null) return false;
        NetworkCapabilities capabilities = cm.getNetworkCapabilities(activeNetwork);
        return capabilities != null &&
                capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED);
    }

    private void goToApp() {
        SharedPreferences prefs = getSharedPreferences("app_prefs", MODE_PRIVATE);
        boolean onboardingDone = prefs.getBoolean("onboarding_ukonczone", false);

        Intent intent;
        if (onboardingDone) {
            intent = new Intent(this, MainActivity.class);
        } else {
            intent = new Intent(this, OnBoarding.class);
        }
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    @Override
    public void onBackPressed() {
    }
}