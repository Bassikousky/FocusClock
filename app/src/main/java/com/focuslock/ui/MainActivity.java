package com.focuslock.ui;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.focuslock.R;
import com.focuslock.receiver.AlarmReceiver;

import java.util.Calendar;

public class MainActivity extends AppCompatActivity {

    private static final String PREFS = "focuslock_prefs";
    private static final String KEY_LOCK_MINUTES = "lock_minutes";
    private static final String KEY_ALARM_HOUR = "alarm_hour";
    private static final String KEY_ALARM_MINUTE = "alarm_minute";
    private static final String KEY_ALARM_SET = "alarm_set";

    private TextView tvAlarmTime;
    private TextView tvLockDuration;
    private TextView tvStatus;
    private SeekBar seekBarDuration;
    private Button btnSetAlarm;
    private Button btnCancelAlarm;
    private Button btnPermissions;

    private int alarmHour = -1;
    private int alarmMinute = -1;
    private int lockMinutes = 30;
    private SharedPreferences prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        prefs = getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        loadSavedState();
        initViews();
        setupListeners();
        updateUI();
    }

    private void loadSavedState() {
        lockMinutes = prefs.getInt(KEY_LOCK_MINUTES, 30);
        alarmHour = prefs.getInt(KEY_ALARM_HOUR, -1);
        alarmMinute = prefs.getInt(KEY_ALARM_MINUTE, -1);
    }

    private void initViews() {
        tvAlarmTime = findViewById(R.id.tvAlarmTime);
        tvLockDuration = findViewById(R.id.tvLockDuration);
        tvStatus = findViewById(R.id.tvStatus);
        seekBarDuration = findViewById(R.id.seekBarDuration);
        btnSetAlarm = findViewById(R.id.btnSetAlarm);
        btnCancelAlarm = findViewById(R.id.btnCancelAlarm);
        btnPermissions = findViewById(R.id.btnPermissions);

        // SeekBar: 5 min → 480 min (8h), paso de 5 min
        seekBarDuration.setMax(95); // (480-5)/5 = 95 pasos
        int progress = (lockMinutes - 5) / 5;
        seekBarDuration.setProgress(Math.max(0, progress));
    }

    private void setupListeners() {
        btnSetAlarm.setOnClickListener(v -> showTimePicker());

        btnCancelAlarm.setOnClickListener(v -> cancelAlarm());

        btnPermissions.setOnClickListener(v -> {
            startActivity(new Intent(this, PermissionsActivity.class));
        });

        seekBarDuration.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                lockMinutes = 5 + (progress * 5);
                updateDurationText();
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {
                prefs.edit().putInt(KEY_LOCK_MINUTES, lockMinutes).apply();
            }
        });
    }

    private void showTimePicker() {
        Calendar cal = Calendar.getInstance();
        int hour = alarmHour >= 0 ? alarmHour : cal.get(Calendar.HOUR_OF_DAY);
        int minute = alarmMinute >= 0 ? alarmMinute : cal.get(Calendar.MINUTE);

        new TimePickerDialog(this, (view, h, m) -> {
            alarmHour = h;
            alarmMinute = m;
            scheduleAlarm();
        }, hour, minute, true).show();
    }

    private void scheduleAlarm() {
        Calendar alarmCal = Calendar.getInstance();
        alarmCal.set(Calendar.HOUR_OF_DAY, alarmHour);
        alarmCal.set(Calendar.MINUTE, alarmMinute);
        alarmCal.set(Calendar.SECOND, 0);
        alarmCal.set(Calendar.MILLISECOND, 0);

        // Si la hora ya pasó hoy, programar para mañana
        if (alarmCal.getTimeInMillis() <= System.currentTimeMillis()) {
            alarmCal.add(Calendar.DAY_OF_MONTH, 1);
        }

        Intent intent = new Intent(this, AlarmReceiver.class);
        intent.setAction("com.focuslock.ACTION_TRIGGER_LOCK");
        intent.putExtra("lock_minutes", lockMinutes);

        PendingIntent pi = PendingIntent.getBroadcast(this, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        AlarmManager am = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, alarmCal.getTimeInMillis(), pi);

        // Guardar estado
        prefs.edit()
                .putInt(KEY_ALARM_HOUR, alarmHour)
                .putInt(KEY_ALARM_MINUTE, alarmMinute)
                .putBoolean(KEY_ALARM_SET, true)
                .apply();

        updateUI();
        Toast.makeText(this, "⏰ Alarma programada", Toast.LENGTH_SHORT).show();
    }

    private void cancelAlarm() {
        Intent intent = new Intent(this, AlarmReceiver.class);
        intent.setAction("com.focuslock.ACTION_TRIGGER_LOCK");
        PendingIntent pi = PendingIntent.getBroadcast(this, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        AlarmManager am = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        am.cancel(pi);

        prefs.edit().putBoolean(KEY_ALARM_SET, false).apply();
        alarmHour = -1;
        alarmMinute = -1;

        updateUI();
        Toast.makeText(this, "Alarma cancelada", Toast.LENGTH_SHORT).show();
    }

    private void updateUI() {
        boolean alarmSet = prefs.getBoolean(KEY_ALARM_SET, false) && alarmHour >= 0;

        if (alarmSet) {
            tvAlarmTime.setText(String.format("⏰ %02d:%02d", alarmHour, alarmMinute));
            tvStatus.setText("Alarma programada — el bloqueo se activará a las " +
                    String.format("%02d:%02d", alarmHour, alarmMinute));
            btnCancelAlarm.setEnabled(true);
            btnSetAlarm.setText("Cambiar hora");
        } else {
            tvAlarmTime.setText("Sin alarma");
            tvStatus.setText("Configura una hora y la duración del bloqueo");
            btnCancelAlarm.setEnabled(false);
            btnSetAlarm.setText("Establecer alarma");
        }

        updateDurationText();
    }

    private void updateDurationText() {
        if (lockMinutes < 60) {
            tvLockDuration.setText("🔒 " + lockMinutes + " minutos");
        } else {
            int h = lockMinutes / 60;
            int m = lockMinutes % 60;
            if (m == 0) {
                tvLockDuration.setText("🔒 " + h + "h");
            } else {
                tvLockDuration.setText("🔒 " + h + "h " + m + "min");
            }
        }
    }
}
