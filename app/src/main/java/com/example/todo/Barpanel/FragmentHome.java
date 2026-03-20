package com.example.todo.Barpanel;

import android.content.Intent;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;
import android.graphics.Typeface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
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
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class FragmentHome extends Fragment {

    private TextView tvGreetingFull;
    private TextView tvDate;
    private TextView tvTodayCount;
    private LinearLayout todayEventsContainer;
    private LinearLayout emptyContainer;
    private ProgressBar todayProgress;

    private boolean initialLoadDone = false;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_home, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        tvGreetingFull       = view.findViewById(R.id.tvGreetingFull);
        tvDate               = view.findViewById(R.id.tvDate);
        tvTodayCount         = view.findViewById(R.id.tvTodayCount);
        todayEventsContainer = view.findViewById(R.id.todayEventsContainer);
        emptyContainer       = view.findViewById(R.id.emptyContainer);
        todayProgress        = view.findViewById(R.id.todayProgress);

        setDate();
        loadNick();
        initialLoadDone = false;
        loadTodayEvents();

        view.findViewById(R.id.btnAddFirst).setOnClickListener(v -> openAddPlanDialog());
    }

    @Override
    public void onResume() {
        super.onResume();
        if (initialLoadDone) {
            refreshEvents();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        tvGreetingFull       = null;
        tvDate               = null;
        tvTodayCount         = null;
        todayEventsContainer = null;
        emptyContainer       = null;
        todayProgress        = null;
    }

    private void openAddPlanDialog() {
        if (!isAdded() || getActivity() == null) return;
        AddPlanDialog dialog = new AddPlanDialog();
        dialog.setOnPlanAddedListener(this::refreshEvents);
        dialog.show(getChildFragmentManager(), "AddPlanDialog");
    }

    public void refreshEvents() {
        if (!isAdded() || todayEventsContainer == null) return;
        todayEventsContainer.removeAllViews();
        if (emptyContainer != null) emptyContainer.setVisibility(View.GONE);
        if (tvTodayCount != null) tvTodayCount.setVisibility(View.GONE);
        if (todayProgress != null) todayProgress.setVisibility(View.VISIBLE);
        loadTodayEvents();
    }

    private String getGreeting() {
        int hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
        if (hour < 12)      return getString(R.string.greeting_morning);
        else if (hour < 18) return getString(R.string.greeting_afternoon);
        else                return getString(R.string.greeting_evening);
    }

    private void setDate() {
        SimpleDateFormat sdf = new SimpleDateFormat("EEEE, d MMMM", new Locale("pl"));
        String dateText = sdf.format(new Date());
        tvDate.setText(Character.toUpperCase(dateText.charAt(0)) + dateText.substring(1));
    }

    private void setGreetingWithNick(String nick) {
        if (!isAdded() || tvGreetingFull == null) return;
        String greeting = getGreeting() + " ";
        SpannableStringBuilder sb = new SpannableStringBuilder();
        SpannableString greetingSpan = new SpannableString(greeting);
        greetingSpan.setSpan(new ForegroundColorSpan(
                ContextCompat.getColor(requireContext(), R.color.brown_medium)), 0, greeting.length(), 0);
        SpannableString nickSpan = new SpannableString(nick);
        nickSpan.setSpan(new ForegroundColorSpan(
                ContextCompat.getColor(requireContext(), R.color.brown_primary)), 0, nick.length(), 0);
        nickSpan.setSpan(new StyleSpan(Typeface.BOLD), 0, nick.length(), 0);
        sb.append(greetingSpan);
        sb.append(nickSpan);
        tvGreetingFull.setText(sb);
        tvGreetingFull.setAlpha(0f);
        tvGreetingFull.animate().alpha(1f).setDuration(400).start();
    }

    private void loadNick() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) { setGreetingWithNick(""); return; }
        FirebaseFirestore.getInstance()
                .collection("nicks")
                .document(user.getUid())
                .get()
                .addOnSuccessListener(doc -> {
                    if (!isAdded() || tvGreetingFull == null) return;
                    String nick = doc.getString("nick");
                    if (nick == null || nick.isEmpty()) nick = user.getEmail();
                    if (nick == null) nick = "";
                    setGreetingWithNick(nick);
                });
    }

    private void loadTodayEvents() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) { initialLoadDone = true; showEmpty(); return; }
        String email = user.getEmail();
        if (email == null) { initialLoadDone = true; showEmpty(); return; }

        Calendar startOfDay = Calendar.getInstance();
        startOfDay.set(Calendar.HOUR_OF_DAY, 0);
        startOfDay.set(Calendar.MINUTE, 0);
        startOfDay.set(Calendar.SECOND, 0);
        startOfDay.set(Calendar.MILLISECOND, 0);
        final long startMs = startOfDay.getTimeInMillis();

        Calendar endOfDay = (Calendar) startOfDay.clone();
        endOfDay.add(Calendar.DAY_OF_MONTH, 1);
        final long endMs = endOfDay.getTimeInMillis();

        FirebaseFirestore.getInstance()
                .collection("plans")
                .whereEqualTo("email", email)
                .get()
                .addOnSuccessListener(query -> {
                    initialLoadDone = true;
                    if (!isAdded() || todayEventsContainer == null) return;
                    if (todayProgress != null) todayProgress.setVisibility(View.GONE);

                    List<QueryDocumentSnapshot> matching = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : query) {
                        com.google.firebase.Timestamp ts = doc.getTimestamp("dueTime");
                        if (ts == null) continue;
                        long docMs = ts.toDate().getTime();
                        if (docMs >= startMs && docMs < endMs) matching.add(doc);
                    }

                    if (matching.isEmpty()) {
                        showEmpty();
                        return;
                    }

                    if (tvTodayCount != null) {
                        tvTodayCount.setText(getString(R.string.home_count, matching.size()));
                        tvTodayCount.setVisibility(View.VISIBLE);
                    }

                    SimpleDateFormat timeFmt = new SimpleDateFormat("HH:mm", new Locale("pl"));
                    int delay = 0;
                    for (QueryDocumentSnapshot doc : matching) {
                        com.google.firebase.Timestamp ts = doc.getTimestamp("dueTime");
                        final String planId = doc.getId();

                        String title = doc.getString("title");
                        if (title == null || title.isEmpty()) title = doc.getString("content");
                        if (title == null || title.isEmpty()) title = getString(R.string.home_untitled);

                        View row = LayoutInflater.from(requireContext())
                                .inflate(R.layout.item_today_event, todayEventsContainer, false);
                        ((TextView) row.findViewById(R.id.tvEventTitle)).setText(title);
                        ((TextView) row.findViewById(R.id.tvEventTime)).setText(timeFmt.format(ts.toDate()));

                        row.setOnClickListener(v -> openPlanDetail(planId));

                        row.setAlpha(0f);
                        row.setTranslationY(16f);
                        todayEventsContainer.addView(row);

                        final int d = delay;
                        row.animate().alpha(1f).translationY(0f)
                                .setStartDelay(d).setDuration(280).start();
                        delay += 60;
                    }
                })
                .addOnFailureListener(e -> {
                    initialLoadDone = true;
                    if (!isAdded()) return;
                    if (todayProgress != null) todayProgress.setVisibility(View.GONE);
                    showEmpty();
                });
    }

    private void openPlanDetail(String planId) {
        if (!isAdded() || getActivity() == null) return;
        Intent intent = new Intent(requireActivity(), PlanDetailActivity.class);
        intent.putExtra("plan_id", planId);
        startActivity(intent);
    }

    private void showEmpty() {
        if (emptyContainer == null) return;
        emptyContainer.setAlpha(0f);
        emptyContainer.setVisibility(View.VISIBLE);
        emptyContainer.animate().alpha(1f).setDuration(300).start();
    }
}