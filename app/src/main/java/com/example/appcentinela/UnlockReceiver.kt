package com.example.appcentinela

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.preference.PreferenceManager
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf

class UnlockReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_USER_PRESENT) {
            val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
            val isProtectionEnabled = sharedPreferences.getBoolean("key_protection_enabled", false)
            val shouldLogSuccessful = sharedPreferences.getBoolean("key_log_successful_attempts", false)

            Log.d("AppCentinelaDebug", "UnlockReceiver -> ProtectionEnabled: $isProtectionEnabled, ShouldLogSuccessful: $shouldLogSuccessful")

            if (isProtectionEnabled && shouldLogSuccessful) {
                Log.d("AppCentinelaDebug", "CONDICIÓN EXITOSA CUMPLIDA. Encolando trabajo en WorkManager.")

                // 1. Creamos los datos que le pasaremos al Worker
                val inputData = workDataOf("IS_SUCCESSFUL" to true)

                // 2. Creamos la solicitud de trabajo para nuestro CaptureWorker
                val captureWorkRequest = OneTimeWorkRequestBuilder<CaptureWorker>()
                    .setInputData(inputData)
                    .build()

                // 3. Le entregamos la solicitud a WorkManager para que la ejecute
                WorkManager.getInstance(context).enqueue(captureWorkRequest)
            }
        }
    }
}