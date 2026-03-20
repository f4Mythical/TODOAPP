package com.example.todo.Barpanel;

import android.app.DatePickerDialog;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.example.todo.FirestoreHelper;
import com.example.todo.R;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class AddPlanDialog extends BottomSheetDialogFragment {

    public interface OnPlanAddedListener {
        void onPlanAdded();
    }

    private static final String ARG_DATE_YEAR  = "year";
    private static final String ARG_DATE_MONTH = "month";
    private static final String ARG_DATE_DAY   = "day";

    private static final int CHIPS_PER_ROW = 6;

    private OnPlanAddedListener listener;

    private TextInputEditText etTitle, etContent;
    private TextInputLayout   tilPlanTitle, tilContent;
    private TextView          tvSelectedDate;
    private TextView          tvDateError;
    private LinearLayout      hoursGrid;
    private ScrollView        notifChipsScroll;
    private LinearLayout      notifChipsContainer;
    private Button            btnLow, btnMedium, btnHigh;
    private Button            btnSave;
    private View              progressSave;

    private Calendar selectedDueDate = null;
    private String   selectedPriority = "medium";
    private final List<int[]> notifTimes = new ArrayList<>();
    private int pendingHour = -1;

    public static AddPlanDialog newInstance(int year, int month, int day) {
        AddPlanDialog f = new AddPlanDialog();
        Bundle b = new Bundle();
        b.putInt(ARG_DATE_YEAR,  year);
        b.putInt(ARG_DATE_MONTH, month);
        b.putInt(ARG_DATE_DAY,   day);
        f.setArguments(b);
        return f;
    }

    public void setOnPlanAddedListener(OnPlanAddedListener l) {
        this.listener = l;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.bottom_sheet_add_plan, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        etTitle              = view.findViewById(R.id.etPlanTitle);
        etContent            = view.findViewById(R.id.etPlanContent);
        tilPlanTitle         = view.findViewById(R.id.tilPlanTitle);
        tilContent           = view.findViewById(R.id.tilPlanContent);
        tvSelectedDate       = view.findViewById(R.id.tvSelectedDate);
        tvDateError          = view.findViewById(R.id.tvDateError);
        hoursGrid            = view.findViewById(R.id.hoursGrid);
        notifChipsScroll     = view.findViewById(R.id.notifChipsScroll);
        notifChipsContainer  = view.findViewById(R.id.notifChipsContainer);
        btnLow               = view.findViewById(R.id.btnPriorityLow);
        btnMedium            = view.findViewById(R.id.btnPriorityMedium);
        btnHigh              = view.findViewById(R.id.btnPriorityHigh);
        btnSave              = view.findViewById(R.id.btnSavePlan);
        progressSave         = view.findViewById(R.id.progressSave);

        applyHintColors();

        Bundle args = getArguments();
        if (args != null) {
            int y = args.getInt(ARG_DATE_YEAR,  -1);
            int m = args.getInt(ARG_DATE_MONTH, -1);
            int d = args.getInt(ARG_DATE_DAY,   -1);
            if (y > 0 && m >= 0 && d > 0) {
                selectedDueDate = Calendar.getInstance();
                selectedDueDate.set(y, m, d, 23, 59, 0);
                selectedDueDate.set(Calendar.MILLISECOND, 0);
                updateDateLabel();
            }
        }

        view.findViewById(R.id.btnPickDate).setOnClickListener(v -> openDatePicker());

        btnLow.setOnClickListener(v    -> selectPriority("low"));
        btnMedium.setOnClickListener(v -> selectPriority("medium"));
        btnHigh.setOnClickListener(v   -> selectPriority("high"));
        selectPriority("medium");

        buildHoursGrid();

        btnSave.setOnClickListener(v -> savePlan());
    }

    @Override
    public void onStart() {
        super.onStart();
        BottomSheetDialog dialog = (BottomSheetDialog) getDialog();
        if (dialog == null) return;

        FrameLayout bottomSheet = dialog.findViewById(
                com.google.android.material.R.id.design_bottom_sheet);
        if (bottomSheet == null) return;

        bottomSheet.getLayoutParams().height = ViewGroup.LayoutParams.MATCH_PARENT;
        bottomSheet.requestLayout();

        BottomSheetBehavior<FrameLayout> behavior = BottomSheetBehavior.from(bottomSheet);
        behavior.setState(BottomSheetBehavior.STATE_EXPANDED);
        behavior.setSkipCollapsed(true);
        behavior.setPeekHeight(0);
    }

    private void applyHintColors() {
        ColorStateList hintColor = ColorStateList.valueOf(
                ContextCompat.getColor(requireContext(), R.color.brown_medium));
        if (tilPlanTitle != null) tilPlanTitle.setDefaultHintTextColor(hintColor);
        if (tilContent   != null) tilContent.setDefaultHintTextColor(hintColor);
    }

    private void openDatePicker() {
        Calendar cal = selectedDueDate != null ? selectedDueDate : Calendar.getInstance();
        new DatePickerDialog(requireContext(),
                (dp, y, m, d) -> {
                    if (selectedDueDate == null) selectedDueDate = Calendar.getInstance();
                    selectedDueDate.set(y, m, d);
                    if (selectedDueDate.get(Calendar.HOUR_OF_DAY) == 0
                            && selectedDueDate.get(Calendar.MINUTE) == 0) {
                        selectedDueDate.set(Calendar.HOUR_OF_DAY, 23);
                        selectedDueDate.set(Calendar.MINUTE, 59);
                    }
                    updateDateLabel();
                    if (tvDateError != null) tvDateError.setVisibility(View.GONE);
                },
                cal.get(Calendar.YEAR),
                cal.get(Calendar.MONTH),
                cal.get(Calendar.DAY_OF_MONTH)
        ).show();
    }

    private void updateDateLabel() {
        if (selectedDueDate == null) {
            tvSelectedDate.setText(getString(R.string.add_plan_no_date));
            tvSelectedDate.setTextColor(ContextCompat.getColor(requireContext(), R.color.brown_medium));
        } else {
            java.text.SimpleDateFormat sdf =
                    new java.text.SimpleDateFormat("d MMMM yyyy", new java.util.Locale("pl"));
            tvSelectedDate.setText(sdf.format(selectedDueDate.getTime()));
            tvSelectedDate.setTextColor(ContextCompat.getColor(requireContext(), R.color.brown_primary));
        }
    }

    private void buildHoursGrid() {
        hoursGrid.removeAllViews();
        for (int row = 0; row < 4; row++) {
            LinearLayout rowLayout = new LinearLayout(requireContext());
            rowLayout.setOrientation(LinearLayout.HORIZONTAL);
            rowLayout.setLayoutParams(new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT));

            for (int col = 0; col < 6; col++) {
                int hour = row * 6 + col + 1;
                if (hour > 24) break;
                TextView tv = buildHourChip(hour);
                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, dpToPx(40), 1f);
                lp.setMargins(dpToPx(3), dpToPx(3), dpToPx(3), dpToPx(3));
                tv.setLayoutParams(lp);
                rowLayout.addView(tv);
            }
            hoursGrid.addView(rowLayout);
        }
    }

    private TextView buildHourChip(int hour) {
        TextView tv = new TextView(requireContext());
        tv.setText(String.valueOf(hour));
        tv.setGravity(android.view.Gravity.CENTER);
        tv.setTextSize(13f);
        tv.setTextColor(ContextCompat.getColor(requireContext(), R.color.brown_primary));
        tv.setBackground(ContextCompat.getDrawable(requireContext(), R.drawable.hour_chip_bg));
        tv.setOnClickListener(v -> onHourSelected(hour, 0, tv));
        tv.setOnLongClickListener(v -> {
            pendingHour = hour;
            showMinutePicker(hour, tv);
            return true;
        });
        return tv;
    }

    private void onHourSelected(int hour, int minute, TextView chip) {
        for (int i = 0; i < notifTimes.size(); i++) {
            if (notifTimes.get(i)[0] == hour && notifTimes.get(i)[1] == minute) {
                notifTimes.remove(i);
                refreshNotifChips();
                updateHourChipState(chip, false);
                return;
            }
        }
        notifTimes.add(new int[]{hour, minute});
        refreshNotifChips();
        updateHourChipState(chip, true);
    }

    private void updateHourChipState(TextView chip, boolean selected) {
        if (selected) {
            chip.setBackground(ContextCompat.getDrawable(requireContext(), R.drawable.hour_chip_selected_bg));
            chip.setTextColor(ContextCompat.getColor(requireContext(), R.color.cream));
        } else {
            chip.setBackground(ContextCompat.getDrawable(requireContext(), R.drawable.hour_chip_bg));
            chip.setTextColor(ContextCompat.getColor(requireContext(), R.color.brown_primary));
        }
    }

    private void showMinutePicker(int hour, TextView originChip) {
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(requireContext());
        builder.setTitle(hour + ":??  — wybierz minuty");

        LinearLayout grid = new LinearLayout(requireContext());
        grid.setOrientation(LinearLayout.VERTICAL);
        int padding = dpToPx(16);
        grid.setPadding(padding, padding, padding, padding);
        grid.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.background));

        int[] minutes = {0, 5, 10, 15, 20, 25, 30, 35, 40, 45, 50, 55};
        for (int row = 0; row < 2; row++) {
            LinearLayout rowL = new LinearLayout(requireContext());
            rowL.setOrientation(LinearLayout.HORIZONTAL);
            for (int col = 0; col < 6; col++) {
                int min = minutes[row * 6 + col];
                TextView tv = new TextView(requireContext());
                tv.setText(String.format(java.util.Locale.getDefault(), ":%02d", min));
                tv.setGravity(android.view.Gravity.CENTER);
                tv.setTextSize(13f);
                tv.setTextColor(ContextCompat.getColor(requireContext(), R.color.brown_primary));
                tv.setBackground(ContextCompat.getDrawable(requireContext(), R.drawable.hour_chip_bg));
                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, dpToPx(40), 1f);
                lp.setMargins(dpToPx(3), dpToPx(3), dpToPx(3), dpToPx(3));
                tv.setLayoutParams(lp);
                rowL.addView(tv);
            }
            grid.addView(rowL);
        }

        builder.setView(grid);
        builder.setNegativeButton(android.R.string.cancel, null);
        android.app.AlertDialog dialog = builder.create();

        dialog.getWindow().setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(
                ContextCompat.getColor(requireContext(), R.color.background)));
        for (int row = 0; row < grid.getChildCount(); row++) {
            LinearLayout rowL = (LinearLayout) grid.getChildAt(row);
            for (int col = 0; col < rowL.getChildCount(); col++) {
                TextView tv = (TextView) rowL.getChildAt(col);
                int min = minutes[row * 6 + col];
                tv.setOnClickListener(v -> {
                    dialog.dismiss();
                    onHourSelected(hour, min, originChip);
                });
            }
        }

        dialog.show();
    }

    private void refreshNotifChips() {
        notifChipsContainer.removeAllViews();

        if (notifTimes.isEmpty()) {
            if (notifChipsScroll != null) notifChipsScroll.setVisibility(View.GONE);
            return;
        }

        if (notifChipsScroll != null) notifChipsScroll.setVisibility(View.VISIBLE);

        int chipMargin = dpToPx(4);
        LinearLayout currentRow = null;

        for (int i = 0; i < notifTimes.size(); i++) {
            if (i % CHIPS_PER_ROW == 0) {
                currentRow = new LinearLayout(requireContext());
                currentRow.setOrientation(LinearLayout.HORIZONTAL);
                LinearLayout.LayoutParams rowLp = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT);
                rowLp.setMargins(0, 0, 0, chipMargin);
                currentRow.setLayoutParams(rowLp);
                notifChipsContainer.addView(currentRow);
            }

            final int[] t = notifTimes.get(i);
            TextView chip = new TextView(requireContext());
            chip.setText(String.format(java.util.Locale.getDefault(), "%d:%02d", t[0], t[1]));
            chip.setTextSize(11f);
            chip.setTextColor(ContextCompat.getColor(requireContext(), R.color.cream));
            chip.setBackground(ContextCompat.getDrawable(requireContext(), R.drawable.hour_chip_selected_bg));
            chip.setGravity(android.view.Gravity.CENTER);
            int ph = dpToPx(4), pv = dpToPx(5);
            chip.setPadding(ph, pv, ph, pv);

            boolean isLastInRow = (i % CHIPS_PER_ROW == CHIPS_PER_ROW - 1);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0,
                    LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
            lp.setMargins(0, 0, isLastInRow ? 0 : chipMargin, 0);
            chip.setLayoutParams(lp);

            chip.setOnClickListener(v -> {
                notifTimes.remove(t);
                refreshNotifChips();
                rebuildHourGrid();
            });

            currentRow.addView(chip);
        }

        int remainder = notifTimes.size() % CHIPS_PER_ROW;
        if (remainder != 0 && currentRow != null) {
            int empty = CHIPS_PER_ROW - remainder;
            for (int i = 0; i < empty; i++) {
                View spacer = new View(requireContext());
                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0,
                        LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
                lp.setMargins(0, 0, (i < empty - 1) ? chipMargin : 0, 0);
                spacer.setLayoutParams(lp);
                currentRow.addView(spacer);
            }
        }

        if (notifChipsScroll != null) {
            notifChipsScroll.post(() -> notifChipsScroll.fullScroll(ScrollView.FOCUS_DOWN));
        }
    }

    private void rebuildHourGrid() {
        buildHoursGrid();
        for (int[] t : notifTimes) {
            int hour   = t[0];
            int rowIdx = (hour - 1) / 6;
            int colIdx = (hour - 1) % 6;
            if (rowIdx < hoursGrid.getChildCount()) {
                LinearLayout row = (LinearLayout) hoursGrid.getChildAt(rowIdx);
                if (colIdx < row.getChildCount()) {
                    updateHourChipState((TextView) row.getChildAt(colIdx), true);
                }
            }
        }
    }

    private void selectPriority(String p) {
        selectedPriority = p;
        int active       = ContextCompat.getColor(requireContext(), R.color.brown_primary);
        int inactive     = ContextCompat.getColor(requireContext(), R.color.cream);
        int activeText   = ContextCompat.getColor(requireContext(), R.color.cream);
        int inactiveText = ContextCompat.getColor(requireContext(), R.color.brown_medium);
        resetPriorityBtn(btnLow,    "low".equals(p),    active, inactive, activeText, inactiveText);
        resetPriorityBtn(btnMedium, "medium".equals(p), active, inactive, activeText, inactiveText);
        resetPriorityBtn(btnHigh,   "high".equals(p),   active, inactive, activeText, inactiveText);
    }

    private void resetPriorityBtn(Button btn, boolean isActive,
                                  int active, int inactive, int activeText, int inactiveText) {
        btn.setBackgroundTintList(ColorStateList.valueOf(isActive ? active : inactive));
        btn.setTextColor(isActive ? activeText : inactiveText);
    }

    private void savePlan() {
        boolean hasError = false;

        String title = etTitle.getText() != null ? etTitle.getText().toString().trim() : "";
        if (title.isEmpty()) {
            tilPlanTitle.setError(getString(R.string.add_plan_error_no_title));
            hasError = true;
        } else {
            tilPlanTitle.setError(null);
        }

        if (selectedDueDate == null) {
            if (tvDateError != null) tvDateError.setVisibility(View.VISIBLE);
            hasError = true;
        } else {
            if (tvDateError != null) tvDateError.setVisibility(View.GONE);
        }

        if (hasError) return;

        String content = etContent.getText() != null ? etContent.getText().toString().trim() : "";

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;

        String email = user.getEmail() != null ? user.getEmail() : "";
        Timestamp dueTime = new Timestamp(selectedDueDate.getTime());

        List<Timestamp> notifTimestamps = new ArrayList<>();
        for (int[] t : notifTimes) {
            Calendar notifCal = (Calendar) selectedDueDate.clone();
            notifCal.set(Calendar.HOUR_OF_DAY, t[0]);
            notifCal.set(Calendar.MINUTE,      t[1]);
            notifCal.set(Calendar.SECOND,      0);
            notifCal.set(Calendar.MILLISECOND, 0);
            notifTimestamps.add(new Timestamp(notifCal.getTime()));
        }

        Timestamp notifTime = notifTimestamps.isEmpty() ? null : notifTimestamps.get(0);

        setSaving(true);

        final List<Timestamp> finalNotifTimestamps = notifTimestamps;
        final Timestamp       finalNotifTime       = notifTime;
        final String          finalTitle           = title;
        final String          finalContent         = content;

        FirebaseFirestore.getInstance()
                .collection("nicks")
                .document(user.getUid())
                .get()
                .addOnSuccessListener(doc -> {
                    String nick = doc.getString("nick");
                    if (nick == null || nick.isEmpty()) nick = email;

                    FirestoreHelper helper = new FirestoreHelper();
                    helper.addPlan(
                            requireContext(),
                            user.getUid(),
                            email,
                            nick,
                            finalTitle,
                            finalContent,
                            finalNotifTime,
                            finalNotifTimestamps,
                            dueTime,
                            selectedPriority
                    );

                    setSaving(false);
                    if (listener != null) listener.onPlanAdded();
                    dismiss();
                })
                .addOnFailureListener(e -> {
                    setSaving(false);
                    Toast.makeText(requireContext(), getString(R.string.add_plan_error_save), Toast.LENGTH_SHORT).show();
                });
    }

    private void setSaving(boolean saving) {
        btnSave.setEnabled(!saving);
        btnSave.setText(saving ? "" : getString(R.string.add_plan_save));
        progressSave.setVisibility(saving ? View.VISIBLE : View.GONE);
    }

    private int dpToPx(int dp) {
        return Math.round(dp * requireContext().getResources().getDisplayMetrics().density);
    }
}