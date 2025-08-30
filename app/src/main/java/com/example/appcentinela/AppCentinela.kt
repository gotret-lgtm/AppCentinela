package com.example.appcentinela

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build

// ELIMINADO: La anotación @HiltAndroidApp
class AppCentinela : Application() {

    companion object {
        const val CHANNEL_ID = "CaptureServiceChannel"
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(
                CHANNEL_ID,
                "Canal del Servicio de Captura",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Canal para la notificación del servicio de AppCentinela"
            }

            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(serviceChannel)
        }
    }
}