package com.example.appcentinela

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service.START_STICKY
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleService
import java.io.File
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class CaptureService : LifecycleService() {

    private lateinit var cameraExecutor: ExecutorService
    private var imageCapture: ImageCapture? = null

    override fun onCreate() {
        super.onCreate()
        cameraExecutor = Executors.newSingleThreadExecutor()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)

        // Para Android 8 y superior, el servicio debe mostrar una notificación
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "capture_channel",
                "Capture Service",
                NotificationManager.IMPORTANCE_LOW
            )
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)

            val notification: Notification = NotificationCompat.Builder(this, "capture_channel")
                .setContentTitle("AppCentinela")
                .setContentText("Protección activa.")
                .build()

            startForeground(1, notification)
        }

        Log.d("CaptureService", "Servicio iniciado. Intentando tomar foto...")
        startCameraAndCapture()

        return START_STICKY
    }

    private fun startCameraAndCapture() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener({
            // Obtenemos el proveedor de la cámara
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()

            // Configuramos el caso de uso para la captura de imagen
            imageCapture = ImageCapture.Builder().build()

            // Seleccionamos la cámara FRONTAL
            val cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA

            try {
                // Desvinculamos cualquier cámara que estuviera en uso
                cameraProvider.unbindAll()

                // Vinculamos la cámara a este ciclo de vida del servicio
                cameraProvider.bindToLifecycle(this, cameraSelector, imageCapture)

                // Una vez vinculada, tomamos la foto
                takePhoto()

            } catch (exc: Exception) {
                Log.e("CaptureService", "Error al vincular la cámara", exc)
                stopSelf() // Detenemos el servicio si hay un error
            }

        }, ContextCompat.getMainExecutor(this))
    }

    private fun takePhoto() {
        val imageCapture = imageCapture ?: return

        // Creamos un archivo con un nombre único basado en la fecha y hora
        val photoFile = File(
            getExternalFilesDir(null),
            SimpleDateFormat("yyyy-MM-dd-HH-mm-ss-SSS", Locale.US)
                .format(System.currentTimeMillis()) + ".jpg"
        )

        val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

        imageCapture.takePicture(
            outputOptions,
            cameraExecutor,
            object : ImageCapture.OnImageSavedCallback {
                override fun onError(exc: ImageCaptureException) {
                    Log.e("CaptureService", "Error al guardar la foto: ${exc.message}", exc)
                    stopSelf() // Detenemos el servicio en caso de error
                }

                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    val msg = "Foto guardada con éxito en: ${output.savedUri}"
                    Log.d("CaptureService", msg)
                    stopSelf() // ¡Trabajo completado! Detenemos el servicio.
                }
            }
        )
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }
}