package com.example.svfs

import android.content.SharedPreferences
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Parcel
import android.os.Parcelable
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.widget.SearchView
import androidx.core.widget.addTextChangedListener
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.FirebaseApp
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase


@Suppress("DEPRECATION")
class VehiclesList : AppCompatActivity() {

    private val db = Firebase.firestore
    private lateinit var adapter: AdapterVehiclesList
    private var allVehicles: List<Vehicle> = ArrayList() // Store all vehicles from Firebase

    private lateinit var sharedPreferences: SharedPreferences
    private var userID:String = ""



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_vehicles_list)

        val rv:RecyclerView = findViewById(R.id.rvVehicleList)
        val layoutManager = LinearLayoutManager(this)

        sharedPreferences = getSharedPreferences("MyPrefs", MODE_PRIVATE)
        userID = sharedPreferences.getString("storedUserId", "userID").toString()


        adapter = AdapterVehiclesList(ArrayList()) // Pass an empty list for now

        rv.layoutManager = layoutManager
        rv.adapter = adapter

        val searchView: EditText = findViewById(R.id.searchVehicle) // Replace with your SearchView ID
        searchView.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                filterData(s.toString().trim())
            }

            override fun afterTextChanged(s: Editable?) {}
        })


        fetchVehicleData(adapter)

    }

    private fun fetchVehicleData(adapter: AdapterVehiclesList) {
        db.collection("Profiles").document(userID).collection("Vehicles")
            .get()
            .addOnSuccessListener { result ->
                val vehicleList = ArrayList<Vehicle>()
                for (document in result) {
                    val vehicleNumber = document.id
                    val driverName = document.getString("DriverName") ?: ""
                    val mobileNumber = document.getString("MobileNumber") ?: ""
                    val totalFilling = document.getDouble("TotalFilling") ?: 0.0
                    val vehicle =
                        Vehicle(vehicleNumber, driverName, mobileNumber, totalFilling)
                    vehicleList.add(vehicle)
                }
                allVehicles = vehicleList // Store all vehicles
                adapter.setData(vehicleList)
            }
            .addOnFailureListener { exception ->
                Log.e("FetchData", "Error getting documents: ", exception)
            }
    }
    private fun filterData(query: String) {
        val filteredList = allVehicles.filter { vehicle ->
            vehicle.vehicleNumber.contains(query, ignoreCase = true)
        }
        adapter.setData(filteredList)
    }
    // data class
    data class Vehicle(val vehicleNumber: String, val driverName: String, val mobileNumber: String, var totalFilling:Double):Parcelable{
        constructor(parcel: Parcel) : this(
            parcel.readString()!!,
            parcel.readString()!!,
            parcel.readString()!!,
            parcel.readDouble()!!
        )

        override fun writeToParcel(parcel: Parcel, flags: Int) {
            parcel.writeString(vehicleNumber)
            parcel.writeString(driverName)
            parcel.writeString(mobileNumber)
            parcel.writeDouble(totalFilling)
        }

        override fun describeContents(): Int {
            return 0
        }

        companion object CREATOR : Parcelable.Creator<Vehicle> {
            override fun createFromParcel(parcel: Parcel): Vehicle {
                return Vehicle(parcel)
            }

            override fun newArray(size: Int): Array<Vehicle?> {
                return arrayOfNulls(size)
            }
        }
    }

    override fun onResume() {
        super.onResume()

        fetchVehicleData(adapter)
    }
}
