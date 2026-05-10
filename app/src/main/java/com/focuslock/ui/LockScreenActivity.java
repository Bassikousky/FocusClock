package com.focuslock.ui;

import android.app.KeyguardManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.view.WindowManager;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.focuslock.R;

public class LockScreenActivity extends AppCompatActivity {

    private TextView tvCountdown;
    private TextView tvMotivation;
    private CountDownTimer countDownTimer;
    private long endTimeMillis;

    private BroadcastReceiver unlockReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if ("com.focuslock.ACTION_UNLOCK_SCREEN".equals(intent.getAction())) {
                finish();
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Mostrar sobre la pantalla de bloqueo del sistema
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true);
            setTurnScreenOn(true);
            KeyguardManager km = (KeyguardManager) getSystemService(KEYGUARD_SERVICE);
            if (km != null) {
                km.requestDismissKeyguard(this, null);
            }
        } else {
            getWindow().addFlags(
                    WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED |
                    WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON |
                    WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
            );
        }

        // Modo inmersivo total (ocultar barra de navegación y estado)
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getWindow().getDecorView().setSystemUiVisibility(
                android.view.View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY |
                android.view.View.SYSTEM_UI_FLAG_HIDE_NAVIGATION |
                android.view.View.SYSTEM_UI_FLAG_FULLSCREEN |
                android.view.View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION |
                android.view.View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
        );

        setContentView(R.layout.activity_lock_screen);

        tvCountdown = findViewById(R.id.tvCountdown);
        tvMotivation = findViewById(R.id.tvMotivation);

        endTimeMillis = getIntent().getLongExtra("end_time_millis", 0);
        if (endTimeMillis == 0) {
            finish();
            return;
        }

        startCountdown();
        registerUnlockReceiver();
    }

    private void startCountdown() {
        long remaining = endTimeMillis - System.currentTimeMillis();
        if (remaining <= 0) {
            finish();
            return;
        }

        countDownTimer = new CountDownTimer(remaining, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                updateCountdownText(millisUntilFinished);
            }

            @Override
            public void onFinish() {
                finish();
            }
        }.start();
    }

    private void updateCountdownText(long millis) {
        long totalSeconds = millis / 1000;
        long hours = totalSeconds / 3600;
        long minutes = (totalSeconds % 3600) / 60;
        long seconds = totalSeconds % 60;

        String text;
        if (hours > 0) {
            text = String.format("%02d:%02d:%02d", hours, minutes, seconds);
        } else {
            text = String.format("%02d:%02d", minutes, seconds);
        }
        tvCountdown.setText(text);

        // Mensajes motivacionales según el tiempo restante
        long minutesLeft = totalSeconds / 60;
        String motivation;
        if (minutesLeft > 30) {
            motivation = "Mantén el foco. Tú puedes.";
        } else if (minutesLeft > 15) {
            motivation = "Vas por la mitad. ¡Sigue!";
        } else if (minutesLeft > 5) {
            motivation = "Ya queda poco. ¡Ánimo!";
        } else {
            motivation = "¡Casi terminas! 💪";
        }
        tvMotivation.setText(motivation);
    }

    private void registerUnlockReceiver() {
        IntentFilter filter = new IntentFilter("com.focuslock.ACTION_UNLOCK_SCREEN");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(unlockReceiver, filter, Context.RECEIVER_NOT_EXPORTED);
        } else {
            registerReceiver(unlockReceiver, filter);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (countDownTimer != null) {
            countDownTimer.cancel();
        }
        try {
            unregisterReceiver(unlockReceiver);
        } catch (Exception ignored) {}
    }

    // Evitar que el botón Atrás cierre la pantalla
    @Override
    public void onBackPressed() {
        // No hacer nada
    }

    // Mantener en primer plano si el usuario intenta salir
    @Override
    protected void onPause() {
        super.onPause();
        // El servicio de accesibilidad se encargará de volver aquí
    }
}
