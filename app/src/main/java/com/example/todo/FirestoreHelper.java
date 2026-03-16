package com.example.todo.Onboarding;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class FirestoreHelper {

    private final FirebaseFirestore db = FirebaseFirestore.getInstance();


    public void addPlan(String uid, String email, String nick,
                        String title, String content,
                        Timestamp notificationTime, Timestamp dueTime,
                        String priority) {

        Map<String, Object> plan = new HashMap<>();
        plan.put("email", email);
        plan.put("nick", nick);
        plan.put("createdAt", Timestamp.now());
        plan.put("content", content);
        plan.put("notificationTime", notificationTime);
        plan.put("dueTime", dueTime);
        plan.put("isDone", false);
        plan.put("priority", priority != null ? priority : "medium");

        if (title != null && !title.isEmpty()) {
            plan.put("title", title);
        }

        db.collection("plans")
                .add(plan)
                .addOnSuccessListener(documentReference -> {
                    // plan dodany, documentReference.getId() = ID dokumentu
                })
                .addOnFailureListener(e -> {
                    // obsługa błędu
                });
    }

    public void getUserPlans(String email, com.google.android.gms.tasks.OnSuccessListener<com.google.firebase.firestore.QuerySnapshot> onSuccess) {
        db.collection("plans")
                .whereEqualTo("email", email)
                .orderBy("dueTime")
                .get()
                .addOnSuccessListener(onSuccess)
                .addOnFailureListener(e -> {
                    // obsługa błędu
                });
    }

    public void markPlanDone(String planId) {
        db.collection("plans")
                .document(planId)
                .update("isDone", true);
    }
}