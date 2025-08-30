package com.example.appcentinela

import android.app.Activity
import android.os.Build
import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class DetailActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_detail)

        // Usamos una forma más moderna y segura de obtener el Parcelable
        val log: IntrusionLog? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra("LOG_DATA", IntrusionLog::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableExtra("LOG_DATA")
        }


        if (log == null) {
            finish() // Si no hay datos, cerramos
            return
        }

        // Obtenemos las vistas
        val textDate: TextView = findViewById(R.id.textDetailDate)
        val textTime: TextView = findViewById(R.id.textDetailTime)
        val textStatus: TextView = findViewById(R.id.textDetailStatus)
        val imageView: ImageView = findViewById(R.id.imageDetailPhoto)
        val btnDelete: Button = findViewById(R.id.btnDelete)

        // Formateamos y mostramos los datos
        val date = Date(log.timestamp)
        textDate.text = "Fecha: ${SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(date)}"
        textTime.text = "Hora: ${SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(date)}"
        textStatus.text = "Estado: ${if (log.wasSuccessful) "Exitoso" else "Fallido"}"

        // Cargamos la imagen con Glide
        Glide.with(this)
            .load(File(log.photoPath))
            .into(imageView)

        // Lógica del botón de borrado
        btnDelete.setOnClickListener {
            showDeleteConfirmationDialog(log)
        }
    }

    private fun showDeleteConfirmationDialog(log: IntrusionLog) {
        AlertDialog.Builder(this)
            .setTitle("Confirmar borrado")
            .setMessage("¿Estás seguro de que quieres borrar este registro? Esta acción no se puede deshacer.")
            .setPositiveButton("Borrar") { _, _ ->
                deleteLog(log)
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun deleteLog(log: IntrusionLog) {
        val photoFile = File(log.photoPath)
        if (photoFile.exists()) {
            photoFile.delete()
        }
        // Aquí también borraríamos el video si existiera

        // Enviamos una señal de vuelta a GalleryActivity para que sepa que debe recargar la lista
        setResult(Activity.RESULT_OK)
        finish() // Cerramos la vista de detalle
    }
}