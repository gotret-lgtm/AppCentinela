package com.example.appcentinela

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build

// Esta es la clase de aplicación personalizada que vamos a crear.
// Hereda de Application para que se comporte como el punto de entrada de la app.
class AppCentinela : Application() {

    companion object {
        // ID constante para el canal de notificaciones que usará el servicio.
        const val CHANNEL_ID = "CaptureServiceChannel"
    }

    override fun onCreate() {
        super.onCreate()
        // Esta función se llama una sola vez cuando la aplicación se inicia.
        // Es el lugar perfecto para crear el canal de notificaciones.
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        // Los canales de notificación solo son necesarios para Android 8.0 (API 26) y superior.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(
                CHANNEL_ID,
                "Canal del Servicio de Captura", // Nombre visible para el usuario en los ajustes
                NotificationManager.IMPORTANCE_LOW // Usamos LOW para que sea menos intrusiva
            ).apply {
                description = "Canal para la notificación del servicio de AppCentinela"
            }

            // Obtener el NotificationManager del sistema y registrar el canal.
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(serviceChannel)
        }
    }
}