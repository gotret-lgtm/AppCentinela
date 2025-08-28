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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d("CaptureActivity", "Actividad de captura iniciada.")
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

                // SOLUCIÓN #1: Esperar a que la cámara esté realmente abierta
                val cameraStateObserver = Observer<androidx.camera.core.CameraState> { cameraState ->
                    if (cameraState.type == androidx.camera.core.CameraState.Type.OPEN) {
                        Log.d("CaptureActivity", "Cámara confirmada como ABIERTA. Tomando foto...")
                        takePhoto(imageCapture)
                        // Una vez que tomamos la foto, dejamos de observar para no hacerlo de nuevo
                        camera.cameraInfo.cameraState.removeObservers(this)
                    }
                }
                camera.cameraInfo.cameraState.observe(this, cameraStateObserver)

            } catch (exc: Exception) {
                Log.e("CaptureActivity", "Fallo al vincular la cámara", exc)
                finishWithError()
            }
        }, ContextCompat.getMainExecutor(this))
    }

    private fun takePhoto(imageCapture: ImageCapture) {
        // Leemos el extra que nos pasaron para saber si el acceso fue exitoso o no
        val isSuccessful = intent.getBooleanExtra("IS_SUCCESSFUL", false)
        val status = if (isSuccessful) "SUCCESS" else "FAILED"

        // Creamos un nombre de archivo único que incluye la fecha y el estado del acceso
        val fileName = "${SimpleDateFormat("yyyy-MM-dd-HH-mm-ss-SSS", Locale.US).format(System.currentTimeMillis())}_$status.jpg"

        // Creamos el archivo de salida en el directorio interno de la app
        val photoFile = File(filesDir, fileName)

        // Configuramos las opciones de salida para la captura
        val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

        // Ejecutamos la captura de la imagen
        imageCapture.takePicture(
            outputOptions,
            cameraExecutor,
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    Log.d("CaptureActivity", "¡¡¡FOTO GUARDADA CON ÉXITO!!! en: ${output.savedUri}")
                    finish() // Cerramos la actividad invisible
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
        finish()
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }
}