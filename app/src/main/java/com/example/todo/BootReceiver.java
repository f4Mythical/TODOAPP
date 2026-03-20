package com.example.todo;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.List;

public class BootReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        if (!Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction()) &&
                !"android.intent.action.QUICKBOOT_POWERON".equals(intent.getAction())) return;

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;
        String email = user.getEmail();
        if (email == null) return;

        long nowMs = System.currentTimeMillis();

        FirebaseFirestore.getInstance()
                .collection("plans")
                .whereEqualTo("email", email)
                .whereEqualTo("isDone", false)
                .get()
                .addOnSuccessListener(query -> {
                    for (QueryDocumentSnapshot doc : query) {
                        String planId  = doc.getId();
                        String title   = doc.getString("title");
                        String content = doc.getString("content");

                        List<com.google.firebase.Timestamp> notifList =
                                (List<com.google.firebase.Timestamp>) doc.get("notificationTimes");

                        if (notifList != null && !notifList.isEmpty()) {
                            for (com.google.firebase.Timestamp ts : notifList) {
                                if (ts == null) continue;
                                long triggerMs = ts.toDate().getTime();
                                if (triggerMs <= nowMs) continue;
                                NotificationScheduler.schedule(
                                        context,
                                        planId + "_" + triggerMs,
                                        title,
                                        content,
                                        triggerMs
                                );
                            }
                        } else {
                            com.google.firebase.Timestamp notifTs = doc.getTimestamp("notificationTime");
                            if (notifTs == null) continue;
                            long triggerMs = notifTs.toDate().getTime();
                            if (triggerMs <= nowMs) continue;
                            NotificationScheduler.schedule(context, planId, title, content, triggerMs);
                        }
                    }
                });
    }
}