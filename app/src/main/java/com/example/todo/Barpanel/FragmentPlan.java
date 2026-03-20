package com.example.todo.Barpanel;

import android.animation.ObjectAnimator;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.todo.PlanDetailActivity;
import com.example.todo.R;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class FragmentPlan extends Fragment {

    private Calendar currentCalendar;
    private Calendar today;
    private boolean isWeekMode = true;

    private TextView tvMonthYear;
    private View toggleIndicator;
    private TextView btnWeekLabel;
    private TextView btnMonthLabel;
    private SwipeRecyclerView recyclerView;
    private PlanAdapter adapter;
    private ObjectAnimator currentToggleAnim;

    private ExecutorService executor;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    private static final String[] DAY_NAMES_FULL = {
            "Poniedziałek", "Wtorek", "Środa", "Czwartek", "Piątek", "Sobota", "Niedziela"};

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_plan, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        executor = Executors.newSingleThreadExecutor();
        today           = Calendar.getInstance();
        currentCalendar = Calendar.getInstance();

        tvMonthYear     = view.findViewById(R.id.tvMonthYear);
        toggleIndicator = view.findViewById(R.id.toggleIndicator);
        btnWeekLabel    = view.findViewById(R.id.btnWeek);
        btnMonthLabel   = view.findViewById(R.id.btnMonth);
        recyclerView    = view.findViewById(R.id.recyclerView);

        adapter = new PlanAdapter(today, currentCalendar, new PlanAdapter.OnDayClickListener() {
            @Override
            public void onClick(Calendar day, int dowIndex) {
                if (isAdded() && getActivity() != null) {
                    showDayPreview(day, dowIndex);
                }
            }
            @Override
            public void onLongClick(Calendar day, int dowIndex) {
                if (isAdded() && getActivity() != null) {
                    currentCalendar.set(Calendar.YEAR,         day.get(Calendar.YEAR));
                    currentCalendar.set(Calendar.MONTH,        day.get(Calendar.MONTH));
                    currentCalendar.set(Calendar.DAY_OF_MONTH, day.get(Calendar.DAY_OF_MONTH));
                    openAddPlanDialog(day);
                }
            }
        });

        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerView.setAdapter(adapter);
        recyclerView.setItemAnimator(null);

        recyclerView.setOnSwipeListener(new SwipeRecyclerView.OnSwipeListener() {
            @Override public void onSwipeLeft()  { navigateForward();  }
            @Override public void onSwipeRight() { navigateBackward(); }
        });

        view.findViewById(R.id.btnPrev).setOnClickListener(v -> navigateBackward());
        view.findViewById(R.id.btnNext).setOnClickListener(v -> navigateForward());
        btnWeekLabel.setOnClickListener(v -> setMode(true));
        btnMonthLabel.setOnClickListener(v -> setMode(false));

        loadData(0);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (currentToggleAnim != null) { currentToggleAnim.cancel(); currentToggleAnim = null; }
        if (recyclerView != null) { recyclerView.animate().cancel(); recyclerView.setOnSwipeListener(null); }
        mainHandler.removeCallbacksAndMessages(null);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (executor != null) executor.shutdownNow();
    }

    private void navigateForward() {
        if (isWeekMode) currentCalendar.add(Calendar.WEEK_OF_YEAR, 1);
        else currentCalendar.add(Calendar.MONTH, 1);
        loadData(-1);
    }

    private void navigateBackward() {
        if (isWeekMode) currentCalendar.add(Calendar.WEEK_OF_YEAR, -1);
        else currentCalendar.add(Calendar.MONTH, -1);
        loadData(1);
    }

    private void setMode(boolean weekMode) {
        if (isWeekMode == weekMode) return;
        isWeekMode = weekMode;
        animateToggle(weekMode);
        loadData(0);
    }

    private void animateToggle(boolean toWeek) {
        if (toggleIndicator == null || !isAdded()) return;
        int targetX = toWeek ? 0 : dpToPx(88);
        if (currentToggleAnim != null) currentToggleAnim.cancel();
        currentToggleAnim = ObjectAnimator.ofFloat(toggleIndicator, "translationX",
                toggleIndicator.getTranslationX(), targetX);
        currentToggleAnim.setDuration(220);
        currentToggleAnim.start();
        btnWeekLabel.setTextColor(ContextCompat.getColor(requireContext(),
                toWeek ? R.color.cream : R.color.brown_medium));
        btnMonthLabel.setTextColor(ContextCompat.getColor(requireContext(),
                toWeek ? R.color.brown_medium : R.color.cream));
    }

    private void loadData(int direction) {
        if (!isAdded()) return;
        updateHeader();
        final Calendar snap    = (Calendar) currentCalendar.clone();
        final boolean weekMode = isWeekMode;

        executor.execute(() -> {
            List<PlanItem> items = weekMode ? buildWeekItems(snap) : buildMonthItems(snap);
            mainHandler.post(() -> {
                if (!isAdded() || recyclerView == null) return;
                adapter.setItems(items, snap);
                recyclerView.animate().cancel();
                recyclerView.setAlpha(0f);
                recyclerView.animate().alpha(1f).setDuration(200).start();
                if (direction != 0) {
                    recyclerView.setTranslationX(direction * dpToPx(40));
                    recyclerView.animate().translationX(0f).setDuration(220)
                            .setInterpolator(new android.view.animation.DecelerateInterpolator())
                            .start();
                }
                recyclerView.scrollToPosition(0);
            });
        });
    }

    private void updateHeader() {
        if (tvMonthYear == null || !isAdded()) return;
        if (isWeekMode) {
            Calendar ws = getWeekStart(currentCalendar);
            Calendar we = (Calendar) ws.clone();
            we.add(Calendar.DAY_OF_MONTH, 6);
            SimpleDateFormat sdf = new SimpleDateFormat("d MMM", new Locale("pl"));
            tvMonthYear.setText(sdf.format(ws.getTime()) + " – " + sdf.format(we.getTime()));
        } else {
            SimpleDateFormat sdf = new SimpleDateFormat("LLLL yyyy", new Locale("pl"));
            String text = sdf.format(currentCalendar.getTime());
            tvMonthYear.setText(Character.toUpperCase(text.charAt(0)) + text.substring(1));
        }
    }

    private List<PlanItem> buildWeekItems(Calendar cal) {
        List<PlanItem> list = new ArrayList<>();
        Calendar ws = getWeekStart(cal);
        for (int i = 0; i < 7; i++) {
            Calendar day = (Calendar) ws.clone();
            day.add(Calendar.DAY_OF_MONTH, i);
            int dow = day.get(Calendar.DAY_OF_WEEK);
            int idx = (dow == Calendar.SUNDAY) ? 6 : dow - Calendar.MONDAY;
            list.add(new PlanItem(day, idx));
        }
        return list;
    }

    private List<PlanItem> buildMonthItems(Calendar cal) {
        List<PlanItem> list = new ArrayList<>();
        Calendar month = (Calendar) cal.clone();
        month.set(Calendar.DAY_OF_MONTH, 1);
        int daysInMonth = month.getActualMaximum(Calendar.DAY_OF_MONTH);
        int lastYear = -1, lastMonth = -1;

        for (int d = 1; d <= daysInMonth; d++) {
            Calendar day = (Calendar) month.clone();
            day.set(Calendar.DAY_OF_MONTH, d);
            int y = day.get(Calendar.YEAR);
            int m = day.get(Calendar.MONTH);

            if (y != lastYear) {
                list.add(new PlanItem(String.valueOf(y), true));
                lastYear = y; lastMonth = -1;
            }
            if (m != lastMonth) {
                SimpleDateFormat sdf = new SimpleDateFormat("LLLL", new Locale("pl"));
                list.add(new PlanItem(sdf.format(day.getTime()).toUpperCase(new Locale("pl")), false));
                lastMonth = m;
            }

            int dow = day.get(Calendar.DAY_OF_WEEK);
            int idx = (dow == Calendar.SUNDAY) ? 6 : dow - Calendar.MONDAY;
            list.add(new PlanItem(day, idx));
        }
        return list;
    }

    private Calendar getWeekStart(Calendar cal) {
        Calendar ws = (Calendar) cal.clone();
        int dow  = ws.get(Calendar.DAY_OF_WEEK);
        int diff = (dow == Calendar.SUNDAY) ? -6 : Calendar.MONDAY - dow;
        ws.add(Calendar.DAY_OF_MONTH, diff);
        return ws;
    }

    private void showDayPreview(Calendar day, int dowIndex) {
        if (!isAdded() || getActivity() == null || getActivity().isFinishing()) return;

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;
        String email = user.getEmail();
        if (email == null) return;

        Calendar startOfDay = (Calendar) day.clone();
        startOfDay.set(Calendar.HOUR_OF_DAY, 0);
        startOfDay.set(Calendar.MINUTE, 0);
        startOfDay.set(Calendar.SECOND, 0);
        startOfDay.set(Calendar.MILLISECOND, 0);
        final long startMs = startOfDay.getTimeInMillis();

        Calendar endOfDay = (Calendar) startOfDay.clone();
        endOfDay.add(Calendar.DAY_OF_MONTH, 1);
        final long endMs = endOfDay.getTimeInMillis();

        BottomSheetDialog dialog = new BottomSheetDialog(requireContext());
        View v = LayoutInflater.from(requireContext())
                .inflate(R.layout.dialog_day_events, null);
        dialog.setContentView(v);

        SimpleDateFormat sdf = new SimpleDateFormat("d MMMM yyyy", new Locale("pl"));
        ((TextView) v.findViewById(R.id.tvDialogDate)).setText(sdf.format(day.getTime()));
        ((TextView) v.findViewById(R.id.tvDialogDow)).setText(DAY_NAMES_FULL[dowIndex]);

        LinearLayout eventsListContainer = v.findViewById(R.id.eventsListContainer);
        TextView tvNoEvents              = v.findViewById(R.id.tvNoEvents);

        FirebaseFirestore.getInstance()
                .collection("plans")
                .whereEqualTo("email", email)
                .get()
                .addOnSuccessListener(query -> {
                    SimpleDateFormat timeFmt = new SimpleDateFormat("HH:mm", new Locale("pl"));
                    int count = 0;

                    for (QueryDocumentSnapshot doc : query) {
                        com.google.firebase.Timestamp ts = doc.getTimestamp("dueTime");
                        if (ts == null) continue;
                        long docMs = ts.toDate().getTime();
                        if (docMs < startMs || docMs >= endMs) continue;

                        count++;
                        final String planId = doc.getId();

                        String title = doc.getString("title");
                        if (title == null || title.isEmpty()) title = doc.getString("content");
                        if (title == null || title.isEmpty()) title = "—";

                        View row = LayoutInflater.from(requireContext())
                                .inflate(R.layout.item_today_event, eventsListContainer, false);
                        ((TextView) row.findViewById(R.id.tvEventTitle)).setText(title);
                        ((TextView) row.findViewById(R.id.tvEventTime)).setText(timeFmt.format(ts.toDate()));

                        row.setOnClickListener(vv -> {
                            dialog.dismiss();
                            openPlanDetail(planId);
                        });

                        eventsListContainer.addView(row);
                    }

                    tvNoEvents.setVisibility(count == 0 ? View.VISIBLE : View.GONE);
                });

        dialog.show();
    }

    private void openPlanDetail(String planId) {
        if (!isAdded() || getActivity() == null) return;
        Intent intent = new Intent(requireActivity(), PlanDetailActivity.class);
        intent.putExtra("plan_id", planId);
        startActivity(intent);
    }

    private void openAddPlanDialog(Calendar day) {
        if (!isAdded() || getActivity() == null || getActivity().isFinishing()) return;
        AddPlanDialog dialog = AddPlanDialog.newInstance(
                day.get(Calendar.YEAR),
                day.get(Calendar.MONTH),
                day.get(Calendar.DAY_OF_MONTH)
        );
        dialog.setOnPlanAddedListener(() -> loadData(0));
        dialog.show(getChildFragmentManager(), "AddPlanDialog");
    }

    private int dpToPx(int dp) {
        if (!isAdded()) return 0;
        return Math.round(dp * requireContext().getResources().getDisplayMetrics().density);
    }
}