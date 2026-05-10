package com.focuslock.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;

import com.focuslock.service.LockService;

public class BootReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        if (!Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) return;

        SharedPreferences prefs = context.getSharedPreferences("focuslock_prefs", Context.MODE_PRIVATE);
        long lockEndTime = prefs.getLong("lock_end_time", 0);

        // Si el bloqueo sigue vigente, reanudarlo
        if (lockEndTime > System.currentTimeMillis()) {
            Intent serviceIntent = new Intent(context, LockService.class);
            serviceIntent.putExtra("end_time_millis", lockEndTime);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(serviceIntent);
            } else {
                context.startService(serviceIntent);
            }
        }
    }
}
