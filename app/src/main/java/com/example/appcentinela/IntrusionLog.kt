package com.example.appcentinela

// Importamos la interfaz Serializable
import java.io.Serializable

// Hacemos que la data class implemente la interfaz Serializable
data class IntrusionLog(
    val timestamp: Long,
    val wasSuccessful: Boolean,
    val photoPath: String
) : Serializable