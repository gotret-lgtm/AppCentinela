package com.example.appcentinela

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.content.ContextCompat

class UnlockReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_USER_PRESENT) {
            Log.d("AppCentinelaDebug", "UnlockReceiver (programático) recibió el evento de desbloqueo.")

            // No necesitamos volver a comprobar las preferencias, porque si este receiver está
            // activo es porque el usuario ya activó la opción.

            val serviceIntent = Intent(context, CaptureService::class.java).apply {
                putExtra("IS_SUCCESSFUL", true)
            }
            ContextCompat.startForegroundService(context, serviceIntent)
        }
    }
}
