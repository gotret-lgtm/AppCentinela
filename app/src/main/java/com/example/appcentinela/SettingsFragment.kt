package com.example.appcentinela

import android.content.Intent
import android.os.Bundle
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.SwitchPreferenceCompat

class SettingsFragment : PreferenceFragmentCompat() {

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.settings_preferences, rootKey)

        // Buscamos el interruptor de los accesos exitosos
        val successfulAttemptsSwitch = findPreference<SwitchPreferenceCompat>("key_log_successful_attempts")

        // Añadimos un listener para saber cuándo cambia su valor
        successfulAttemptsSwitch?.setOnPreferenceChangeListener { _, newValue ->
            val isEnabled = newValue as Boolean
            if (isEnabled) {
                // Si el usuario lo activa, iniciamos el servicio de monitorización
                activity?.startService(Intent(activity, MonitoringService::class.java))
            } else {
                // Si el usuario lo desactiva, detenemos el servicio
                activity?.stopService(Intent(activity, MonitoringService::class.java))
            }
            true // Indicamos que hemos manejado el cambio
        }
    }
}