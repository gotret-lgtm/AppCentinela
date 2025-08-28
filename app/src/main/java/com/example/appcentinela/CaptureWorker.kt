package com.example.appcentinela

import android.content.Context
import android.util.Log
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.google.common.util.concurrent.ListenableFuture
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.ExecutionException
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

class CaptureWorker(private val appContext: Context, workerParams: WorkerParameters) :
    CoroutineWorker(appContext, workerParams) {

    // Creamos una instancia de nuestro LifecycleOwner personalizado
    private val lifecycleOwner = ProcessLifecycleOwner()

    override suspend fun doWork(): Result {
        val isSuccessful = inputData.getBoolean("IS_SUCCESSFUL", false)
        Log.d("CaptureWorker", "Trabajo iniciado. Es exitoso: $isSuccessful")

        // Marcamos nuestro ciclo de vida como "iniciado"
        lifecycleOwner.start()

        return try {
            val cameraProvider = ProcessCameraProvider.getInstance(appContext).await()
            val imageCapture = initializeCamera(cameraProvider)
            takePhoto(imageCapture, isSuccessful).await()

            Log.d("CaptureWorker", "Trabajo finalizado con éxito.")
            Result.success()
        } catch (e: Exception) {
            Log.e("CaptureWorker", "El trabajo falló", e)
            Result.failure()
        } finally {
            // Nos aseguramos de marcar el ciclo de vida como "destruido" al final
            lifecycleOwner.destroy()
        }
    }

    private fun initializeCamera(cameraProvider: ProcessCameraProvider): ImageCapture {
        val imageCapture = ImageCapture.Builder().build()
        val cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA

        // Desvinculamos todo antes de vincular de nuevo para evitar errores
        cameraProvider.unbindAll()

        // ===== CORRECCIÓN CLAVE =====
        // Ahora le pasamos nuestro LifecycleOwner personalizado y válido
        cameraProvider.bindToLifecycle(
            lifecycleOwner,
            cameraSelector,
            imageCapture
        )
        return imageCapture
    }

    private fun takePhoto(imageCapture: ImageCapture, isSuccessful: Boolean): ListenableFuture<ImageCapture.OutputFileResults> {
        val status = if (isSuccessful) "SUCCESS" else "FAILED"
        val fileName = "${SimpleDateFormat("yyyy-MM-dd-HH-mm-ss-SSS", Locale.US).format(System.currentTimeMillis())}_$status.jpg"
        val photoFile = File(appContext.filesDir, fileName)
        val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

        // Creamos un Future para poder usarlo con corutinas
        val future = SettableFuture.create<ImageCapture.OutputFileResults>()
        imageCapture.takePicture(
            outputOptions,
            ContextCompat.getMainExecutor(appContext),
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                    Log.d("CaptureWorker", "FOTO GUARDADA CON ÉXITO en: ${outputFileResults.savedUri}")
                    future.set(outputFileResults)
                }
                override fun onError(exception: ImageCaptureException) {
                    Log.e("CaptureWorker", "Error al guardar la foto", exception)
                    future.setException(exception)
                }
            }
        )
        return future
    }

    // --- Clases y funciones de ayuda ---

    // Un LifecycleOwner simple para tareas en segundo plano
    private class ProcessLifecycleOwner : LifecycleOwner {
        private val lifecycleRegistry = LifecycleRegistry(this)

        fun start() {
            lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START)
        }

        fun destroy() {
            lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        }

        override val lifecycle: Lifecycle
            get() = lifecycleRegistry
    }

    // Adaptador para usar ListenableFuture con Corutinas
    private suspend fun <T> ListenableFuture<T>.await(): T = suspendCoroutine { continuation ->
        addListener({
            try {
                continuation.resume(get())
            } catch (e: ExecutionException) {
                continuation.resumeWithException(e.cause ?: e)
            } catch (e: Exception) {
                continuation.resumeWithException(e)
            }
        }, ContextCompat.getMainExecutor(appContext))
    }

    // Implementación simple de un ListenableFuture que podemos completar nosotros
    private class SettableFuture<T> private constructor() : ListenableFuture<T> {
        // ... (Implementación completa de SettableFuture)
        companion object { fun <T> create(): SettableFuture<T> = SettableFuture() }
        private var value: T? = null
        private var exception: Exception? = null
        private var isDone = false
        private val listeners = mutableListOf<Runnable>()

        fun set(value: T) {
            this.value = value
            isDone = true
            listeners.forEach { it.run() }
        }
        fun setException(exception: Exception) {
            this.exception = exception
            isDone = true
            listeners.forEach { it.run() }
        }
        override fun addListener(listener: Runnable, executor: java.util.concurrent.Executor) {
            executor.execute(listener)
        }
        override fun cancel(mayInterruptIfRunning: Boolean): Boolean = false
        override fun isCancelled(): Boolean = false
        override fun isDone(): Boolean = isDone
        override fun get(): T {
            exception?.let { throw it }
            return value!!
        }
        override fun get(timeout: Long, unit: java.util.concurrent.TimeUnit): T = get()
    }
}