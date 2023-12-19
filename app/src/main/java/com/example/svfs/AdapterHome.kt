package com.example.svfs

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.ktx.initialize

class AdapterHome : BaseAdapter {
    private var context: Context
    private var names: Array<String>
    private var images: IntArray
    val db = Firebase.firestore
    private lateinit var sharedPreferences: SharedPreferences
    private var userID:String = ""


    constructor(context: Context,names:Array<String>, images:IntArray): super(){
        this.context = context
        this.names = names
        this.images = images
    }




    override fun getCount() :Int{
        return names.size
    }

    override fun getItem(position: Int): Any {
        return names[position]
    }

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    override fun getView(position: Int, p1: View?, p2: ViewGroup?): View {
        val view:View
        val viewHolder:ViewHolder

        if (p1==null){
            view  = LayoutInflater.from(context).inflate(R.layout.custome_home,p2,false)
            viewHolder = ViewHolder()
            viewHolder.nameTextView = view.findViewById(R.id.txt)
            viewHolder.imageView = view.findViewById(R.id.img)
            view.tag = viewHolder
        }
        else {
            view = p1
            viewHolder = view.tag as ViewHolder
        }

        viewHolder.nameTextView?.text = names[position]
        viewHolder.imageView?.setImageResource(images[position])

        view.setOnClickListener {
            if (position==0) {
                addVehicle()
            }
            else if (position==1) {
                val i = Intent(context, VehiclesList::class.java)
                context.startActivity(i)
            }
            else if(position==2){
                val i = Intent(context,GiftPage::class.java)
                context.startActivity(i)
            }
            else if (position==3) {
                val i = Intent(context, GalleryPage::class.java)
                context.startActivity(i)
            }


        }

        return view
    }

    private class ViewHolder {
        var nameTextView: TextView? = null
        var imageView: ImageView? = null
    }

    private fun addVehicle() {

        sharedPreferences = context.getSharedPreferences("MyPrefs", AppCompatActivity.MODE_PRIVATE)
        userID = sharedPreferences.getString("storedUserId", "userID").toString()

        val builder = AlertDialog.Builder(context)
        builder.setTitle("Add New Vehicle")

        // Inflate the dialog layout
        val inflater: LayoutInflater = LayoutInflater.from(context)
        val dialogView: View = inflater.inflate(R.layout.dialog_add_vehicle, null)
        builder.setView(dialogView)

        val edVehicleNo = dialogView.findViewById<EditText>(R.id.hEtVehicleNumber)
        val edDriverName = dialogView.findViewById<EditText>(R.id.hEtDriverName)
        val edMobileNo = dialogView.findViewById<EditText>(R.id.hEtMobileNumber)

        builder.setPositiveButton("Add") { dialogInterface, which ->
            val vehicleNo = edVehicleNo.text.toString().toLowerCase().trim()
            val driverName = edDriverName.text.toString().trim().toLowerCase()
            val mobileNo = edMobileNo.text.toString().trim().toLowerCase()
            val totalFilling = 0.0

            Log.d("AddVehicle", "Vehicle Number: $vehicleNo")
            Log.d("AddVehicle", "Driver Name: $driverName")
            Log.d("AddVehicle", "Mobile Number: $mobileNo")
//            Toast.makeText(context, "$vehicleNo", Toast.LENGTH_LONG).show()

            if (!vehicleNo.equals("")) {
                        val map = hashMapOf(
                            "DriverName" to driverName,
                            "MobileNumber" to mobileNo,
                            "TotalFilling" to totalFilling
                        )
                        db.collection("Profiles").document(userID).collection("Vehicles")
                            .document(vehicleNo).set(map)
                            .addOnSuccessListener {
                                Toast.makeText(
                                    context,
                                    "Vehicle Added successfully",
                                    Toast.LENGTH_LONG
                                )
                                    .show()
                            }.addOnFailureListener { Exception ->
                                Toast.makeText(
                                    context,
                                    "Failed to Add vehicle\n Try Again...",
                                    Toast.LENGTH_LONG
                                ).show()
                                Log.e("AddVehicle", "Failed to Add vehicle1", Exception)
                            }

            } else {
                Toast.makeText(context, "Vehicle Number cannot be empty", Toast.LENGTH_SHORT).show()
            }
        }
        builder.setNegativeButton("Cancel") { dialogInterface, which ->
            Toast.makeText(context, "clicked no", Toast.LENGTH_LONG).show()
        }

        val dialog = builder.create()
        dialog.setOnShowListener {

            val addButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
            // Initially, disable the Add button
            addButton.isEnabled = false
            var noExists = false;

            edVehicleNo.addTextChangedListener(object : TextWatcher {
                override fun afterTextChanged(s: Editable?) {
                    addButton.isEnabled = !s.isNullOrBlank() && !edDriverName.text.isNullOrBlank() && !edMobileNo.text.isNullOrBlank()

                    // Remove previous formatting before applying new formatting
                    val unformattedText = s.toString().replace("-", "").toUpperCase()

                    val formattedVehicleNo = formatVehicleNumber(unformattedText)
                    if (s.toString() != formattedVehicleNo) {
                        edVehicleNo.removeTextChangedListener(this)
                        edVehicleNo.setText(formattedVehicleNo)
                        edVehicleNo.setSelection(formattedVehicleNo.length)
                        edVehicleNo.addTextChangedListener(this)
                    }

//                    // Check if the vehicle number already exists in the database

                    vehicleNoExists(formattedVehicleNo.toLowerCase()) { exists ->
                        if (exists) {
                            noExists = true
                            addButton.isEnabled = false
                        } else {
                            noExists = false
                        }
                    }

                }

                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            })

            edDriverName.addTextChangedListener(object : TextWatcher {
                override fun afterTextChanged(s: Editable?) {
                    addButton.isEnabled = !s.isNullOrBlank() && !edVehicleNo.text.isNullOrBlank() && !edMobileNo.text.isNullOrBlank() && !noExists
                }

                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            })

            edMobileNo.addTextChangedListener(object : TextWatcher {
                override fun afterTextChanged(s: Editable?) {
                    addButton.isEnabled = !s.isNullOrBlank() && !edVehicleNo.text.isNullOrBlank() && !edDriverName.text.isNullOrBlank() && !noExists
                }

                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            })
        }
        dialog.show()
    }

    private fun formatVehicleNumber(input: String): String {
        val strippedInput = input.replace("-", "").toUpperCase()
        val builder = StringBuilder()
        for (i in strippedInput.indices) {
            builder.append(strippedInput[i])
            if ((i == 1 || i == 3 || i == 5) && i != strippedInput.lastIndex) {
                builder.append("-")
            }
        }
        return builder.toString()
    }

    private fun vehicleNoExists(vehicleNo: String, callback: (Boolean) -> Unit) {
        val collectionReference = db.collection("Profiles").document(userID).collection("Vehicles")
        val documentReference = collectionReference.document(vehicleNo)

        documentReference.get()
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val document = task.result
                    if (document != null && document.exists()) {
                        callback(true) // Vehicle number exists
                    } else {
                        callback(false) // Vehicle number does not exist
                    }
                } else {
                    // Handle any errors here
                    Log.e("AddVehicle", "Error checking if vehicle number exists", task.exception)
                    callback(false) // Assuming the vehicle doesn't exist in case of error
                }
            }
    }




}