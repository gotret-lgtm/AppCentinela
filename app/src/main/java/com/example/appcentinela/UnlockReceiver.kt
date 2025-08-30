package com.example.appcentinela

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log

class UnlockReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_USER_PRESENT) {
            Log.d("AppCentinelaDebug", "UnlockReceiver (programático) recibió el evento de desbloqueo.")
            val serviceIntent = Intent(context, CaptureService::class.java)

            // MODIFICADO: Usar startForegroundService para Android 8+
            // Esto es crucial para evitar que el sistema cierre la app por iniciar un servicio en segundo plano.
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(serviceIntent)
            } else {
                context.startService(serviceIntent)
            }
        }
    }
}