package com.focuslock.service;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.IBinder;

import androidx.core.app.NotificationCompat;

import com.focuslock.R;
import com.focuslock.ui.LockScreenActivity;

public class LockService extends Service {

    private static final String CHANNEL_ID = "focuslock_channel";
    private static final int NOTIF_ID = 1;

    public static boolean isRunning = false;
    public static long endTimeMillis = 0;

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent == null) {
            stopSelf();
            return START_NOT_STICKY;
        }

        long endTime = intent.getLongExtra("end_time_millis", 0);
        if (endTime == 0) {
            stopSelf();
            return START_NOT_STICKY;
        }

        endTimeMillis = endTime;
        isRunning = true;

        // Guardar para sobrevivir a reinicios
        SharedPreferences prefs = getSharedPreferences("focuslock_prefs", MODE_PRIVATE);
        prefs.edit().putLong("lock_end_time", endTime).apply();

        // Lanzar pantalla de bloqueo
        launchLockScreen(endTime);

        // Notificación persistente
        startForeground(NOTIF_ID, buildNotification());

        return START_STICKY;
    }

    private void launchLockScreen(long endTime) {
        Intent lockIntent = new Intent(this, LockScreenActivity.class);
        lockIntent.putExtra("end_time_millis", endTime);
        lockIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK |
                Intent.FLAG_ACTIVITY_CLEAR_TOP |
                Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(lockIntent);
    }

    private Notification buildNotification() {
        Intent lockIntent = new Intent(this, LockScreenActivity.class);
        lockIntent.putExtra("end_time_millis", endTimeMillis);
        lockIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        PendingIntent pi = PendingIntent.getActivity(this, 0, lockIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("FocusLock activo")
                .setContentText("El bloqueo está en curso. ¡Mantén el enfoque!")
                .setSmallIcon(R.drawable.ic_lock)
                .setContentIntent(pi)
                .setOngoing(true)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .build();
    }

    private void createNotificationChannel() {
        NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                "FocusLock",
                NotificationManager.IMPORTANCE_HIGH
        );
        channel.setDescription("Servicio de bloqueo de pantalla");
        NotificationManager nm = getSystemService(NotificationManager.class);
        if (nm != null) nm.createNotificationChannel(channel);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        isRunning = false;
        endTimeMillis = 0;
        // Borrar tiempo de fin guardado
        SharedPreferences prefs = getSharedPreferences("focuslock_prefs", MODE_PRIVATE);
        prefs.edit().remove("lock_end_time").apply();
        // Enviar broadcast de desbloqueo a la LockScreenActivity
        sendBroadcast(new Intent("com.focuslock.ACTION_UNLOCK_SCREEN"));
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
