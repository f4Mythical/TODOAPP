package com.example.todo;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import androidx.core.app.NotificationCompat;

public class AlarmReceiver extends BroadcastReceiver {

    private static final String CHANNEL_ID   = "ember_plans";
    private static final String CHANNEL_NAME = "Plany";
    private static final int    NOTIF_ID_BASE = 1000;

    @Override
    public void onReceive(Context context, Intent intent) {
        String planId      = intent.getStringExtra("plan_id");
        String planTitle   = intent.getStringExtra("plan_title");
        String planContent = intent.getStringExtra("plan_content");

        if (planId == null) return;

        createChannel(context);

        Intent openIntent = new Intent(context, PlanDetailActivity.class);
        openIntent.putExtra("plan_id", planId);
        openIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);

        PendingIntent pi = PendingIntent.getActivity(
                context,
                planId.hashCode(),
                openIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        String title = (planTitle != null && !planTitle.isEmpty()) ? planTitle : "Nowy plan";
        String text  = (planContent != null && !planContent.isEmpty()) ? planContent : "Masz zaplanowane zadanie";

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_calendar)
                .setContentTitle(title)
                .setContentText(text)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(text))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setContentIntent(pi);

        NotificationManager nm =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (nm != null) {
            nm.notify(NOTIF_ID_BASE + planId.hashCode(), builder.build());
        }
    }

    private void createChannel(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_HIGH
            );
            channel.setDescription("Powiadomienia o planach");
            channel.enableVibration(true);
            NotificationManager nm =
                    (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            if (nm != null) nm.createNotificationChannel(channel);
        }
    }
}