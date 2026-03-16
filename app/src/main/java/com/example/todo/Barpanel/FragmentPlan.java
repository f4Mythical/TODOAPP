package com.example.todo.Barpanel;

import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.GridLayout;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.example.todo.R;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.button.MaterialButtonToggleGroup;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public class FragmentPlan extends Fragment {

    private Calendar currentCalendar;
    private Calendar today;
    private boolean isWeekMode = true;

    private TextView tvMonthYear;
    private FrameLayout calendarContainer;

    private static final String[] DAY_NAMES_SHORT = {"Pn", "Wt", "Śr", "Cz", "Pt", "Sb", "Nd"};
    private static final String[] DAY_NAMES_FULL  = {"Poniedziałek", "Wtorek", "Środa", "Czwartek", "Piątek", "Sobota", "Niedziela"};

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_plan, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        today = Calendar.getInstance();
        currentCalendar = Calendar.getInstance();

        tvMonthYear = view.findViewById(R.id.tvMonthYear);
        calendarContainer = view.findViewById(R.id.calendarContainer);

        ImageButton btnPrev = view.findViewById(R.id.btnPrev);
        ImageButton btnNext = view.findViewById(R.id.btnNext);
        MaterialButtonToggleGroup toggle = view.findViewById(R.id.toggleViewMode);

        btnPrev.setOnClickListener(v -> {
            if (isWeekMode) currentCalendar.add(Calendar.WEEK_OF_YEAR, -1);
            else currentCalendar.add(Calendar.MONTH, -1);
            render();
        });

        btnNext.setOnClickListener(v -> {
            if (isWeekMode) currentCalendar.add(Calendar.WEEK_OF_YEAR, 1);
            else currentCalendar.add(Calendar.MONTH, 1);
            render();
        });

        toggle.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (!isChecked) return;
            isWeekMode = checkedId == R.id.btnWeek;
            render();
        });

        toggle.check(R.id.btnWeek);
        render();
    }

    private void render() {
        updateHeader();
        calendarContainer.removeAllViews();
        View calView = isWeekMode ? buildWeekView() : buildMonthView();
        calView.setAlpha(0f);
        calendarContainer.addView(calView);
        calView.animate().alpha(1f).setDuration(200).start();
    }

    private void updateHeader() {
        SimpleDateFormat sdf = new SimpleDateFormat("LLLL yyyy", new Locale("pl"));
        String text = sdf.format(currentCalendar.getTime());
        tvMonthYear.setText(Character.toUpperCase(text.charAt(0)) + text.substring(1));
    }

    private View buildWeekView() {
        LinearLayout list = new LinearLayout(requireContext());
        list.setOrientation(LinearLayout.VERTICAL);
        list.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));

        Calendar weekStart = (Calendar) currentCalendar.clone();
        int dow = weekStart.get(Calendar.DAY_OF_WEEK);
        int diff = (dow == Calendar.SUNDAY) ? -6 : Calendar.MONDAY - dow;
        weekStart.add(Calendar.DAY_OF_MONTH, diff);

        for (int i = 0; i < 7; i++) {
            final Calendar day = (Calendar) weekStart.clone();
            day.add(Calendar.DAY_OF_MONTH, i);

            View row = LayoutInflater.from(requireContext())
                    .inflate(R.layout.item_day_week, list, false);

            TextView tvName = row.findViewById(R.id.tvDayName);
            TextView tvNum  = row.findViewById(R.id.tvDayNum);
            FrameLayout circle = (FrameLayout) tvNum.getParent();

            tvName.setText(DAY_NAMES_SHORT[i]);
            tvNum.setText(String.valueOf(day.get(Calendar.DAY_OF_MONTH)));

            boolean isToday    = isSameDay(day, today);
            boolean isSelected = isSameDay(day, currentCalendar);

            if (isSelected) {
                circle.setBackground(ContextCompat.getDrawable(requireContext(), R.drawable.day_circle_selected));
                tvNum.setTextColor(ContextCompat.getColor(requireContext(), R.color.cream));
            } else if (isToday) {
                circle.setBackground(ContextCompat.getDrawable(requireContext(), R.drawable.day_circle_today));
                tvNum.setTextColor(ContextCompat.getColor(requireContext(), R.color.brown_primary));
            }

            LinearLayout eventsContainer = row.findViewById(R.id.eventsContainer);
            TextView tvEmpty = new TextView(requireContext());
            tvEmpty.setText(getString(R.string.plan_no_events));
            tvEmpty.setTextSize(13f);
            tvEmpty.setTextColor(ContextCompat.getColor(requireContext(), R.color.brown_light));
            eventsContainer.addView(tvEmpty);

            int finalI = i;
            row.setOnClickListener(v -> showDayDialog(day, finalI));

            list.addView(row);

            if (i < 6) {
                View divider = new View(requireContext());
                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, dpToPx(1));
                lp.setMarginStart(dpToPx(80));
                divider.setLayoutParams(lp);
                divider.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.cream_dark));
                list.addView(divider);
            }
        }

        return list;
    }

    private View buildMonthView() {
        LinearLayout container = new LinearLayout(requireContext());
        container.setOrientation(LinearLayout.VERTICAL);
        container.setPadding(dpToPx(8), 0, dpToPx(8), 0);

        LinearLayout headers = new LinearLayout(requireContext());
        headers.setOrientation(LinearLayout.HORIZONTAL);
        headers.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dpToPx(32)));

        for (String label : DAY_NAMES_SHORT) {
            TextView tv = new TextView(requireContext());
            tv.setText(label);
            tv.setTextColor(ContextCompat.getColor(requireContext(), R.color.brown_medium));
            tv.setTextSize(11f);
            tv.setGravity(Gravity.CENTER);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0,
                    ViewGroup.LayoutParams.MATCH_PARENT, 1f);
            tv.setLayoutParams(lp);
            headers.addView(tv);
        }
        container.addView(headers);

        GridLayout grid = new GridLayout(requireContext());
        grid.setColumnCount(7);
        grid.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));

        Calendar monthStart = (Calendar) currentCalendar.clone();
        monthStart.set(Calendar.DAY_OF_MONTH, 1);

        int firstDow = monthStart.get(Calendar.DAY_OF_WEEK);
        int offset = (firstDow == Calendar.SUNDAY) ? 6 : firstDow - Calendar.MONDAY;
        int daysInMonth = monthStart.getActualMaximum(Calendar.DAY_OF_MONTH);
        int totalCells = (int) Math.ceil((offset + daysInMonth) / 7.0) * 7;

        for (int i = 0; i < totalCells; i++) {
            int dayNum = i - offset + 1;

            GridLayout.LayoutParams lp = new GridLayout.LayoutParams();
            lp.width = 0;
            lp.height = dpToPx(52);
            lp.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f);

            if (dayNum < 1 || dayNum > daysInMonth) {
                View empty = new View(requireContext());
                empty.setLayoutParams(lp);
                grid.addView(empty);
            } else {
                final Calendar day = (Calendar) monthStart.clone();
                day.set(Calendar.DAY_OF_MONTH, dayNum);
                final int dowIndex = (i % 7);

                LinearLayout cell = new LinearLayout(requireContext());
                cell.setOrientation(LinearLayout.VERTICAL);
                cell.setGravity(Gravity.TOP | Gravity.CENTER_HORIZONTAL);
                cell.setPadding(dpToPx(2), dpToPx(4), dpToPx(2), dpToPx(4));
                cell.setLayoutParams(lp);
                cell.setBackground(ContextCompat.getDrawable(requireContext(),
                        android.R.drawable.list_selector_background));

                FrameLayout circleWrap = new FrameLayout(requireContext());
                int size = dpToPx(30);
                LinearLayout.LayoutParams clp = new LinearLayout.LayoutParams(size, size);
                circleWrap.setLayoutParams(clp);

                boolean isToday    = isSameDay(day, today);
                boolean isSelected = isSameDay(day, currentCalendar);

                if (isSelected) {
                    circleWrap.setBackground(ContextCompat.getDrawable(requireContext(), R.drawable.day_circle_selected));
                } else if (isToday) {
                    circleWrap.setBackground(ContextCompat.getDrawable(requireContext(), R.drawable.day_circle_today));
                }

                TextView tvNum = new TextView(requireContext());
                tvNum.setText(String.valueOf(dayNum));
                tvNum.setTextSize(13f);
                tvNum.setGravity(Gravity.CENTER);
                tvNum.setLayoutParams(new FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT));
                tvNum.setTextColor(isSelected
                        ? ContextCompat.getColor(requireContext(), R.color.cream)
                        : ContextCompat.getColor(requireContext(), R.color.brown_primary));

                circleWrap.addView(tvNum);
                cell.addView(circleWrap);

                cell.setOnClickListener(v -> {
                    currentCalendar.set(Calendar.DAY_OF_MONTH, day.get(Calendar.DAY_OF_MONTH));
                    showDayDialog(day, dowIndex);
                    render();
                });

                grid.addView(cell);
            }
        }

        container.addView(grid);
        return container;
    }

    private void showDayDialog(Calendar day, int dowIndex) {
        BottomSheetDialog dialog = new BottomSheetDialog(requireContext());
        View v = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_day, null);
        dialog.setContentView(v);

        SimpleDateFormat sdf = new SimpleDateFormat("d MMMM yyyy", new Locale("pl"));
        TextView tvDate = v.findViewById(R.id.tvDialogDate);
        TextView tvDow  = v.findViewById(R.id.tvDialogDayOfWeek);
        TextView tvEvts = v.findViewById(R.id.tvDialogEvents);

        tvDate.setText(sdf.format(day.getTime()));
        tvDow.setText(DAY_NAMES_FULL[dowIndex]);
        tvEvts.setText(getString(R.string.plan_no_events));

        v.findViewById(R.id.btnDialogAdd).setOnClickListener(btn -> {
            dialog.dismiss();
            // TODO: OKIENKO POJAWIENIA
        });

        dialog.show();
    }

    private boolean isSameDay(Calendar a, Calendar b) {
        return a.get(Calendar.YEAR) == b.get(Calendar.YEAR)
                && a.get(Calendar.MONTH) == b.get(Calendar.MONTH)
                && a.get(Calendar.DAY_OF_MONTH) == b.get(Calendar.DAY_OF_MONTH);
    }

    private int dpToPx(int dp) {
        return Math.round(dp * requireContext().getResources().getDisplayMetrics().density);
    }
}