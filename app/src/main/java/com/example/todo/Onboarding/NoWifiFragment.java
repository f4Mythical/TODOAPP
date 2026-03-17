package com.example.todo.Onboarding;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.todo.R;

public class NoWifiFragment extends Fragment {

    public interface OnRetryListener {
        void onRetry();
    }

    private OnRetryListener retryListener;
    private ConnectivityManager.NetworkCallback networkCallback;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    private TextView tvSubtitle;
    private final Handler dotsHandler = new Handler(Looper.getMainLooper());
    private int dotsCount = 0;
    private boolean retryHandled = false;

    private final Runnable dotsRunnable = new Runnable() {
        @Override
        public void run() {
            if (tvSubtitle == null || !isAdded()) return;

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

    public void setOnRetryListener(OnRetryListener listener) {
        this.retryListener = listener;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.nowifi, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        retryHandled = false;
        tvSubtitle = view.findViewById(R.id.tv_subtitle);
        dotsHandler.post(dotsRunnable);
        registerNetworkCallback();
    }

    @Override
    public void onDestroyView() {
        dotsHandler.removeCallbacks(dotsRunnable);
        tvSubtitle = null;
        super.onDestroyView();
        unregisterNetworkCallback();
    }

    private void registerNetworkCallback() {
        if (!isAdded()) return;

        ConnectivityManager cm = (ConnectivityManager) requireContext().getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkRequest request = new NetworkRequest.Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .build();

        networkCallback = new ConnectivityManager.NetworkCallback() {
            @Override
            public void onCapabilitiesChanged(Network network, NetworkCapabilities capabilities) {
                if (capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)) {
                    mainHandler.post(() -> {
                        if (!retryHandled && retryListener != null) {
                            retryHandled = true;
                            retryListener.onRetry();
                        }
                    });
                }
            }

            @Override
            public void onAvailable(Network network) {
                mainHandler.post(() -> {
                    if (!retryHandled) checkInternet();
                });
            }
        };

        cm.registerNetworkCallback(request, networkCallback);
        checkInternet();
    }

    private void unregisterNetworkCallback() {
        if (networkCallback != null && isAdded()) {
            ConnectivityManager cm = (ConnectivityManager) requireContext().getSystemService(Context.CONNECTIVITY_SERVICE);
            cm.unregisterNetworkCallback(networkCallback);
            networkCallback = null;
        }
    }

    private void checkInternet() {
        if (isInternetAvailable() && retryListener != null) {
            retryHandled = true;
            retryListener.onRetry();
        }
    }

    private boolean isInternetAvailable() {
        if (!isAdded()) return false;

        ConnectivityManager cm = (ConnectivityManager) requireContext().getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm == null) return false;

        Network activeNetwork = cm.getActiveNetwork();
        if (activeNetwork == null) return false;

        NetworkCapabilities capabilities = cm.getNetworkCapabilities(activeNetwork);
        return capabilities != null &&
                capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED);
    }
}