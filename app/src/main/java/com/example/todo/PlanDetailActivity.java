package com.example.todo;

import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.Locale;

public class PlanDetailActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_plan_detail);

        String planId = getIntent().getStringExtra("plan_id");
        if (planId == null) { finish(); return; }

        TextView tvTitle    = findViewById(R.id.tvDetailTitle);
        TextView tvContent  = findViewById(R.id.tvDetailContent);
        TextView tvDueTime  = findViewById(R.id.tvDetailDueTime);
        TextView tvPriority = findViewById(R.id.tvDetailPriority);

        findViewById(R.id.btnDetailClose).setOnClickListener(v -> finish());

        FirebaseFirestore.getInstance()
                .collection("plans")
                .document(planId)
                .get()
                .addOnSuccessListener(doc -> {
                    if (!doc.exists()) { finish(); return; }

                    String title    = doc.getString("title");
                    String content  = doc.getString("content");
                    String priority = doc.getString("priority");
                    com.google.firebase.Timestamp dueTs = doc.getTimestamp("dueTime");

                    tvTitle.setText(title != null && !title.isEmpty() ? title : "—");
                    tvContent.setText(content != null && !content.isEmpty() ? content : "—");
                    tvPriority.setText(formatPriority(priority));

                    if (dueTs != null) {
                        SimpleDateFormat sdf =
                                new SimpleDateFormat("d MMMM yyyy, HH:mm", new Locale("pl"));
                        tvDueTime.setText(sdf.format(dueTs.toDate()));
                    } else {
                        tvDueTime.setText("—");
                    }
                })
                .addOnFailureListener(e -> finish());
    }

    private String formatPriority(String p) {
        if (p == null) return "Średni";
        switch (p) {
            case "low":  return "Niski";
            case "high": return "Wysoki";
            default:     return "Średni";
        }
    }
}