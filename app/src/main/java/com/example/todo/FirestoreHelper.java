package com.example.todo;

import android.content.Context;

import com.example.todo.NotificationScheduler;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class FirestoreHelper {

    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    public void addPlan(Context context,
                        String uid, String email, String nick,
                        String title, String content,
                        Timestamp notificationTime, Timestamp dueTime,
                        String priority) {

        Map<String, Object> plan = new HashMap<>();
        plan.put("email", email);
        plan.put("nick", nick);
        plan.put("createdAt", Timestamp.now());
        plan.put("content", content != null ? content : "");
        plan.put("notificationTime", notificationTime);
        plan.put("dueTime", dueTime);
        plan.put("isDone", false);
        plan.put("priority", priority != null ? priority : "medium");
        if (title != null && !title.isEmpty()) plan.put("title", title);

        db.collection("plans")
                .add(plan)
                .addOnSuccessListener(documentReference -> {
                    String planId = documentReference.getId();

                    if (notificationTime != null && context != null) {
                        long triggerMs = notificationTime.toDate().getTime();
                        NotificationScheduler.schedule(
                                context.getApplicationContext(),
                                planId,
                                title,
                                content,
                                triggerMs
                        );
                    }
                })
                .addOnFailureListener(e -> {
                });
    }

    public void addPlan(String uid, String email, String nick,
                        String title, String content,
                        Timestamp notificationTime, Timestamp dueTime,
                        String priority) {
        addPlan(null, uid, email, nick, title, content, notificationTime, dueTime, priority);
    }

    public void getUserPlans(String email,
                             com.google.android.gms.tasks.OnSuccessListener<com.google.firebase.firestore.QuerySnapshot> onSuccess) {
        db.collection("plans")
                .whereEqualTo("email", email)
                .orderBy("dueTime")
                .get()
                .addOnSuccessListener(onSuccess)
                .addOnFailureListener(e -> {});
    }

    public void markPlanDone(String planId) {
        db.collection("plans")
                .document(planId)
                .update("isDone", true);
    }
}