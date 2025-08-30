package com.example.appcentinela

import android.app.Notification
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.util.Log
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleService
import com.google.common.util.concurrent.ListenableFuture
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class CaptureService : LifecycleService() {

    private lateinit var cameraProviderFuture: ListenableFuture<ProcessCameraProvider>
    private var imageCapture: ImageCapture? = null
    private val NOTIFICATION_ID = 1

    override fun onCreate() {
        super.onCreate()
        cameraProviderFuture = ProcessCameraProvider.getInstance(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        val notification = createNotification()
        startForeground(NOTIFICATION_ID, notification)
        startCameraAndCapture()
        return START_NOT_STICKY
    }

    private fun startCameraAndCapture() {
        // Primero guardamos el log en SharedPreferences, luego intentamos tomar la foto.
        saveSuccessLog()

        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()
            bindCamera(cameraProvider)
        }, ContextCompat.getMainExecutor(this))
    }

    // --- LÓGICA DE GUARDADO EN SHAREDPREFERENCES (LA ÚNICA QUE USA TU APP) ---

    private fun saveSuccessLog() {
        val timestamp: Long = System.currentTimeMillis()
        val wasSuccessful: Boolean = true
        // CORREGIDO: Usamos una cadena vacía en lugar de null
        val photoPath: String = ""

        try {
            val sharedPreferences = getSharedPreferences("AppCentinelaPrefs", Context.MODE_PRIVATE)
            val editor = sharedPreferences.edit()
            val gson = Gson()

            val logs = loadLogs(sharedPreferences).toMutableList()

            // Ahora los tipos coinciden perfectamente con la definición de IntrusionLog
            val newLog = IntrusionLog(
                timestamp = timestamp,
                wasSuccessful = wasSuccessful,
                photoPath = photoPath
            )
            logs.add(0, newLog)

            val jsonLogs = gson.toJson(logs)
            editor.putString("intrusion_logs", jsonLogs)
            editor.apply()

            Log.d("CaptureService", "Registro de 'Ingreso Exitoso' guardado en SharedPreferences.")

        } catch (e: Exception) {
            Log.e("CaptureService", "Error al guardar el log en SharedPreferences", e)
        }
    }

    private fun loadLogs(sharedPreferences: SharedPreferences): List<IntrusionLog> {
        val gson = Gson()
        val json = sharedPreferences.getString("intrusion_logs", null)
        return if (json != null) {
            val type = object : TypeToken<ArrayList<IntrusionLog>>() {}.type
            gson.fromJson(json, type)
        } else {
            emptyList()
        }
    }

    // --- LÓGICA DE LA CÁMARA (SIN CAMBIOS) ---

    private fun createNotification(): Notification {
        return NotificationCompat.Builder(this, AppCentinela.CHANNEL_ID)
            .setContentTitle("AppCentinela")
            .setContentText("Protección activa, procesando evento.")
            // CORREGIDO: Usamos un icono de sistema de Android que siempre existe.
            .setSmallIcon(android.R.drawable.ic_lock_lock)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun bindCamera(cameraProvider: ProcessCameraProvider) {
        val cameraSelector = CameraSelector.Builder()
            .requireLensFacing(CameraSelector.LENS_FACING_FRONT)
            .build()
        imageCapture = ImageCapture.Builder().build()
        try {
            cameraProvider.unbindAll()
            cameraProvider.bindToLifecycle(this, cameraSelector, imageCapture)
            takePicture()
        } catch (exc: Exception) {
            Log.e("CaptureService", "Error al vincular la cámara: ${exc.message}")
            stopServiceAndCleanup()
        }
    }

    private fun takePicture() {
        val imageCapture = this.imageCapture ?: run {
            Log.e("CaptureService", "ImageCapture no está inicializado.")
            stopServiceAndCleanup()
            return
        }
        val photoFile = createFile()
        val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()
        imageCapture.takePicture(
            outputOptions,
            ContextCompat.getMainExecutor(this),
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                    val msg = "Foto guardada con éxito: ${outputFileResults.savedUri}"
                    Log.d("CaptureService", msg)
                    // Aquí se podría actualizar el log con la ruta de la imagen, pero no es esencial para el fix.
                    stopServiceAndCleanup()
                }
                override fun onError(exception: ImageCaptureException) {
                    Log.e("CaptureService", "Error al guardar la foto", exception)
                    stopServiceAndCleanup()
                }
            }
        )
    }

    private fun createFile(): File {
        val outputDirectory = getOutputDirectory()
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(System.currentTimeMillis())
        return File(outputDirectory, "IMG_$timeStamp.jpg")
    }

    private fun getOutputDirectory(): File {
        val mediaDir = externalMediaDirs.firstOrNull()?.let {
            File(it, resources.getString(R.string.app_name)).apply { mkdirs() }
        }
        return if (mediaDir != null && mediaDir.exists())
            mediaDir else filesDir
    }

    private fun stopServiceAndCleanup() {
        Log.d("CaptureService", "Tarea completada. Deteniendo el servicio.")
        cameraProviderFuture.get()?.unbindAll()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d("CaptureService", "Servicio destruido.")
    }
}