package com.example.todo.Barpanel;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
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

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_profile, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        tvAvatarInitials = view.findViewById(R.id.tvAvatarInitials);
        tvProfileNick    = view.findViewById(R.id.tvProfileNick);
        tvProfileEmail   = view.findViewById(R.id.tvProfileEmail);
        tvRowNick        = view.findViewById(R.id.tvRowNick);
        tvRowJoined      = view.findViewById(R.id.tvRowJoined);
        tvRowPlans       = view.findViewById(R.id.tvRowPlans);
        Button btnLogout = view.findViewById(R.id.btnLogout);

        loadProfile();
        loadPlansCount();

        btnLogout.setOnClickListener(v -> logout());
    }

    private void loadProfile() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;

        String email = user.getEmail() != null ? user.getEmail() : "";
        tvProfileEmail.setText(email);

        FirebaseFirestore.getInstance()
                .collection("nicks")
                .document(user.getUid())
                .get()
                .addOnSuccessListener(doc -> {
                    if (!isAdded()) return;
                    String nick = doc.getString("nick");
                    if (nick == null || nick.isEmpty()) nick = email;

                    tvProfileNick.setText(nick);
                    tvRowNick.setText(nick);
                    tvAvatarInitials.setText(nick.substring(0, 1).toUpperCase());
                });

        FirebaseFirestore.getInstance()
                .collection("users")
                .document(user.getUid())
                .get()
                .addOnSuccessListener(doc -> {
                    if (!isAdded()) return;
                    Timestamp ts = doc.getTimestamp("createdAt");
                    if (ts != null) {
                        Date date = ts.toDate();
                        SimpleDateFormat sdf = new SimpleDateFormat("d MMM yyyy", new Locale("pl"));
                        tvRowJoined.setText(sdf.format(date));
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
                    if (!isAdded()) return;
                    tvRowPlans.setText(String.valueOf(query.size()));
                })
                .addOnFailureListener(e -> {
                    if (!isAdded()) return;
                    tvRowPlans.setText("0");
                });
    }

    private void logout() {
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