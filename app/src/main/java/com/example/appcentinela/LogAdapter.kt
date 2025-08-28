package com.example.appcentinela

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import java.util.*

class LogAdapter(
    private var logs: List<IntrusionLog>,
    private val onItemClicked: (IntrusionLog) -> Unit
) : RecyclerView.Adapter<LogAdapter.LogViewHolder>() {

    class LogViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val iconStatus: ImageView = view.findViewById(R.id.iconStatus)
        val textLogInfo: TextView = view.findViewById(R.id.textLogInfo)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LogViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.list_item_log, parent, false)
        return LogViewHolder(view)
    }

    override fun onBindViewHolder(holder: LogViewHolder, position: Int) {
        val log = logs[position]
        val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault())
        val dateString = sdf.format(Date(log.timestamp))
        val statusString = if (log.wasSuccessful) "Ingreso Exitoso" else "Ingreso Fallido"

        holder.textLogInfo.text = "$dateString - $statusString"

        // Cambia el icono según el estado
        val iconRes = if (log.wasSuccessful) android.R.drawable.ic_lock_idle_lock else android.R.drawable.ic_lock_lock
        holder.iconStatus.setImageResource(iconRes)

        holder.itemView.setOnClickListener {
            onItemClicked(log)
        }
    }

    override fun getItemCount() = logs.size
}
