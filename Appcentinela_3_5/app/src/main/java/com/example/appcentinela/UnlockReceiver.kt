package com.example.appcentinela

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.preference.PreferenceManager
import java.io.File

class UnlockReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_USER_PRESENT) {
            Log.d("UnlockReceiver", "Desbloqueo exitoso detectado.")

            // Usamos applicationContext para la lectura más fiable de preferencias
            val prefs = PreferenceManager.getDefaultSharedPreferences(context.applicationContext)
            val isProtectionEnabled = prefs.getBoolean("key_protection_enabled", false)
            val shouldCapturePhoto = prefs.getBoolean("key_capture_photo", false)

            // Si la protección o la captura de fotos están desactivadas, no hacemos nada más.
            if (!isProtectionEnabled || !shouldCapturePhoto) {
                Log.d("UnlockReceiver", "Protección o captura de foto desactivada. No se hace nada.")
                return
            }

            // --- Lógica de la bandera de fallo ---
            val flagFile = File(context.filesDir, "failed_attempt.flag")
            var eventTimestamp = 0L
            var isSuccessfulEvent = true
            var takePhoto = false

            if (flagFile.exists()) {
                Log.d("UnlockReceiver", "¡Bandera de fallo encontrada! Se procesará la foto del intento fallido.")
                try {
                    // Leemos la hora original del fallo desde el archivo
                    eventTimestamp = flagFile.readText().toLong()
                    isSuccessfulEvent = false // Marcamos que este evento representa un fallo
                    takePhoto = true
                    // Borramos la bandera para no procesarla de nuevo
                    flagFile.delete()
                } catch (e: Exception) {
                    Log.e("UnlockReceiver", "Error al leer/borrar el archivo de bandera.", e)
                    // Si hay un error, procedemos como un evento normal y usamos la hora actual
                    eventTimestamp = System.currentTimeMillis()
                }
            } else {
                // No hay bandera, es un desbloqueo normal.
                // Comprobamos si el usuario quiere registrar también los accesos exitosos.
                val monitoringMode = prefs.getString("key_monitoring_mode", "failed")
                if (monitoringMode == "all") {
                    Log.d("UnlockReceiver", "Modo 'todos' activado. Se procesará la foto del intento exitoso.")
                    eventTimestamp = System.currentTimeMillis()
                    isSuccessfulEvent = true
                    takePhoto = true
                }
            }

            if (takePhoto) {
                Log.d("UnlockReceiver", "Lanzando CaptureActivity. ¿Fue exitoso? = $isSuccessfulEvent")
                val activityIntent = Intent(context, CaptureActivity::class.java).apply {
                    putExtra("IS_SUCCESSFUL", isSuccessfulEvent)
                    putExtra("EVENT_TIMESTAMP", eventTimestamp)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(activityIntent)
            } else {
                Log.d("UnlockReceiver", "No se cumplen las condiciones para tomar una foto.")
            }
        }
    }
}