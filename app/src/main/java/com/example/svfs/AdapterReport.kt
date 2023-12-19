package com.example.svfs

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import java.util.Locale

class AdapterReport(private val records: List<report.VehicleRecord>) :
    RecyclerView.Adapter<AdapterReport.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view =
            LayoutInflater.from(parent.context).inflate(R.layout.item_report, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val record = records[position]
        Log.d("AdapterReport", "Binding data: $record")
        holder.bind(record)
    }

    override fun getItemCount(): Int = records.size

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val vehicleNumberTextView: TextView = itemView.findViewById(R.id.rtvVehicleNo)
        val dateTextView: TextView = itemView.findViewById(R.id.rtvDate)
        val fillingAmountTextView: TextView = itemView.findViewById(R.id.rtvfilling)
        fun bind(record: report.VehicleRecord) {
            val dateFormatter = SimpleDateFormat("dd/MM/yy", Locale.getDefault())
            val formattedDate = dateFormatter.format(record.date)
                vehicleNumberTextView.text = record.vehicleNumber
                dateTextView.text = "Date: $formattedDate"
                fillingAmountTextView.text = record.fillingAmount.toString()
        }
    }
}
