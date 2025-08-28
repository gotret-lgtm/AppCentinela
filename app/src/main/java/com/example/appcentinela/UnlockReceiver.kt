package com.example.appcentinela

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.preference.PreferenceManager

class UnlockReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        // Nos aseguramos de que la acción sea la de un desbloqueo exitoso
        if (intent.action == Intent.ACTION_USER_PRESENT) {

            val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
            val isProtectionEnabled = sharedPreferences.getBoolean("key_protection_enabled", false)

            // Leemos el valor del nuevo interruptor para accesos exitosos
            val shouldLogSuccessful = sharedPreferences.getBoolean("key_log_successful_attempts", false)

            Log.d("AppCentinelaDebug", "UnlockReceiver -> ProtectionEnabled: $isProtectionEnabled, ShouldLogSuccessful: $shouldLogSuccessful")

            // Si la protección general Y el registro de éxitos están activos...
            if (isProtectionEnabled && shouldLogSuccessful) {
                Log.d("AppCentinelaDebug", "CONDICIÓN EXITOSA CUMPLIDA. Lanzando CaptureService.")

                val serviceIntent = Intent(context, CaptureService::class.java).apply {
                    putExtra("IS_SUCCESSFUL", true) // Indicamos que fue un éxito
                }
                ContextCompat.startForegroundService(context, serviceIntent)
            } else {
                Log.d("AppCentinelaDebug", "CONDICIÓN EXITOSA NO CUMPLIDA. No se hará nada.")
            }
        }
    }
}