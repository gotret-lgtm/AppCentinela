package com.example.appcentinela

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
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
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

// ¡IMPORTANTE! Hacemos que herede de LifecycleService para usar la cámara
class CaptureService : LifecycleService() {

    private lateinit var cameraExecutor: ExecutorService
    private val NOTIFICATION_ID = 101
    private val CHANNEL_ID = "CaptureServiceChannel"

    override fun onCreate() {
        super.onCreate()
        cameraExecutor = Executors.newSingleThreadExecutor()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)

        // 1. Iniciar como servicio en primer plano
        val notification = createNotification()
        startForeground(NOTIFICATION_ID, notification)

        Log.d("CaptureService", "Servicio iniciado. Preparando para capturar.")

        // 2. Iniciar la cámara y tomar la foto
        startCameraAndCapture(intent)

        // Indica al sistema que no reinicie el servicio si lo mata
        return START_NOT_STICKY
    }

    private fun startCameraAndCapture(intent: Intent?) {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener({
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()
            val imageCapture = ImageCapture.Builder().build()

            // Aseguramos que haya una cámara frontal disponible antes de continuar
            val cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA
            try {
                // Usamos el lifecycle del servicio para vincular la cámara
                cameraProvider.bindToLifecycle(this, cameraSelector, imageCapture)
                Log.d("CaptureService", "Cámara vinculada. Tomando foto...")
                takePhoto(imageCapture, intent)
            } catch (exc: Exception) {
                Log.e("CaptureService", "No se pudo vincular la cámara o no hay cámara frontal", exc)
                stopSelf() // Detener el servicio si falla
            }
        }, ContextCompat.getMainExecutor(this))
    }

    private fun takePhoto(imageCapture: ImageCapture, intent: Intent?) {
        val isSuccessful = intent?.getBooleanExtra("IS_SUCCESSFUL", false) ?: false
        val status = if (isSuccessful) "SUCCESS" else "FAILED"

        val fileName = "${SimpleDateFormat("yyyy-MM-dd-HH-mm-ss-SSS", Locale.US).format(System.currentTimeMillis())}_$status.jpg"
        val photoFile = File(filesDir, fileName)
        val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

        imageCapture.takePicture(
            outputOptions,
            cameraExecutor,
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    Log.d("CaptureService", "¡¡¡FOTO GUARDADA CON ÉXITO!!! en: ${output.savedUri}")
                    stopSelf() // Trabajo terminado, detener el servicio
                }

                override fun onError(exc: ImageCaptureException) {
                    Log.e("CaptureService", "Error al guardar la foto", exc)
                    stopSelf() // Trabajo fallido, detener el servicio
                }
            }
        )
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(
                CHANNEL_ID,
                "Canal del Servicio de Captura",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(serviceChannel)
        }
    }

    private fun createNotification(): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("App Centinela")
            .setContentText("Procesando evento de seguridad...")
            .setSmallIcon(R.mipmap.ic_launcher) // Asegúrate de tener este icono
            .build()
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
        Log.d("CaptureService", "Servicio destruido.")
    }

    override fun onBind(intent: Intent): IBinder? {
        super.onBind(intent)
        return null
    }
}