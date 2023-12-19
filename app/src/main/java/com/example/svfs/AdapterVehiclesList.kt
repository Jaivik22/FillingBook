package com.example.svfs

import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class AdapterVehiclesList(private var vehicles: List<VehiclesList.Vehicle>) : RecyclerView.Adapter<AdapterVehiclesList.ViewHolder>() {



    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val textVehicleNumber: TextView = itemView.findViewById(R.id.textVehicleNumber)
        val textDriverName: TextView = itemView.findViewById(R.id.textDriverName)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val itemView = LayoutInflater.from(parent.context).inflate(R.layout.item_vehicle_list, parent, false)

        itemView.setOnClickListener {
            val position = it.tag as Int  // Get the clicked item's position
            val vehicle = vehicles[position]

            val intent = Intent(parent.context, FillingEntryPage::class.java)
            intent.putExtra("vehicle", vehicle)
            parent.context.startActivity(intent)
        }

        return ViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val vehicle = vehicles[position]
        holder.textVehicleNumber.text = "${vehicle.vehicleNumber.toUpperCase()}"
        holder.textDriverName.text = "${vehicle.driverName}"

        // Store the item's position as a tag to retrieve it later in the click listener
        holder.itemView.tag = position
    }

    override fun getItemCount(): Int {
        return vehicles.size
    }

    fun setData(newData: List<VehiclesList.Vehicle>) {
        vehicles = newData
        notifyDataSetChanged()
    }

}