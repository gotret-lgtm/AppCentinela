package com.example.appcentinela

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.preference.PreferenceManager

class UnlockReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_USER_PRESENT) {
            Log.d("UnlockReceiver", "¡Desbloqueo exitoso detectado!")

            val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
            val isProtectionEnabled = sharedPreferences.getBoolean("key_protection_enabled", false)
            val monitoringMode = sharedPreferences.getString("key_monitoring_mode", "failed")
            val shouldCapturePhoto = sharedPreferences.getBoolean("key_capture_photo", false)

            // Solo actuamos si la protección está activa, el modo es "todos" y queremos tomar foto
            if (isProtectionEnabled && monitoringMode == "all" && shouldCapturePhoto) {
                Log.d("UnlockReceiver", "Lanzando actividad de captura para acceso exitoso.")

                val activityIntent = Intent(context, CaptureActivity::class.java).apply {
                    // ¡MUY IMPORTANTE! Le pasamos un extra para indicar que fue un éxito
                    putExtra("IS_SUCCESSFUL", true)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(activityIntent)
            }
        }
    }
}
