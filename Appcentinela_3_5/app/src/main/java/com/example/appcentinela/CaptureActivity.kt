package com.example.appcentinela

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.lifecycle.Observer
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class CaptureActivity : AppCompatActivity() {

    private lateinit var cameraExecutor: ExecutorService

    companion object {
        @Volatile
        private var isCapturing = false // Para evitar capturas solapadas
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d("CaptureActivity", "Actividad de captura iniciada.")

        if (isCapturing) {
            Log.w("CaptureActivity", "Intento de captura solapado. Ignorando.")
            finish()
            return
        }
        isCapturing = true

        cameraExecutor = Executors.newSingleThreadExecutor()
        startCameraAndCapture()
    }

    private fun startCameraAndCapture() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener({
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()
            val preview = Preview.Builder().build()
            val imageCapture = ImageCapture.Builder().build()
            val cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA

            try {
                cameraProvider.unbindAll()
                val camera = cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageCapture)

                // Esperamos a que la cámara esté realmente abierta
                camera.cameraInfo.cameraState.observe(this, object : Observer<androidx.camera.core.CameraState> {
                    override fun onChanged(cameraState: androidx.camera.core.CameraState) {
                        if (cameraState.type == androidx.camera.core.CameraState.Type.OPEN) {
                            Log.d("CaptureActivity", "Cámara confirmada como ABIERTA. Tomando foto...")
                            takePhoto(imageCapture)
                            // Dejamos de observar para no tomar la foto múltiples veces
                            camera.cameraInfo.cameraState.removeObserver(this)
                        }
                    }
                })

            } catch (exc: Exception) {
                Log.e("CaptureActivity", "Fallo al vincular la cámara", exc)
                finishWithError()
            }
        }, ContextCompat.getMainExecutor(this))
    }

    private fun takePhoto(imageCapture: ImageCapture) {
        // --- ¡AQUÍ ESTÁ LA CORRECCIÓN CLAVE! ---
        val isSuccessful = intent.getBooleanExtra("IS_SUCCESSFUL", false)
        // Obtenemos la hora del evento que nos pasó el UnlockReceiver.
        // Si no viene, usamos la hora actual como respaldo.
        val eventTimestamp = intent.getLongExtra("EVENT_TIMESTAMP", System.currentTimeMillis())
        // -----------------------------------------

        val status = if (isSuccessful) "SUCCESS" else "FAILED"
        val sdf = SimpleDateFormat("yyyy-MM-dd-HH-mm-ss-SSS", Locale.US)

        // Usamos la hora del evento para el nombre del archivo.
        val fileName = "${sdf.format(Date(eventTimestamp))}_$status.jpg"

        val photoFile = File(filesDir, fileName)
        val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

        imageCapture.takePicture(
            outputOptions,
            cameraExecutor,
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    Log.d("CaptureActivity", "¡FOTO GUARDADA CON ÉXITO! en: ${output.savedUri}")
                    finishAndRelease()
                }

                override fun onError(exc: ImageCaptureException) {
                    Log.e("CaptureActivity", "Error al guardar la foto", exc)
                    finishWithError()
                }
            }
        )
    }

    private fun finishWithError() {
        runOnUiThread {
            Toast.makeText(this, "Error al capturar imagen", Toast.LENGTH_SHORT).show()
        }
        finishAndRelease()
    }

    private fun finishAndRelease() {
        isCapturing = false // Liberamos el "cerrojo"
        finish()
    }

    override fun onDestroy() {
        super.onDestroy()
        isCapturing = false // Doble seguridad
        if (::cameraExecutor.isInitialized) {
            cameraExecutor.shutdown()
        }
    }

}