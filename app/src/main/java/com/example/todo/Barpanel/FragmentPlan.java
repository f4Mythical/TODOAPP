package com.example.todo.Barpanel;

import android.animation.ObjectAnimator;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.todo.R;
import com.google.android.material.bottomsheet.BottomSheetDialog;

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

    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    private static final String[] DAY_NAMES_FULL = {
            "Poniedziałek","Wtorek","Środa","Czwartek","Piątek","Sobota","Niedziela"};

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_plan, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

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
                showEventsDialog(day, dowIndex);
            }
            @Override
            public void onLongClick(Calendar day, int dowIndex) {
                currentCalendar.set(Calendar.YEAR,         day.get(Calendar.YEAR));
                currentCalendar.set(Calendar.MONTH,        day.get(Calendar.MONTH));
                currentCalendar.set(Calendar.DAY_OF_MONTH, day.get(Calendar.DAY_OF_MONTH));
                // TODO: OKIENKO POJAWIENIA
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
    public void onDestroy() {
        super.onDestroy();
        executor.shutdownNow();
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
        int targetX = toWeek ? 0 : dpToPx(88);
        ObjectAnimator.ofFloat(toggleIndicator, "translationX",
                toggleIndicator.getTranslationX(), targetX).setDuration(220).start();
        btnWeekLabel.setTextColor(ContextCompat.getColor(requireContext(),
                toWeek ? R.color.cream : R.color.brown_medium));
        btnMonthLabel.setTextColor(ContextCompat.getColor(requireContext(),
                toWeek ? R.color.brown_medium : R.color.cream));
    }

    private void loadData(int direction) {
        updateHeader();
        final Calendar snap    = (Calendar) currentCalendar.clone();
        final boolean weekMode = isWeekMode;

        executor.execute(() -> {
            if (!isAdded()) return;
            List<PlanItem> items = weekMode ? buildWeekItems(snap) : buildMonthItems(snap);
            mainHandler.post(() -> {
                if (!isAdded()) return;
                adapter.setItems(items, snap);
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
                list.add(new PlanItem(
                        sdf.format(day.getTime()).toUpperCase(new Locale("pl")), false));
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

    private void showEventsDialog(Calendar day, int dowIndex) {
        BottomSheetDialog dialog = new BottomSheetDialog(requireContext());
        View v = LayoutInflater.from(requireContext())
                .inflate(R.layout.dialog_day_events, null);
        dialog.setContentView(v);
        SimpleDateFormat sdf = new SimpleDateFormat("d MMMM yyyy", new Locale("pl"));
        ((TextView) v.findViewById(R.id.tvDialogDate)).setText(sdf.format(day.getTime()));
        ((TextView) v.findViewById(R.id.tvDialogDow)).setText(DAY_NAMES_FULL[dowIndex]);
        dialog.show();
    }

    private int dpToPx(int dp) {
        return Math.round(dp * requireContext().getResources().getDisplayMetrics().density);
    }
}