package com.example.todo;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.os.Handler;
import android.os.Looper;

public class NetworkReceiver {

    public interface NetworkListener {
        void onNetworkAvailable();
        void onNetworkLost();
    }

    private final Context context;
    private final ConnectivityManager connectivityManager;
    private ConnectivityManager.NetworkCallback networkCallback;
    private NetworkListener listener;
    private boolean wasAvailable = true;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    public NetworkReceiver(Context context) {
        this.context = context.getApplicationContext();
        this.connectivityManager = (ConnectivityManager) this.context.getSystemService(Context.CONNECTIVITY_SERVICE);
    }

    public void setListener(NetworkListener listener) {
        this.listener = listener;
    }

    public void start() {
        NetworkRequest request = new NetworkRequest.Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .build();

        networkCallback = new ConnectivityManager.NetworkCallback() {
            @Override
            public void onAvailable(Network network) {
            }

            @Override
            public void onCapabilitiesChanged(Network network, NetworkCapabilities capabilities) {
                mainHandler.post(() -> {
                    boolean isValidated = capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED);
                    if (isValidated && !wasAvailable && listener != null) {
                        listener.onNetworkAvailable();
                        wasAvailable = true;
                    }
                });
            }

            @Override
            public void onLost(Network network) {
                mainHandler.post(() -> {
                    if (wasAvailable && listener != null) {
                        listener.onNetworkLost();
                    }
                    wasAvailable = false;
                });
            }
        };

        connectivityManager.registerNetworkCallback(request, networkCallback);
        wasAvailable = isInternetAvailable(context);
    }

    public void stop() {
        if (networkCallback != null) {
            connectivityManager.unregisterNetworkCallback(networkCallback);
            networkCallback = null;
        }
    }

    public boolean isCurrentlyAvailable() {
        return isInternetAvailable(context);
    }

    public static boolean isInternetAvailable(Context context) {
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm == null) return false;

        Network activeNetwork = cm.getActiveNetwork();
        if (activeNetwork != null) {
            NetworkCapabilities capabilities = cm.getNetworkCapabilities(activeNetwork);
            if (capabilities != null &&
                    capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                    capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)) {
                return true;
            }
        }

        Network[] networks = cm.getAllNetworks();
        for (Network network : networks) {
            NetworkCapabilities capabilities = cm.getNetworkCapabilities(network);
            if (capabilities != null &&
                    capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                    capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)) {
                return true;
            }
        }

        return false;
    }
}