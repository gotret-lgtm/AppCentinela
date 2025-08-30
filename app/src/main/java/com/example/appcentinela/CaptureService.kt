package com.example.appcentinela

import android.app.Notification
import android.util.Log
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleService
import com.google.common.util.concurrent.ListenableFuture
import com.example.appcentinela.data.LogEntry
import com.example.appcentinela.data.LogEntryDao
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Locale
import javax.inject.Inject

// AÑADIDO: Anotación para permitir la inyección de dependencias con Hilt.
@AndroidEntryPoint
class CaptureService : LifecycleService() {

    // AÑADIDO: Inyección de la dependencia del DAO para acceder a la base de datos.
    @Inject
    lateinit var logEntryDao: LogEntryDao

    // AÑADIDO: Job y Scope para ejecutar operaciones de base de datos en un hilo secundario.
    private val serviceJob = SupervisorJob()
    private val serviceScope = CoroutineScope(Dispatchers.IO + serviceJob)

    private lateinit var cameraProviderFuture: ListenableFuture<ProcessCameraProvider>
    private var imageCapture: ImageCapture? = null

    // AÑADIDO: ID constante para la notificación.
    private val NOTIFICATION_ID = 1

    override fun onCreate() {
        super.onCreate()
        cameraProviderFuture = ProcessCameraProvider.getInstance(this)
    }

    // MODIFICADO: Este es el corazón del Foreground Service.
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)

        // Crear y mostrar la notificación para poner el servicio en primer plano.
        val notification = createNotification()
        startForeground(NOTIFICATION_ID, notification)

        // Iniciar la lógica de captura.
        startCameraAndCapture()

        // Usamos START_NOT_STICKY porque solo queremos que se ejecute cuando se le llama,
        // no que el sistema lo reinicie por su cuenta.
        return START_NOT_STICKY
    }

    // AÑADIDO: Función para crear la notificación del servicio.
    private fun createNotification(): Notification {
        return NotificationCompat.Builder(this, AppCentinela.CHANNEL_ID)
            .setContentTitle("AppCentinela")
            .setContentText("Protección activa, procesando evento.")
            .setSmallIcon(R.drawable.ic_security) // IMPORTANTE: Crea un icono llamado 'ic_security' en tu carpeta drawable.
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun startCameraAndCapture() {
        // MODIFICADO: Desacoplamos la creación del log. Lo creamos inmediatamente.
        // De esta forma, aunque falle la foto, el desbloqueo queda registrado.
        createSuccessLogEntry()

        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()
            bindCamera(cameraProvider)
        }, ContextCompat.getMainExecutor(this))
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
            stopServiceAndCleanup() // Asegurarse de parar si hay un error aquí
        }
    }

    private fun takePicture() {
        val imageCapture = this.imageCapture ?: run {
            Log.e("CaptureService", "ImageCapture no está inicializado.")
            stopServiceAndCleanup() // Parar si imageCapture es nulo
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
                    // La tarea ha finalizado, ahora podemos detener el servicio.
                    stopServiceAndCleanup()
                }

                override fun onError(exception: ImageCaptureException) {
                    Log.e("CaptureService", "Error al guardar la foto", exception)
                    // La tarea ha fallado, pero también debemos detener el servicio.
                    stopServiceAndCleanup()
                }
            }
        )
    }

    // AÑADIDO: Función para crear el registro de "Ingreso Exitoso" en la base de datos.
    private fun createSuccessLogEntry() {
        serviceScope.launch {
            val logEntry = LogEntry(
                timestamp = System.currentTimeMillis(),
                type = "Ingreso Exitoso",
                imagePath = null // El path se podría actualizar después si la foto se guarda bien
            )
            logEntryDao.insert(logEntry)
            Log.d("CaptureService", "Registro de Ingreso Exitoso creado en la base de datos.")
        }
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

    // AÑADIDO: Función centralizada para detener el servicio y limpiar recursos.
    private fun stopServiceAndCleanup() {
        Log.d("CaptureService", "Tarea completada. Deteniendo el servicio.")
        cameraProviderFuture.get()?.unbindAll()
        // Detiene el modo de primer plano y elimina la notificación.
        stopForeground(STOP_FOREGROUND_REMOVE)
        // Detiene el servicio completamente.
        stopSelf()
    }

    override fun onDestroy() {
        super.onDestroy()
        // Cancelar todas las coroutines cuando el servicio es destruido.
        serviceJob.cancel()
        Log.d("CaptureService", "Servicio destruido.")
    }
}