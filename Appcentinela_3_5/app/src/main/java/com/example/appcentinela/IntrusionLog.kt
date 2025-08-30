package com.example.appcentinela

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class IntrusionLog(
    val timestamp: Long,      // La hora del evento en milisegundos
    val wasSuccessful: Boolean, // ¿Fue un intento exitoso o fallido?
    val photoPath: String,      // La ruta al archivo de la foto
    val videoPath: String? = null, // La ruta al video (para el futuro)
) : Parcelable