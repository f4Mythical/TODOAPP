package com.example.todo.Barpanel;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;

import com.example.todo.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class PlanAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    public interface OnDayClickListener {
        void onClick(Calendar day, int dowIndex);
        void onLongClick(Calendar day, int dowIndex);
    }

    private List<PlanItem> items = new ArrayList<>();
    private Calendar today;
    private Calendar selected;
    private OnDayClickListener listener;
    private final SimpleDateFormat dateFmt = new SimpleDateFormat("d.MM.yyyy", new Locale("pl"));
    private final SimpleDateFormat timeFmt = new SimpleDateFormat("HH:mm", new Locale("pl"));

    // Cache emaila żeby nie pobierać za każdym razem
    private String cachedEmail = null;

    public PlanAdapter(Calendar today, Calendar selected, OnDayClickListener listener) {
        this.today    = today;
        this.selected = selected;
        this.listener = listener;

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) cachedEmail = user.getEmail();
    }

    public void setItems(List<PlanItem> newItems, Calendar newSelected) {
        this.selected = newSelected;
        DiffUtil.DiffResult diff = DiffUtil.calculateDiff(new DiffUtil.Callback() {
            @Override public int getOldListSize() { return items.size(); }
            @Override public int getNewListSize() { return newItems.size(); }
            @Override public boolean areItemsTheSame(int o, int n) {
                PlanItem a = items.get(o), b = newItems.get(n);
                if (a.type != b.type) return false;
                if (a.type == PlanItem.TYPE_SEPARATOR) return a.separatorText.equals(b.separatorText);
                return isSameDay(a.day, b.day);
            }
            @Override public boolean areContentsTheSame(int o, int n) {
                return areItemsTheSame(o, n);
            }
        });
        items = newItems;
        diff.dispatchUpdatesTo(this);
    }

    @Override public int getItemViewType(int pos) { return items.get(pos).type; }
    @Override public int getItemCount() { return items.size(); }

    @NonNull @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inf = LayoutInflater.from(parent.getContext());
        if (viewType == PlanItem.TYPE_SEPARATOR) {
            View v = inf.inflate(R.layout.item_separator, parent, false);
            return new SepHolder(v);
        }
        View v = inf.inflate(R.layout.item_day_row, parent, false);
        return new DayHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int pos) {
        PlanItem item = items.get(pos);
        if (item.type == PlanItem.TYPE_SEPARATOR) {
            SepHolder h = (SepHolder) holder;
            h.tv.setText(item.separatorText);
            h.tv.setTextSize(item.isYear ? 22f : 13f);
        } else {
            DayHolder h = (DayHolder) holder;
            h.bind(item);
        }
    }

    class DayHolder extends RecyclerView.ViewHolder {
        TextView tvName, tvNum;
        FrameLayout circle;
        LinearLayout eventsContainer;

        DayHolder(View v) {
            super(v);
            tvName          = v.findViewById(R.id.tvDayName);
            tvNum           = v.findViewById(R.id.tvDayNum);
            circle          = v.findViewById(R.id.dayCircle);
            eventsContainer = v.findViewById(R.id.eventsContainer);
        }

        void bind(PlanItem item) {
            android.content.Context ctx = itemView.getContext();

            tvName.setText(item.dowIndex >= 0
                    ? new String[]{"Pn","Wt","Śr","Cz","Pt","Sb","Nd"}[item.dowIndex] : "");
            tvNum.setText(String.valueOf(item.day.get(Calendar.DAY_OF_MONTH)));

            boolean isToday = isSameDay(item.day, today);
            boolean isSel   = isSameDay(item.day, selected);

            circle.setBackground(null);
            tvNum.setTextColor(ContextCompat.getColor(ctx, R.color.brown_primary));

            if (isSel) {
                circle.setBackground(ContextCompat.getDrawable(ctx, R.drawable.day_circle_selected));
                tvNum.setTextColor(ContextCompat.getColor(ctx, R.color.cream));
            } else if (isToday) {
                circle.setBackground(ContextCompat.getDrawable(ctx, R.drawable.day_circle_today));
            }

            eventsContainer.removeAllViews();

            // Data jako placeholder do czasu załadowania
            TextView tvDate = new TextView(ctx);
            tvDate.setText(dateFmt.format(item.day.getTime()));
            tvDate.setTextSize(12f);
            tvDate.setTextColor(ContextCompat.getColor(ctx, R.color.brown_medium));
            eventsContainer.addView(tvDate);

            itemView.setOnClickListener(v -> {
                if (listener != null) listener.onClick(item.day, item.dowIndex);
            });
            itemView.setOnLongClickListener(v -> {
                if (listener != null) listener.onLongClick(item.day, item.dowIndex);
                return true;
            });

            // Załaduj plany dla tego dnia
            loadEventsForDay(item.day, eventsContainer, ctx);
        }

        private void loadEventsForDay(Calendar day, LinearLayout container, android.content.Context ctx) {
            if (cachedEmail == null) {
                showNoEvents(container, ctx);
                return;
            }

            Calendar startOfDay = (Calendar) day.clone();
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
                    .whereEqualTo("email", cachedEmail)
                    .get()
                    .addOnSuccessListener(query -> {
                        // Sprawdź czy kontener nadal jest przypisany do tego dnia
                        // (RecyclerView może już zrecykloweać ten ViewHolder)
                        if (container.getTag() != null &&
                                !container.getTag().equals(day.getTimeInMillis())) return;

                        container.removeAllViews();

                        List<QueryDocumentSnapshot> matching = new ArrayList<>();
                        for (QueryDocumentSnapshot doc : query) {
                            com.google.firebase.Timestamp ts = doc.getTimestamp("dueTime");
                            if (ts == null) continue;
                            long ms = ts.toDate().getTime();
                            if (ms >= startMs && ms < endMs) matching.add(doc);
                        }

                        if (matching.isEmpty()) {
                            showNoEvents(container, ctx);
                            return;
                        }

                        for (QueryDocumentSnapshot doc : matching) {
                            com.google.firebase.Timestamp ts = doc.getTimestamp("dueTime");
                            String title = doc.getString("title");
                            if (title == null || title.isEmpty()) title = doc.getString("content");
                            if (title == null || title.isEmpty()) title = "—";

                            LinearLayout row = new LinearLayout(ctx);
                            row.setOrientation(LinearLayout.HORIZONTAL);
                            row.setPadding(0, 2, 0, 2);

                            TextView tvTime = new TextView(ctx);
                            tvTime.setText(ts != null ? timeFmt.format(ts.toDate()) : "");
                            tvTime.setTextSize(11f);
                            tvTime.setTextColor(ContextCompat.getColor(ctx, R.color.brown_medium));
                            tvTime.setMinWidth(dpToPx(ctx, 38));

                            TextView tvTitle = new TextView(ctx);
                            tvTitle.setText(title);
                            tvTitle.setTextSize(13f);
                            tvTitle.setTextColor(ContextCompat.getColor(ctx, R.color.brown_primary));
                            tvTitle.setMaxLines(1);
                            tvTitle.setEllipsize(android.text.TextUtils.TruncateAt.END);
                            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                                    0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
                            lp.setMarginStart(6);
                            tvTitle.setLayoutParams(lp);

                            row.addView(tvTime);
                            row.addView(tvTitle);
                            container.addView(row);
                        }
                    });

            // Tag kontenera żeby wykryć recykling
            container.setTag(day.getTimeInMillis());
        }

        private void showNoEvents(LinearLayout container, android.content.Context ctx) {
            container.removeAllViews();
            TextView tv = new TextView(ctx);
            tv.setText(R.string.plan_no_events);
            tv.setTextSize(12f);
            tv.setTextColor(ContextCompat.getColor(ctx, R.color.brown_light));
            container.addView(tv);
        }

        private int dpToPx(android.content.Context ctx, int dp) {
            return Math.round(dp * ctx.getResources().getDisplayMetrics().density);
        }
    }

    static class SepHolder extends RecyclerView.ViewHolder {
        TextView tv;
        SepHolder(View v) { super(v); tv = v.findViewById(R.id.tvSeparator); }
    }

    private boolean isSameDay(Calendar a, Calendar b) {
        return a.get(Calendar.YEAR)         == b.get(Calendar.YEAR)
                && a.get(Calendar.MONTH)        == b.get(Calendar.MONTH)
                && a.get(Calendar.DAY_OF_MONTH) == b.get(Calendar.DAY_OF_MONTH);
    }
}