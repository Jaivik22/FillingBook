package com.example.svfs

import android.content.SharedPreferences
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.ktx.Firebase
import com.google.type.Date
import java.util.Calendar



class report : AppCompatActivity() {
    val db = FirebaseFirestore.getInstance()
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: AdapterReport
    private lateinit var sharedPreferences: SharedPreferences
    private var userID:String = ""
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_report)
        recyclerView = findViewById(R.id.rrv)
        recyclerView.layoutManager = LinearLayoutManager(this)

        sharedPreferences = getSharedPreferences("MyPrefs", MODE_PRIVATE)
        userID = sharedPreferences.getString("storedUserId", "userID").toString()

        fetchAllVehicleRecords()
    }

    // Fetch all records of all vehicles
    private fun fetchAllVehicleRecords() {
        db.collection("Profiles").document(userID).collection("Vehicles")
            .get()
            .addOnSuccessListener { vehicleQuerySnapshot ->
                val allRecords = ArrayList<VehicleRecord>()

                for (vehicleDocument in vehicleQuerySnapshot) {
                    val vehicleNumber = vehicleDocument.id

                    vehicleDocument.reference.collection("FillingEntry")
                        .get()
                        .addOnSuccessListener { fillingQuerySnapshot ->
                            for (fillingDocument in fillingQuerySnapshot) {
                                val dateTimestamp = fillingDocument.getTimestamp("Date")
                                val date = dateTimestamp?.toDate() // Convert Timestamp to Date
                                val fillingAmount = fillingDocument.getDouble("Filling")

                                // Format the date and filling information as needed
//                                val recordInfo = "vehicleNumber: $vehicleNumber, date: $date, fillingAmount: $fillingAmount"
//                                allRecords.add(recordInfo)
                                if (dateTimestamp != null && fillingAmount != null) {
                                    allRecords.add(VehicleRecord(vehicleNumber, date,fillingAmount))
                                }
                            }
                            // Sort the data by date
                            allRecords.sortByDescending { it.date }
                            Log.d("AdapterReport", "Data size: ${allRecords.size}")
                            adapter = AdapterReport(allRecords)
                            recyclerView.adapter = adapter
                            adapter.notifyDataSetChanged()

                            // Update your UI or perform further actions with allRecords list
                            // For example, you can set up a RecyclerView to display the records.
                            // Here, I'm printing the records to the log for simplicity.
                            allRecords.forEach { record ->
                                Log.d("AllRecords", record.toString())
                            }
                        }
                        .addOnFailureListener { fillingException ->
                            Log.e("FetchingFilling", "Failed to fetch filling records", fillingException)
                        }

                }


            }
            .addOnFailureListener { vehicleException ->
                Log.e("FetchingVehicles", "Failed to fetch vehicle documents", vehicleException)
            }

    }

    data class VehicleRecord(
        val vehicleNumber: String,
        val date: java.util.Date?,
        val fillingAmount: Double
    )



}