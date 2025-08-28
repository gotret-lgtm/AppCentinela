package com.example.appcentinela

import android.app.admin.DeviceAdminReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.preference.PreferenceManager

class DeviceAdmin : DeviceAdminReceiver() {

    override fun onPasswordFailed(context: Context, intent: Intent) {
        // ... tu código para leer las SharedPreferences ...
        Log.d("AppCentinelaDebug", "onPasswordFailed -> ProtectionEnabled: $isProtectionEnabled, ShouldCapture: $shouldCapturePhoto")

        if (isProtectionEnabled && shouldCapturePhoto) {
            Log.d("AppCentinelaDebug", "CONDICIÓN CUMPLIDA. Lanzando CaptureService.")

            // ===== CAMBIA ESTO =====
            // val activityIntent = Intent(context, CaptureActivity::class.java) ...
            // context.startActivity(activityIntent)

            // ===== POR ESTO =====
            val serviceIntent = Intent(context, CaptureService::class.java).apply {
                putExtra("IS_SUCCESSFUL", false)
            }

            // Usamos startForegroundService que es obligatorio para API 26+
            ContextCompat.startForegroundService(context, serviceIntent)

        } else {
            Log.d("AppCentinelaDebug", "CONDICIÓN NO CUMPLIDA. No se lanzará CaptureActivity.")
        }
    }

    override fun onEnabled(context: Context, intent: Intent) {
        super.onEnabled(context, intent)
        Log.d("DeviceAdmin", "Administrador de dispositivo activado")
    }

    override fun onDisabled(context: Context, intent: Intent) {
        super.onDisabled(context, intent)
        Log.d("DeviceAdmin", "Administrador de dispositivo desactivado")
    }
}