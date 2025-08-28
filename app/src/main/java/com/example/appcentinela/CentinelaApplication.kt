package com.example.appcentinela

import android.app.Application
import androidx.camera.camera2.Camera2Config
import androidx.camera.core.CameraXConfig

// Hacemos que la aplicación implemente CameraXConfig.Provider
class CentinelaApplication : Application(), CameraXConfig.Provider {

    // Proporcionamos la configuración personalizada de CameraX
    override fun getCameraXConfig(): CameraXConfig {
        return Camera2Config.defaultConfig()
    }
}