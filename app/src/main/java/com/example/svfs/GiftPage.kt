package com.example.svfs

import android.content.SharedPreferences
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.firestore.FirebaseFirestore
import java.util.Date


class GiftPage : AppCompatActivity(),OnRefreshListener {
    private val db = FirebaseFirestore.getInstance()
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: AdapteGiftPage
    private lateinit var sharedPreferences: SharedPreferences
    private var userID:String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_gift_page)

        recyclerView = findViewById(R.id.grv)
        recyclerView.layoutManager = LinearLayoutManager(this)
        adapter = AdapteGiftPage(this,this)
        recyclerView.adapter = adapter

        sharedPreferences = getSharedPreferences("MyPrefs", MODE_PRIVATE)
        userID = sharedPreferences.getString("storedUserId", "userID").toString()

        // Fetch data from "GiftedVehicles" collection
        db.collection("Profiles").document(userID).collection("GiftedVehicles")
            .get()
            .addOnSuccessListener { result ->
                val giftedVehicleList = ArrayList<GiftedVehicle>()
                for (document in result) {
                    val vehicleNumber = document.getString("vehicleNumber")
                    val date = document.getDate("date")
                    val documentId = document.id
                    if (vehicleNumber != null && date != null) {
                        giftedVehicleList.add(GiftedVehicle(vehicleNumber, date,documentId))
                    }
                }
                adapter.setGiftedVehicles(giftedVehicleList)
            }
            .addOnFailureListener { exception ->
                Log.e("GiftedVehicles", "Error getting documents: ", exception)
            }
    }

    data class GiftedVehicle(val vehicleNumber: String, val date: Date,val documentId:String)

    override fun onRefresh() {
        db.collection("Profiles").document(userID).collection("GiftedVehicles")
            .get()
            .addOnSuccessListener { result ->
                val giftedVehicleList = ArrayList<GiftedVehicle>()
                for (document in result) {
                    val vehicleNumber = document.getString("vehicleNumber")
                    val date = document.getDate("date")
                    val documentId = document.id
                    if (vehicleNumber != null && date != null) {
                        giftedVehicleList.add(GiftedVehicle(vehicleNumber, date,documentId))
                    }
                }
                adapter.setGiftedVehicles(giftedVehicleList)
            }
            .addOnFailureListener { exception ->
                Log.e("GiftedVehicles", "Error getting documents: ", exception)
            }
    }

}