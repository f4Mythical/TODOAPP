package com.example.todo.Onboarding;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.example.todo.NetworkReceiver;
import com.example.todo.R;

public class OnBoarding extends AppCompatActivity implements NoWifiFragment.OnRetryListener, NetworkReceiver.NetworkListener {

    private NetworkReceiver networkReceiver;
    private boolean isShowingNoWifi = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_on_boarding);

        networkReceiver = new NetworkReceiver(this);
        networkReceiver.setListener(this);

        if (savedInstanceState == null) {
            checkInitialState();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        networkReceiver.start();
        if (!isShowingNoWifi && !networkReceiver.isCurrentlyAvailable()) {
            showNoWifi();
            isShowingNoWifi = true;
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        networkReceiver.stop();
    }

    @Override
    public void onNetworkLost() {
        if (!isShowingNoWifi) {
            showNoWifi();
            isShowingNoWifi = true;
        }
    }

    @Override
    public void onNetworkAvailable() {

    }

    private void checkInitialState() {
        networkReceiver.start();
        if (networkReceiver.isCurrentlyAvailable()) {
            goToGreetings();
        } else {
            showNoWifi();
            isShowingNoWifi = true;
        }
    }

    private void showNoWifi() {
        NoWifiFragment fragment = new NoWifiFragment();
        fragment.setOnRetryListener(this);
        getSupportFragmentManager()
                .beginTransaction()
                .setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out)
                .replace(R.id.onboarding_fragment_container, fragment)
                .commit();
    }

    @Override
    public void onRetry() {
        if (NetworkReceiver.isInternetAvailable(this)) {
            goToGreetings();
            isShowingNoWifi = false;
        }
    }

    public void showFragment(Fragment fragment, boolean addToBackStack) {
        FragmentTransaction transaction = getSupportFragmentManager()
                .beginTransaction()
                .setCustomAnimations(
                        android.R.anim.fade_in,
                        android.R.anim.fade_out,
                        android.R.anim.fade_in,
                        android.R.anim.fade_out
                )
                .replace(R.id.onboarding_fragment_container, fragment);
        if (addToBackStack) {
            transaction.addToBackStack(null);
        }
        transaction.commit();
    }

    public void goToGreetings() { showFragment(new OnBoarding1(), true); }

    public void goToLogin()     { showFragment(new Onboarding2(), true); }

    public void goToAppIntro()  { showFragment(new Onboarding3(), true); }

    @Override
    public void onBackPressed() {
        FragmentManager fm = getSupportFragmentManager();
        if (isShowingNoWifi) {
            return;
        }
        if (fm.getBackStackEntryCount() > 0) {
            fm.popBackStack();
        } else {
            super.onBackPressed();
        }
    }
}