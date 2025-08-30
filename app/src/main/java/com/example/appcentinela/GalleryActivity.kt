package com.example.appcentinela

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import java.io.File
import java.text.SimpleDateFormat
import java.util.*


class GalleryActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: LogAdapter

    // Lanzador para esperar un resultado de DetailActivity
    private val detailActivityResultLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        // Si volvemos de la vista de detalle y el resultado es OK (porque se borró un log),
        // recargamos la lista.
        if (result.resultCode == Activity.RESULT_OK) {
            loadLogs()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_gallery)
        recyclerView = findViewById(R.id.recyclerViewLogs)
        recyclerView.layoutManager = LinearLayoutManager(this)
        loadLogs()
    }

    private fun loadLogs() {
        val logs = mutableListOf<IntrusionLog>()
        val filesDir = this.filesDir
        val imageFiles = filesDir.listFiles { file -> file.extension == "jpg" } ?: emptyArray()
        val sdf = SimpleDateFormat("yyyy-MM-dd-HH-mm-ss-SSS", Locale.US)

        for (file in imageFiles) {
            try {
                // Separamos el nombre del archivo en partes: "fecha_ESTADO"
                val nameParts = file.nameWithoutExtension.split('_')
                if (nameParts.size == 2) {
                    val dateString = nameParts[0]
                    val statusString = nameParts[1]

                    val timestamp = sdf.parse(dateString)?.time ?: 0
                    val wasSuccessful = statusString == "SUCCESS"

                    logs.add(IntrusionLog(timestamp, wasSuccessful, file.absolutePath))
                }
            } catch (e: Exception) {
                Log.e("GalleryActivity", "Error al parsear el nombre del archivo: ${file.name}", e)
            }
        }


        // Ordenar los registros del más reciente al más antiguo
        logs.sortByDescending { it.timestamp }

        adapter = LogAdapter(logs) { log ->
            val intent = Intent(this, DetailActivity::class.java)
            intent.putExtra("LOG_DATA", log)
            detailActivityResultLauncher.launch(intent) // Usamos el lanzador
        }
        recyclerView.adapter = adapter
    }
}
