package com.example.todo;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import com.example.todo.Barpanel.BarPanel;
import com.example.todo.Barpanel.FragmentHistory;
import com.example.todo.Barpanel.FragmentPlan;
import com.example.todo.Barpanel.FragmentProfile;
import com.example.todo.Barpanel.FragmentSettings;
import com.example.todo.Onboarding.OnBoarding;

public class MainActivity extends AppCompatActivity {

    private View navProfile;
    private View navPlan;
    private View navSettings;
    private View navHistory;
    private View navNew;

    private int currentTab = R.id.navPlan;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        SharedPreferences prefs = getSharedPreferences("app_prefs", MODE_PRIVATE);
        boolean onboardingDone = prefs.getBoolean("onboarding_ukonczone", false);

        if (!onboardingDone) {
            Intent intent = new Intent(this, OnBoarding.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
            return;
        }

        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        navProfile  = findViewById(R.id.navProfile);
        navPlan     = findViewById(R.id.navPlan);
        navSettings = findViewById(R.id.navSettings);
        navHistory  = findViewById(R.id.navHistory);
        navNew      = findViewById(R.id.navNew);

        navProfile.setOnClickListener(v  -> switchTab(R.id.navProfile,  new FragmentProfile()));
        navPlan.setOnClickListener(v     -> switchTab(R.id.navPlan,     new FragmentPlan()));
        navSettings.setOnClickListener(v -> switchTab(R.id.navSettings, new FragmentSettings()));
        navHistory.setOnClickListener(v  -> switchTab(R.id.navHistory,  new FragmentHistory()));
        navNew.setOnClickListener(v      -> BarPanel.showNewMenu(this, navNew, () -> {}));

        loadFragment(new FragmentPlan(), false);
        updateTabColors(R.id.navPlan);
    }

    private void switchTab(int tabId, Fragment fragment) {
        if (currentTab == tabId) return;
        currentTab = tabId;
        loadFragment(fragment, true);
        updateTabColors(tabId);
    }

    private void loadFragment(Fragment fragment, boolean animate) {
        FragmentTransaction tx = getSupportFragmentManager()
                .beginTransaction();

        if (animate) {
            tx.setCustomAnimations(
                    R.anim.fragment_fade_in,
                    R.anim.fragment_fade_out
            );
        }

        tx.replace(R.id.fragmentContainer, fragment)
                .commit();
    }

    private void updateTabColors(int activeTabId) {
        int active   = getColor(R.color.brown_primary);
        int inactive = getColor(R.color.brown_light);

        setTabColor(navProfile,  R.id.navProfileIcon,  R.id.navProfileLabel,  activeTabId == R.id.navProfile,  active, inactive);
        setTabColor(navPlan,     R.id.navPlanIcon,     R.id.navPlanLabel,     activeTabId == R.id.navPlan,     active, inactive);
        setTabColor(navSettings, R.id.navSettingsIcon, R.id.navSettingsLabel, activeTabId == R.id.navSettings, active, inactive);
        setTabColor(navHistory,  R.id.navHistoryIcon,  R.id.navHistoryLabel,  activeTabId == R.id.navHistory,  active, inactive);
    }

    private void setTabColor(View parent, int iconId, int labelId, boolean isActive, int active, int inactive) {
        int color = isActive ? active : inactive;

        android.widget.ImageView icon = parent.findViewById(iconId);
        android.widget.TextView label = parent.findViewById(labelId);

        if (icon != null)  icon.setColorFilter(color);
        if (label != null) label.setTextColor(color);

        parent.animate()
                .scaleX(isActive ? 1.1f : 1f)
                .scaleY(isActive ? 1.1f : 1f)
                .setDuration(200)
                .start();
    }
}