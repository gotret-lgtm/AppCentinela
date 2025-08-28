package com.example.appcentinela

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

class SettingsActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Carga el fragmento de configuración en el contenedor de la actividad
        supportFragmentManager
            .beginTransaction()
            .replace(android.R.id.content, SettingsFragment())
            .commit()
        // Añade una flecha para volver atrás en la barra de acción
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }
}