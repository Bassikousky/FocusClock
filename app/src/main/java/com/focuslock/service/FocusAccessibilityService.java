package com.focuslock.service;

import android.accessibilityservice.AccessibilityService;
import android.content.Intent;
import android.view.accessibility.AccessibilityEvent;

import com.focuslock.ui.LockScreenActivity;

public class FocusAccessibilityService extends AccessibilityService {

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        if (!LockService.isRunning) return;

        long endTime = LockService.endTimeMillis;
        if (endTime == 0 || System.currentTimeMillis() >= endTime) return;

        int type = event.getEventType();
        if (type == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            String packageName = event.getPackageName() != null
                    ? event.getPackageName().toString() : "";

            // Si la ventana activa NO es nuestra app, volver a la pantalla de bloqueo
            if (!packageName.equals(getPackageName())) {
                bringBackLockScreen(endTime);
            }
        }
    }

    private void bringBackLockScreen(long endTime) {
        Intent intent = new Intent(this, LockScreenActivity.class);
        intent.putExtra("end_time_millis", endTime);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK |
                Intent.FLAG_ACTIVITY_CLEAR_TOP |
                Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(intent);
    }

    @Override
    public void onInterrupt() {}
}
