package com.example.svfs

import AdapterFillingEntry
import  android.content.Context
import android.content.SharedPreferences
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextClock
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.svfs.databinding.ItemGiftedVehicleBinding
import com.example.svfs.databinding.PopupFillingDetailsBinding
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.Tasks
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import java.text.SimpleDateFormat
import java.util.Locale

class AdapteGiftPage(private val context: Context, private val refreshListener: OnRefreshListener) : RecyclerView.Adapter<AdapteGiftPage.ViewHolder>() {
    private var giftedVehicles: List<GiftPage.GiftedVehicle> = ArrayList()
    private val db = FirebaseFirestore.getInstance()
    private  var sharedPreferences:SharedPreferences = context.getSharedPreferences("MyPrefs", AppCompatActivity.MODE_PRIVATE)
    private var userID:String = ""

    fun setGiftedVehicles(giftedVehicles: List<GiftPage.GiftedVehicle>) {
        this.giftedVehicles = giftedVehicles
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemGiftedVehicleBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val giftedVehicle = giftedVehicles[position]
        holder.bind(giftedVehicle)
    }

    override fun getItemCount(): Int {
        return giftedVehicles.size
    }

    inner class ViewHolder(private val binding: ItemGiftedVehicleBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(giftedVehicle: GiftPage.GiftedVehicle) {
            binding.gtvvehicleNumber.text = giftedVehicle.vehicleNumber.toUpperCase()
            binding.gtvdate.text = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(giftedVehicle.date)

            binding.root.setOnClickListener {
                showFillingDetailsPopup(binding.root.context, giftedVehicle)
            }
        }
    }

    // Add this code inside the GiftedVehiclesAdapter class, after the ViewHolder class

    private fun showFillingDetailsPopup(context: Context, giftedVehicle: GiftPage.GiftedVehicle) {
        val dialogView = LayoutInflater.from(context).inflate(R.layout.popup_filling_details, null)
        val popupVehicleNumber: TextView = dialogView.findViewById(R.id.popupVehicleNumber)
        val rv:RecyclerView  = dialogView.findViewById(R.id.pgrv)
        val btnCancel: Button = dialogView.findViewById(R.id.btnCancel)
        val btnGift: Button = dialogView.findViewById(R.id.btnGift)

        popupVehicleNumber.text = giftedVehicle.vehicleNumber.toUpperCase()

        val userID = sharedPreferences.getString("storedUserId", "userID").toString()

        val giftedVehicleEntryRef = db.collection("Profiles").document(userID).collection("GiftedVehicles")
            .document(giftedVehicle.documentId)
            .collection("Entries")

        // Create a list of FillingEntry objects to populate the RecyclerView
        val fillingEntries = ArrayList<FillingEntryPage.FillingEntry>()

        giftedVehicleEntryRef.get()
            .addOnSuccessListener { result ->
                for (document in result) {
                    val filling = document.getDouble("filling")
                    val date = document.getDate("date")
                    val extra = document.getBoolean("extra")
                    val photoUrl = document.getString("photoUrl") // Assuming the field name for photo URL is "photoUrl"
                    val id= document.id

                    if (filling != null && date != null) {
                        fillingEntries.add(
                            FillingEntryPage.FillingEntry(filling, date, id, extra, photoUrl)
                        )
                    }
                }

                fillingEntries.sortByDescending { it.date } // Sort entries by date
                val adapter = AdapterGiftFillingEntry(fillingEntries)
                rv.adapter = adapter

                rv.adapter = adapter
                rv.layoutManager = LinearLayoutManager(context)

                // ... (existing code)

                val alertDialog = AlertDialog.Builder(context)
                    .setView(dialogView)
                    .setCancelable(true)
                    .create()

                btnCancel.setOnClickListener {
                    alertDialog.dismiss()
                }

                btnGift.setOnClickListener {
                    // Delete the document
                    db.collection("Profiles").document(userID).collection("GiftedVehicles")
                        .document(giftedVehicle.documentId).delete()
                        .addOnSuccessListener {
                            // Document deleted successfully, you can take further actions if needed
                            Toast.makeText(context, "Gifted", Toast.LENGTH_SHORT).show()

                            // Delete associated images from storage
                            val imagesToDelete = ArrayList<String>()
                            for (entry in fillingEntries) {
                                entry.photoUrl?.let { photoUrl ->
                                    imagesToDelete.add(photoUrl)
                                }
                            }
                            val storage = FirebaseStorage.getInstance()
                            val deleteImagesTasks = ArrayList<Task<Void>>()
                            for (imageUri in imagesToDelete) {
                                Log.d("PhotoUri",imageUri)
                                val imageRef = storage.getReferenceFromUrl(imageUri)
                                val deleteTask = imageRef.delete()
                                deleteImagesTasks.add(deleteTask)
                            }

                            // Execute each delete task separately
                            deleteImagesTasks.forEach { deleteTask ->
                                deleteTask
                                    .addOnSuccessListener {
                                        // Image deleted successfully
                                        Log.d("ImageDeleteSuccess", "Image deleted successfully")
                                    }
                                    .addOnFailureListener { exception ->
                                        // Handle image deletion error
                                        Log.e("ImageDeleteError", "Error deleting image: ${exception.message}", exception)
                                    }
                            }

                            // Refresh the listener after all deletion tasks are complete
                            Tasks.whenAll(deleteImagesTasks)
                                .addOnSuccessListener {
                                    Toast.makeText(context, "Associated images deleted", Toast.LENGTH_SHORT).show()
                                    refreshListener.onRefresh()
                                }
                                .addOnFailureListener { exception ->
                                    // Handle error
                                    Log.e("ImageDeleteError", "Error deleting images: ${exception.message}", exception)
                                }
                            refreshListener.onRefresh()
                        }
                        .addOnFailureListener { exception ->
                            // Handle error
                            Log.e("GiftError", exception.toString())
                        }
                    alertDialog.dismiss()
                }


                alertDialog.show()
            }
            .addOnFailureListener { exception ->
                // Handle error
            }
    }




}
