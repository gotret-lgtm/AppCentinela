package com.example.appcentinela

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat

class MonitoringService : Service() {

    private val unlockReceiver = UnlockReceiver()

    override fun onCreate() {
        super.onCreate()
        // Registramos el receiver programáticamente
        val filter = IntentFilter(Intent.ACTION_USER_PRESENT)
        registerReceiver(unlockReceiver, filter)
        Log.d("MonitoringService", "Servicio creado y UnlockReceiver registrado.")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        createNotificationChannel()
        val notification = NotificationCompat.Builder(this, "MonitoringChannel")
            .setContentTitle("App Centinela")
            .setContentText("La protección para accesos exitosos está activa.")
            .setSmallIcon(R.mipmap.ic_launcher) // Asegúrate de que este icono existe
            .build()

        startForeground(2, notification) // Usamos un ID diferente al de CaptureService

        // START_STICKY asegura que el sistema intente reiniciar el servicio si lo mata
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        // Es crucial desregistrar el receiver cuando el servicio se destruye
        unregisterReceiver(unlockReceiver)
        Log.d("MonitoringService", "Servicio destruido y UnlockReceiver desregistrado.")
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null // No necesitamos vincularnos a este servicio
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(
                "MonitoringChannel",
                "Servicio de Monitorización",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            getSystemService(NotificationManager::class.java).createNotificationChannel(serviceChannel)
        }
    }
}