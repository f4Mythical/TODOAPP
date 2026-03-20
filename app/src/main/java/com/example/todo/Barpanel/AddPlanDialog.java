package com.example.todo.Barpanel;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.todo.FirestoreHelper;
import com.example.todo.R;
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

    private OnPlanAddedListener listener;

    private TextInputEditText etTitle, etContent;
    private TextInputLayout   tilContent;
    private TextView          tvSelectedDate;
    private LinearLayout      hoursGrid;
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
        tilContent           = view.findViewById(R.id.tilPlanContent);
        tvSelectedDate       = view.findViewById(R.id.tvSelectedDate);
        hoursGrid            = view.findViewById(R.id.hoursGrid);
        notifChipsContainer  = view.findViewById(R.id.notifChipsContainer);
        btnLow               = view.findViewById(R.id.btnPriorityLow);
        btnMedium            = view.findViewById(R.id.btnPriorityMedium);
        btnHigh              = view.findViewById(R.id.btnPriorityHigh);
        btnSave              = view.findViewById(R.id.btnSavePlan);
        progressSave         = view.findViewById(R.id.progressSave);

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
                },
                cal.get(Calendar.YEAR),
                cal.get(Calendar.MONTH),
                cal.get(Calendar.DAY_OF_MONTH)
        ).show();
    }

    private void updateDateLabel() {
        if (selectedDueDate == null) {
            tvSelectedDate.setText(getString(R.string.add_plan_no_date));
        } else {
            java.text.SimpleDateFormat sdf =
                    new java.text.SimpleDateFormat("d MMMM yyyy", new java.util.Locale("pl"));
            tvSelectedDate.setText(sdf.format(selectedDueDate.getTime()));
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
        tv.setTextColor(androidx.core.content.ContextCompat.getColor(requireContext(), R.color.brown_primary));
        tv.setBackground(androidx.core.content.ContextCompat.getDrawable(requireContext(), R.drawable.hour_chip_bg));
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
            chip.setBackground(androidx.core.content.ContextCompat.getDrawable(requireContext(), R.drawable.hour_chip_selected_bg));
            chip.setTextColor(androidx.core.content.ContextCompat.getColor(requireContext(), R.color.cream));
        } else {
            chip.setBackground(androidx.core.content.ContextCompat.getDrawable(requireContext(), R.drawable.hour_chip_bg));
            chip.setTextColor(androidx.core.content.ContextCompat.getColor(requireContext(), R.color.brown_primary));
        }
    }

    private void showMinutePicker(int hour, TextView originChip) {
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(requireContext());
        builder.setTitle(hour + ":??  — wybierz minuty");

        LinearLayout grid = new LinearLayout(requireContext());
        grid.setOrientation(LinearLayout.VERTICAL);
        int padding = dpToPx(16);
        grid.setPadding(padding, padding, padding, padding);

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
                tv.setTextColor(androidx.core.content.ContextCompat.getColor(requireContext(), R.color.brown_primary));
                tv.setBackground(androidx.core.content.ContextCompat.getDrawable(requireContext(), R.drawable.hour_chip_bg));
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
        for (int[] t : notifTimes) {
            TextView chip = new TextView(requireContext());
            chip.setText(String.format(java.util.Locale.getDefault(), "%d:%02d", t[0], t[1]));
            chip.setTextSize(12f);
            chip.setTextColor(androidx.core.content.ContextCompat.getColor(requireContext(), R.color.cream));
            chip.setBackground(androidx.core.content.ContextCompat.getDrawable(requireContext(), R.drawable.hour_chip_selected_bg));
            int ph = dpToPx(6), pv = dpToPx(4);
            chip.setPadding(ph, pv, ph, pv);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            lp.setMargins(0, 0, dpToPx(6), 0);
            chip.setLayoutParams(lp);
            final int[] timeRef = t;
            chip.setOnClickListener(v -> {
                notifTimes.remove(timeRef);
                refreshNotifChips();
                rebuildHourGrid();
            });
            notifChipsContainer.addView(chip);
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
        int active       = androidx.core.content.ContextCompat.getColor(requireContext(), R.color.brown_primary);
        int inactive     = androidx.core.content.ContextCompat.getColor(requireContext(), R.color.cream);
        int activeText   = androidx.core.content.ContextCompat.getColor(requireContext(), R.color.cream);
        int inactiveText = androidx.core.content.ContextCompat.getColor(requireContext(), R.color.brown_medium);
        resetPriorityBtn(btnLow,    "low".equals(p),    active, inactive, activeText, inactiveText);
        resetPriorityBtn(btnMedium, "medium".equals(p), active, inactive, activeText, inactiveText);
        resetPriorityBtn(btnHigh,   "high".equals(p),   active, inactive, activeText, inactiveText);
    }

    private void resetPriorityBtn(Button btn, boolean isActive,
                                  int active, int inactive, int activeText, int inactiveText) {
        btn.setBackgroundTintList(android.content.res.ColorStateList.valueOf(isActive ? active : inactive));
        btn.setTextColor(isActive ? activeText : inactiveText);
    }

    private void savePlan() {
        String title   = etTitle.getText()   != null ? etTitle.getText().toString().trim()   : "";
        String content = etContent.getText() != null ? etContent.getText().toString().trim() : "";

        if (selectedDueDate == null) {
            Toast.makeText(requireContext(), getString(R.string.add_plan_error_no_date), Toast.LENGTH_SHORT).show();
            return;
        }

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;

        String email = user.getEmail() != null ? user.getEmail() : "";
        Timestamp dueTime = new Timestamp(selectedDueDate.getTime());

        Timestamp notifTime = null;
        if (!notifTimes.isEmpty()) {
            Calendar notifCal = (Calendar) selectedDueDate.clone();
            notifCal.set(Calendar.HOUR_OF_DAY, notifTimes.get(0)[0]);
            notifCal.set(Calendar.MINUTE,      notifTimes.get(0)[1]);
            notifCal.set(Calendar.SECOND, 0);
            notifTime = new Timestamp(notifCal.getTime());
        }

        setSaving(true);

        final Timestamp finalNotifTime = notifTime;
        final String    finalTitle     = title;
        final String    finalContent   = content;

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
                            finalTitle.isEmpty() ? null : finalTitle,
                            finalContent,
                            finalNotifTime,
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