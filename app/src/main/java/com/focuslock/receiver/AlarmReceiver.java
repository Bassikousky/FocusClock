package com.focuslock.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import com.focuslock.service.LockService;

public class AlarmReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();

        if ("com.focuslock.ACTION_TRIGGER_LOCK".equals(action)) {
            int lockMinutes = intent.getIntExtra("lock_minutes", 30);
            long endTimeMillis = System.currentTimeMillis() + (lockMinutes * 60 * 1000L);

            Intent serviceIntent = new Intent(context, LockService.class);
            serviceIntent.putExtra("end_time_millis", endTimeMillis);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(serviceIntent);
            } else {
                context.startService(serviceIntent);
            }
        }
    }
}
