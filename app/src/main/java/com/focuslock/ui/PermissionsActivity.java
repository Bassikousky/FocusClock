package com.focuslock.ui;

import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.accessibility.AccessibilityManager;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.focuslock.R;
import com.focuslock.admin.FocusDeviceAdminReceiver;

public class PermissionsActivity extends AppCompatActivity {

    private static final int REQUEST_DEVICE_ADMIN = 101;

    private TextView tvPermStatus1, tvPermStatus2, tvPermStatus3;
    private Button btnPerm1, btnPerm2, btnPerm3;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_permissions);

        tvPermStatus1 = findViewById(R.id.tvPermStatus1);
        tvPermStatus2 = findViewById(R.id.tvPermStatus2);
        tvPermStatus3 = findViewById(R.id.tvPermStatus3);
        btnPerm1 = findViewById(R.id.btnPerm1);
        btnPerm2 = findViewById(R.id.btnPerm2);
        btnPerm3 = findViewById(R.id.btnPerm3);

        btnPerm1.setOnClickListener(v -> requestOverlayPermission());
        btnPerm2.setOnClickListener(v -> requestAccessibilityPermission());
        btnPerm3.setOnClickListener(v -> requestDeviceAdmin());
    }

    @Override
    protected void onResume() {
        super.onResume();
        updatePermissionStatus();
    }

    private void updatePermissionStatus() {
        // 1. Permiso de superposición
        boolean hasOverlay = Settings.canDrawOverlays(this);
        tvPermStatus1.setText(hasOverlay ? "✅ Concedido" : "❌ Requerido");
        btnPerm1.setEnabled(!hasOverlay);

        // 2. Servicio de Accesibilidad
        boolean hasAccessibility = isAccessibilityServiceEnabled();
        tvPermStatus2.setText(hasAccessibility ? "✅ Activo" : "❌ Requerido");
        btnPerm2.setEnabled(!hasAccessibility);

        // 3. Administrador del dispositivo
        DevicePolicyManager dpm = (DevicePolicyManager) getSystemService(DEVICE_POLICY_SERVICE);
        ComponentName cn = new ComponentName(this, FocusDeviceAdminReceiver.class);
        boolean hasAdmin = dpm.isAdminActive(cn);
        tvPermStatus3.setText(hasAdmin ? "✅ Activo" : "⚠️ Recomendado");
        btnPerm3.setEnabled(!hasAdmin);
    }

    private void requestOverlayPermission() {
        new AlertDialog.Builder(this)
                .setTitle("Permiso de superposición")
                .setMessage("Este permiso permite que FocusLock muestre la pantalla de bloqueo sobre otras apps.\n\nBusca 'FocusLock' en la lista y activa el permiso.")
                .setPositiveButton("Abrir ajustes", (d, w) -> {
                    Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                            Uri.parse("package:" + getPackageName()));
                    startActivity(intent);
                })
                .show();
    }

    private void requestAccessibilityPermission() {
        new AlertDialog.Builder(this)
                .setTitle("Servicio de Accesibilidad")
                .setMessage("El servicio de accesibilidad permite a FocusLock detectar cuando intentas salir de la pantalla de bloqueo y volverte a mostrarla.\n\nBusca 'FocusLock' en Servicios descargados y actívalo.")
                .setPositiveButton("Abrir ajustes", (d, w) -> {
                    startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS));
                })
                .show();
    }

    private void requestDeviceAdmin() {
        new AlertDialog.Builder(this)
                .setTitle("Administrador del dispositivo")
                .setMessage("Este permiso (opcional) permite bloquear la pantalla del sistema también. Proporciona una capa extra de seguridad.")
                .setPositiveButton("Activar", (d, w) -> {
                    Intent intent = new Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN);
                    intent.putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN,
                            new ComponentName(this, FocusDeviceAdminReceiver.class));
                    intent.putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION,
                            "FocusLock necesita este permiso para bloquear la pantalla del dispositivo.");
                    startActivityForResult(intent, REQUEST_DEVICE_ADMIN);
                })
                .setNegativeButton("Omitir", null)
                .show();
    }

    private boolean isAccessibilityServiceEnabled() {
        AccessibilityManager am = (AccessibilityManager) getSystemService(ACCESSIBILITY_SERVICE);
        String service = getPackageName() + "/.service.FocusAccessibilityService";
        try {
            int enabled = Settings.Secure.getInt(getContentResolver(),
                    Settings.Secure.ACCESSIBILITY_ENABLED);
            if (enabled == 1) {
                String services = Settings.Secure.getString(getContentResolver(),
                        Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);
                if (services != null) {
                    return services.toLowerCase().contains(service.toLowerCase());
                }
            }
        } catch (Exception ignored) {}
        return false;
    }
}
