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
        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
        val isProtectionEnabled = sharedPreferences.getBoolean("key_protection_enabled", false)

        // Leemos el valor del nuevo interruptor para accesos fallidos
        val shouldLogFailed = sharedPreferences.getBoolean("key_log_failed_attempts", false)

        Log.d("AppCentinelaDebug", "onPasswordFailed -> ProtectionEnabled: $isProtectionEnabled, ShouldLogFailed: $shouldLogFailed")

        // Si la protección general Y el registro de fallos están activos...
        if (isProtectionEnabled && shouldLogFailed) {
            Log.d("AppCentinelaDebug", "CONDICIÓN FALLIDA CUMPLIDA. Lanzando CaptureService.")

            val serviceIntent = Intent(context, CaptureService::class.java).apply {
                putExtra("IS_SUCCESSFUL", false) // Indicamos que fue un fallo
            }
            ContextCompat.startForegroundService(context, serviceIntent)

        } else {
            Log.d("AppCentinelaDebug", "CONDICIÓN FALLIDA NO CUMPLIDA. No se hará nada.")
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