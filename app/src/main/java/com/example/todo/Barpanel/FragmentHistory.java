package com.example.todo.Barpanel;

import android.animation.ValueAnimator;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.example.todo.PlanDetailActivity;
import com.example.todo.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public class FragmentHistory extends Fragment {

    private LinearLayout listContainer;
    private LinearLayout emptyContainer;
    private TextView     tvHistoryCount;

    private View shimmerCard1, shimmerCard2, shimmerCard3;
    private ValueAnimator shimmerAnim;

    private boolean initialLoadDone = false;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_history, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        listContainer  = view.findViewById(R.id.historyListContainer);
        emptyContainer = view.findViewById(R.id.historyEmptyContainer);
        tvHistoryCount = view.findViewById(R.id.tvHistoryCount);
        shimmerCard1   = view.findViewById(R.id.shimmerCard1);
        shimmerCard2   = view.findViewById(R.id.shimmerCard2);
        shimmerCard3   = view.findViewById(R.id.shimmerCard3);

        initialLoadDone = false;
        startShimmer();
        loadPlans();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (initialLoadDone) {
            refresh();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        stopShimmer();
        listContainer  = null;
        emptyContainer = null;
        tvHistoryCount = null;
        shimmerCard1   = null;
        shimmerCard2   = null;
        shimmerCard3   = null;
    }

    private void refresh() {
        if (!isAdded() || listContainer == null) return;
        listContainer.removeAllViews();
        if (emptyContainer != null) emptyContainer.setVisibility(View.GONE);
        if (tvHistoryCount != null) tvHistoryCount.setVisibility(View.GONE);
        loadPlans();
    }

    private void startShimmer() {
        shimmerAnim = ValueAnimator.ofFloat(0.12f, 0.45f);
        shimmerAnim.setDuration(900);
        shimmerAnim.setRepeatMode(ValueAnimator.REVERSE);
        shimmerAnim.setRepeatCount(ValueAnimator.INFINITE);
        shimmerAnim.addUpdateListener(anim -> {
            if (!isAdded()) return;
            float v = (float) anim.getAnimatedValue();
            if (shimmerCard1 != null) shimmerCard1.setAlpha(v);
            if (shimmerCard2 != null) shimmerCard2.setAlpha(v * 0.85f);
            if (shimmerCard3 != null) shimmerCard3.setAlpha(v * 0.65f);
        });
        shimmerAnim.start();
    }

    private void stopShimmer() {
        if (shimmerAnim != null) {
            shimmerAnim.cancel();
            shimmerAnim = null;
        }
    }

    private void hideShimmers() {
        stopShimmer();
        if (shimmerCard1 != null) shimmerCard1.setVisibility(View.GONE);
        if (shimmerCard2 != null) shimmerCard2.setVisibility(View.GONE);
        if (shimmerCard3 != null) shimmerCard3.setVisibility(View.GONE);
    }

    private void loadPlans() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) { hideShimmers(); showEmpty(); return; }

        String email = user.getEmail();
        if (email == null) { hideShimmers(); showEmpty(); return; }

        FirebaseFirestore.getInstance()
                .collection("plans")
                .whereEqualTo("email", email)
                .get()
                .addOnSuccessListener(query -> {
                    if (!isAdded() || listContainer == null) return;
                    initialLoadDone = true;
                    hideShimmers();

                    List<QueryDocumentSnapshot> all = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : query) all.add(doc);

                    Collections.sort(all, (a, b) -> {
                        com.google.firebase.Timestamp ta = a.getTimestamp("createdAt");
                        com.google.firebase.Timestamp tb = b.getTimestamp("createdAt");
                        if (ta == null && tb == null) return 0;
                        if (ta == null) return 1;
                        if (tb == null) return -1;
                        return tb.compareTo(ta);
                    });

                    if (all.isEmpty()) { showEmpty(); return; }

                    if (tvHistoryCount != null) {
                        tvHistoryCount.setText(getString(R.string.history_count, all.size()));
                        tvHistoryCount.setVisibility(View.VISIBLE);
                    }

                    SimpleDateFormat dateFmt =
                            new SimpleDateFormat("d MMM yyyy, HH:mm", new Locale("pl"));
                    SimpleDateFormat timeFmt =
                            new SimpleDateFormat("HH:mm", new Locale("pl"));

                    int delay = 0;
                    for (QueryDocumentSnapshot doc : all) {
                        final String planId = doc.getId();

                        String title = doc.getString("title");
                        if (title == null || title.isEmpty()) title = doc.getString("content");
                        if (title == null || title.isEmpty()) title = getString(R.string.home_untitled);

                        String priority = doc.getString("priority");

                        com.google.firebase.Timestamp createdTs = doc.getTimestamp("createdAt");
                        String createdLabel = createdTs != null
                                ? dateFmt.format(createdTs.toDate()) : "—";

                        com.google.firebase.Timestamp dueTs = doc.getTimestamp("dueTime");
                        String dueLabel = dueTs != null
                                ? dateFmt.format(dueTs.toDate()) : "—";

                        // Czytaj listę wszystkich powiadomień
                        List<com.google.firebase.Timestamp> notifList =
                                (List<com.google.firebase.Timestamp>) doc.get("notificationTimes");

                        // Fallback dla starych planów
                        if (notifList == null || notifList.isEmpty()) {
                            com.google.firebase.Timestamp single = doc.getTimestamp("notificationTime");
                            if (single != null) {
                                notifList = new ArrayList<>();
                                notifList.add(single);
                            }
                        }

                        boolean hasNotif = notifList != null && !notifList.isEmpty();

                        StringBuilder notifLabel = new StringBuilder();
                        if (hasNotif) {
                            for (int i = 0; i < notifList.size(); i++) {
                                if (i > 0) notifLabel.append(", ");
                                notifLabel.append(timeFmt.format(notifList.get(i).toDate()));
                            }
                        } else {
                            notifLabel.append(getString(R.string.history_no_notif));
                        }

                        Boolean isDone = doc.getBoolean("isDone");
                        boolean done = isDone != null && isDone;

                        View card = LayoutInflater.from(requireContext())
                                .inflate(R.layout.item_history_card, listContainer, false);

                        ((TextView) card.findViewById(R.id.tvHistoryTitle)).setText(title);
                        ((TextView) card.findViewById(R.id.tvHistoryCreated)).setText(
                                getString(R.string.history_label_added) + " " + createdLabel);
                        ((TextView) card.findViewById(R.id.tvHistoryDue)).setText(
                                getString(R.string.history_label_due) + " " + dueLabel);
                        ((TextView) card.findViewById(R.id.tvHistoryNotif)).setText(
                                getString(R.string.history_label_notif) + " " + notifLabel);

                        TextView tvPrio = card.findViewById(R.id.tvHistoryPriority);
                        tvPrio.setText(formatPriority(priority));
                        tvPrio.setBackgroundTintList(
                                android.content.res.ColorStateList.valueOf(priorityColor(priority)));

                        TextView tvStatus = card.findViewById(R.id.tvHistoryStatus);
                        if (done) {
                            tvStatus.setText(getString(R.string.history_status_done));
                            tvStatus.setBackgroundTintList(
                                    android.content.res.ColorStateList.valueOf(
                                            ContextCompat.getColor(requireContext(), R.color.priority_low)));
                        } else {
                            tvStatus.setText(getString(R.string.history_status_pending));
                            tvStatus.setBackgroundTintList(
                                    android.content.res.ColorStateList.valueOf(
                                            ContextCompat.getColor(requireContext(), R.color.priority_medium)));
                        }

                        View notifDot = card.findViewById(R.id.notifIndicator);
                        notifDot.setBackgroundTintList(
                                android.content.res.ColorStateList.valueOf(
                                        hasNotif
                                                ? ContextCompat.getColor(requireContext(), R.color.brown_primary)
                                                : ContextCompat.getColor(requireContext(), R.color.brown_light)));

                        card.setOnClickListener(v -> openPlanDetail(planId));

                        card.setAlpha(0f);
                        card.setTranslationY(20f);
                        listContainer.addView(card);
                        final int d = delay;
                        card.animate()
                                .alpha(1f)
                                .translationY(0f)
                                .setStartDelay(d)
                                .setDuration(300)
                                .start();
                        delay += 55;
                    }
                })
                .addOnFailureListener(e -> {
                    if (!isAdded()) return;
                    initialLoadDone = true;
                    hideShimmers();
                    showEmpty();
                });
    }

    private void showEmpty() {
        if (emptyContainer == null) return;
        emptyContainer.setAlpha(0f);
        emptyContainer.setVisibility(View.VISIBLE);
        emptyContainer.animate().alpha(1f).setDuration(300).start();
    }

    private void openPlanDetail(String planId) {
        if (!isAdded() || getActivity() == null) return;
        Intent intent = new Intent(requireActivity(), PlanDetailActivity.class);
        intent.putExtra("plan_id", planId);
        startActivity(intent);
    }

    private String formatPriority(String p) {
        if (p == null) return getString(R.string.priority_medium);
        switch (p) {
            case "low":  return getString(R.string.priority_low);
            case "high": return getString(R.string.priority_high);
            default:     return getString(R.string.priority_medium);
        }
    }

    private int priorityColor(String p) {
        if (!isAdded()) return 0;
        if ("high".equals(p))
            return ContextCompat.getColor(requireContext(), R.color.priority_high);
        if ("low".equals(p))
            return ContextCompat.getColor(requireContext(), R.color.priority_low);
        return ContextCompat.getColor(requireContext(), R.color.priority_medium);
    }
}