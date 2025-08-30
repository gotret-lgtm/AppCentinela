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
        super.onPasswordFailed(context, intent)

        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
        val isProtectionEnabled = sharedPreferences.getBoolean("key_protection_enabled", false)
        val shouldCapturePhoto = sharedPreferences.getBoolean("key_capture_photo", false)

        if (isProtectionEnabled && shouldCapturePhoto) {
            Log.d("DeviceAdmin", "Lanzando actividad de captura para acceso fallido.")

            val activityIntent = Intent(context, CaptureActivity::class.java).apply {
                // ¡IMPORTANTE! Le pasamos el extra indicando que fue un fallo
                putExtra("IS_SUCCESSFUL", false)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(activityIntent)
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