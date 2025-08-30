package com.example.appcentinela

import android.app.admin.DeviceAdminReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.preference.PreferenceManager

class DeviceAdmin : DeviceAdminReceiver() {
    override fun onPasswordFailed(context: Context, intent: Intent) {
        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
        val isProtectionEnabled = sharedPreferences.getBoolean("key_protection_enabled", false)
        val shouldLogFailed = sharedPreferences.getBoolean("key_log_failed_attempts", false)

        Log.d("AppCentinelaDebug", "onPasswordFailed -> ProtectionEnabled: $isProtectionEnabled, ShouldLogFailed: $shouldLogFailed")

        if (isProtectionEnabled && shouldLogFailed) {
            Log.d("AppCentinelaDebug", "CONDICIÓN FALLIDA CUMPLIDA. Lanzando CaptureService.")
            val serviceIntent = Intent(context, CaptureService::class.java).apply {
                putExtra("IS_SUCCESSFUL", false)
            }
            ContextCompat.startForegroundService(context, serviceIntent)
        }
    }
}