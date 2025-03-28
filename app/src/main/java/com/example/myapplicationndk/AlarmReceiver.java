package com.example.myapplicationndk;


import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.SystemClock;

import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

public class AlarmReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        // Trigger background task
        OneTimeWorkRequest workRequest = new OneTimeWorkRequest.Builder(ServiceHandler.class).build();
        WorkManager.getInstance(context).enqueue(workRequest);

        // Immediately schedule the next alarm
        scheduleNextAlarm(context);
    }

    private void scheduleNextAlarm(Context context) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(context, AlarmReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        // Set the next alarm 5 minutes (300,000 milliseconds) after the current one
        long triggerTime = SystemClock.elapsedRealtime() + 300000;

        if (alarmManager != null) {
            alarmManager.setExact(AlarmManager.ELAPSED_REALTIME_WAKEUP, triggerTime, pendingIntent);
        }
    }
}
