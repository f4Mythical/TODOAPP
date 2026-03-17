package com.example.todo.Barpanel;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.example.todo.Onboarding.OnBoarding;
import com.example.todo.R;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class FragmentProfile extends Fragment {

    private TextView tvAvatarInitials;
    private TextView tvProfileNick;
    private TextView tvProfileEmail;
    private TextView tvRowNick;
    private TextView tvRowJoined;
    private TextView tvRowPlans;

    private ProgressBar avatarProgress;
    private View shimmerNick;
    private View shimmerEmail;
    private View shimmerRowNick;
    private View shimmerRowJoined;
    private View shimmerRowPlans;

    private ValueAnimator shimmerAnim;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_profile, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        tvAvatarInitials  = view.findViewById(R.id.tvAvatarInitials);
        tvProfileNick     = view.findViewById(R.id.tvProfileNick);
        tvProfileEmail    = view.findViewById(R.id.tvProfileEmail);
        tvRowNick         = view.findViewById(R.id.tvRowNick);
        tvRowJoined       = view.findViewById(R.id.tvRowJoined);
        tvRowPlans        = view.findViewById(R.id.tvRowPlans);
        avatarProgress    = view.findViewById(R.id.avatarProgress);
        shimmerNick       = view.findViewById(R.id.shimmerNick);
        shimmerEmail      = view.findViewById(R.id.shimmerEmail);
        shimmerRowNick    = view.findViewById(R.id.shimmerRowNick);
        shimmerRowJoined  = view.findViewById(R.id.shimmerRowJoined);
        shimmerRowPlans   = view.findViewById(R.id.shimmerRowPlans);
        Button btnLogout  = view.findViewById(R.id.btnLogout);

        startShimmer();
        loadProfile();
        loadPlansCount();

        btnLogout.setOnClickListener(v -> logout());
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        stopShimmer();
        tvAvatarInitials  = null;
        tvProfileNick     = null;
        tvProfileEmail    = null;
        tvRowNick         = null;
        tvRowJoined       = null;
        tvRowPlans        = null;
        avatarProgress    = null;
        shimmerNick       = null;
        shimmerEmail      = null;
        shimmerRowNick    = null;
        shimmerRowJoined  = null;
        shimmerRowPlans   = null;
    }

    private void startShimmer() {
        shimmerAnim = ValueAnimator.ofFloat(0.15f, 0.5f);
        shimmerAnim.setDuration(900);
        shimmerAnim.setRepeatMode(ValueAnimator.REVERSE);
        shimmerAnim.setRepeatCount(ValueAnimator.INFINITE);
        shimmerAnim.addUpdateListener(anim -> {
            if (!isAdded()) return;
            float val = (float) anim.getAnimatedValue();
            setShimmerAlpha(val);
        });
        shimmerAnim.start();
    }

    private void stopShimmer() {
        if (shimmerAnim != null) {
            shimmerAnim.cancel();
            shimmerAnim = null;
        }
    }

    private void setShimmerAlpha(float alpha) {
        if (shimmerNick      != null) shimmerNick.setAlpha(alpha);
        if (shimmerEmail     != null) shimmerEmail.setAlpha(alpha);
        if (shimmerRowNick   != null) shimmerRowNick.setAlpha(alpha);
        if (shimmerRowJoined != null) shimmerRowJoined.setAlpha(alpha);
        if (shimmerRowPlans  != null) shimmerRowPlans.setAlpha(alpha);
    }

    private void revealView(View shimmer, View real, String text) {
        if (shimmer == null || real == null || !isAdded()) return;
        if (real instanceof TextView) ((TextView) real).setText(text);
        real.setAlpha(0f);
        real.setVisibility(View.VISIBLE);
        real.animate().alpha(1f).setDuration(300).start();
        shimmer.animate().alpha(0f).setDuration(200)
                .setListener(new AnimatorListenerAdapter() {
                    @Override public void onAnimationEnd(Animator animation) {
                        if (shimmer != null) shimmer.setVisibility(View.GONE);
                    }
                }).start();
    }

    private void loadProfile() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;

        String email = user.getEmail() != null ? user.getEmail() : "";
        final String finalEmail = email;

        FirebaseFirestore.getInstance()
                .collection("nicks")
                .document(user.getUid())
                .get()
                .addOnSuccessListener(doc -> {
                    if (!isAdded() || tvProfileNick == null) return;

                    String nick = doc.getString("nick");
                    if (nick == null || nick.isEmpty()) nick = finalEmail;
                    final String finalNick = nick;

                    revealView(shimmerNick,     tvProfileNick, finalNick);
                    revealView(shimmerEmail,    tvProfileEmail, finalEmail);
                    revealView(shimmerRowNick,  tvRowNick, finalNick);

                    if (avatarProgress != null) {
                        avatarProgress.setVisibility(View.GONE);
                    }
                    if (tvAvatarInitials != null && !finalNick.isEmpty()) {
                        tvAvatarInitials.setText(finalNick.substring(0, 1).toUpperCase());
                        tvAvatarInitials.setAlpha(0f);
                        tvAvatarInitials.setVisibility(View.VISIBLE);
                        tvAvatarInitials.animate().alpha(1f).setDuration(300).start();
                    }
                });

        FirebaseFirestore.getInstance()
                .collection("users")
                .document(user.getUid())
                .get()
                .addOnSuccessListener(doc -> {
                    if (!isAdded() || tvRowJoined == null) return;
                    Timestamp ts = doc.getTimestamp("createdAt");
                    if (ts != null) {
                        Date date = ts.toDate();
                        SimpleDateFormat sdf = new SimpleDateFormat("d MMM yyyy", new Locale("pl"));
                        revealView(shimmerRowJoined, tvRowJoined, sdf.format(date));
                    }
                });
    }

    private void loadPlansCount() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;
        String email = user.getEmail();
        if (email == null) return;

        FirebaseFirestore.getInstance()
                .collection("plans")
                .whereEqualTo("email", email)
                .get()
                .addOnSuccessListener(query -> {
                    if (!isAdded() || tvRowPlans == null) return;
                    revealView(shimmerRowPlans, tvRowPlans, String.valueOf(query.size()));
                })
                .addOnFailureListener(e -> {
                    if (!isAdded() || tvRowPlans == null) return;
                    revealView(shimmerRowPlans, tvRowPlans, "0");
                });
    }

    private void logout() {
        if (!isAdded() || getActivity() == null) return;
        stopShimmer();
        FirebaseAuth.getInstance().signOut();
        requireActivity()
                .getSharedPreferences("app_prefs", android.content.Context.MODE_PRIVATE)
                .edit()
                .putBoolean("onboarding_ukonczone", false)
                .apply();
        Intent intent = new Intent(requireActivity(), OnBoarding.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
    }
}