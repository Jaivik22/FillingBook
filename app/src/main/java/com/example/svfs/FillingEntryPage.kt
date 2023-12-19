package com.example.svfs

import AdapterFillingEntry
import android.app.AlertDialog
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Bundle
import android.telephony.SmsManager
import android.text.Editable
import android.text.Html
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import java.util.Calendar
import java.util.Date
import android.Manifest
import android.app.ProgressDialog
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import android.provider.MediaStore
import android.widget.ImageView
import androidx.core.content.FileProvider
import com.google.android.gms.tasks.Task
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.withContext
import java.io.File
import kotlinx.coroutines.launch



class FillingEntryPage : AppCompatActivity(),AdapterFillingEntry.TotalFillingUpdateListener {

    private lateinit var vehicleNumber: String
    private lateinit var vehiclePhoneNumber: String
    private val db = FirebaseFirestore.getInstance()
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: AdapterFillingEntry
    private lateinit var vehicle:VehiclesList.Vehicle
    private lateinit var textViewTotalFilling:TextView
    private lateinit var sharedPreferences: SharedPreferences
    private var userID:String = ""
    private var phoneNo:String = ""

    private val REQUEST_IMAGE_CAPTURE = 1
    private val EXTRA_PHOTO_URI = "extra_photo_uri"

// Inside your FillingEntryPage class

    private  var photoUri: Uri? = null

    private lateinit var ivPhotoPreview:ImageView
    private lateinit var btnRetakePhoto:Button
    private lateinit var btnClickPhoto:Button
    private lateinit var progressDialog: ProgressDialog
    val entries = ArrayList<FillingEntryPage.FillingEntry>()


    private val storageRef = FirebaseStorage.getInstance().reference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        FirebaseApp.initializeApp(this);
        setContentView(R.layout.activity_filling_entry_page)

        val textViewVehicleNumber: TextView = findViewById(R.id.textViewVehicleNumber)
        val textViewDriverName: TextView = findViewById(R.id.textViewDriverName)
        val textViewPhoneNumber: TextView = findViewById(R.id.textViewPhoneNumber)
        textViewTotalFilling = findViewById(R.id.textViewTotalFilling)
        val entryFab: MaterialButton = findViewById(R.id.entryfab)
        val deleteButton:ImageView = findViewById(R.id.deleteIcon)

        sharedPreferences = getSharedPreferences("MyPrefs", MODE_PRIVATE)
        userID = sharedPreferences.getString("storedUserId", "userID").toString()
        phoneNo = sharedPreferences.getString("storedPhoneNo", "Phone no.").toString()


        progressDialog = ProgressDialog(this)
        progressDialog!!.setMessage("Please wait...")
        progressDialog!!.setCancelable(false)


        vehicle = intent.getParcelableExtra<VehiclesList.Vehicle>("vehicle")!!

        recyclerView = findViewById(R.id.flr)
        recyclerView.layoutManager = LinearLayoutManager(this)

        if (vehicle != null) {
            vehicleNumber = vehicle.vehicleNumber
            vehiclePhoneNumber = vehicle.mobileNumber
            textViewVehicleNumber.text = "${vehicle.vehicleNumber.toUpperCase()}"
            textViewDriverName.text = "Driver Name: ${vehicle.driverName}"
            textViewPhoneNumber.text = "Phone Number: ${vehicle.mobileNumber}"
            textViewTotalFilling.text = "Total Filling: ${vehicle.totalFilling}"
        }

        fetchEntries()

        entryFab.setOnClickListener {
            showAddEntryDialog()
        }

        deleteButton.setOnClickListener {
            showDeleteEntryDialog()
        }
    }

    private fun showAddEntryDialog() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Add Filling Entry")

        // Inflate the dialog layout
        val inflater: LayoutInflater = LayoutInflater.from(this)
        val dialogView: View = inflater.inflate(R.layout.dialog_add_filling_entry, null)
        builder.setView(dialogView)

        val etfillingEntry = dialogView.findViewById<EditText>(R.id.etFillingEntry)

        ivPhotoPreview = dialogView.findViewById(R.id.ivPhotoPreview)
        btnClickPhoto = dialogView.findViewById(R.id.btnClickPhoto)
        btnRetakePhoto = dialogView.findViewById(R.id.btnRetakePhoto)

        // ... Other dialog setup ...
        btnClickPhoto.setOnClickListener {
            takePhoto()
        }

        btnRetakePhoto.setOnClickListener {
            takePhoto()
        }

        builder.setPositiveButton("Add") { dialogInterface, which ->
            val filling = etfillingEntry.text.toString().toLowerCase().trim()
            val dfilling = filling.toDouble()
                GlobalScope.launch {
                    addEntry(dfilling, photoUri)
                }
        }
        builder.setNegativeButton("Cancel") { dialogInterface, which ->
            Toast.makeText(this, "Clicked Cancel", Toast.LENGTH_LONG).show()
        }

        val dialog = builder.create()
        dialog.setOnShowListener {

            val addButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
            // Initially, disable the Add button
            addButton.isEnabled = false

            etfillingEntry.addTextChangedListener(object : TextWatcher {
                override fun afterTextChanged(s: Editable?) {
                    addButton.isEnabled = !s.isNullOrBlank()
                }

                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            })

        }
        dialog.show()
    }

    private suspend fun addEntry(filling:Double,photoUri: Uri? = null, extra:Boolean = false) {
        withContext(Dispatchers.Main) {
            progressDialog.show()

            val imageByteArray = photoUri?.let { compressImageToFile(it)?.readBytes() } ?: byteArrayOf()
            val mAuth = FirebaseAuth.getInstance()
            Log.d("Authentication", "Current user: ${mAuth.currentUser}")


            if (mAuth.currentUser != null) {
                val photoFileName = "filling_${System.currentTimeMillis()}.jpg"
                val photoRef = storageRef.child(photoFileName)

                photoRef.putBytes(imageByteArray as ByteArray)
                    .addOnSuccessListener {
                        photoRef.downloadUrl.addOnSuccessListener { photoUrl ->
                            val calendar = Calendar.getInstance()
                            calendar.timeInMillis = System.currentTimeMillis()
                            val entryMap = hashMapOf(
                                "Date" to calendar.time,
                                "Filling" to filling,
                                "PhotoUrl" to photoUrl.toString(),
                                "extra" to extra
                            )
                            if (filling != 0.0) {
                                db.collection("Profiles").document(userID).collection("Vehicles")
                                    .document(vehicleNumber).collection("FillingEntry")
                                    .add(entryMap).addOnSuccessListener {
                                        Toast.makeText(
                                            applicationContext,
                                            "Filling Entry Added Successfully",
                                            Toast.LENGTH_LONG
                                        )
                                            .show()
                                        if (!extra) {
                                            updateTotal(filling)
                                            progressDialog.dismiss()
                                        } else {
                                            updateTotal(filling, "Remaining")
                                            progressDialog.dismiss()
                                        }



                                    }
                                    .addOnFailureListener { exception ->
                                        Toast.makeText(
                                            applicationContext,
                                            "Failed To Add Filling Entry",
                                            Toast.LENGTH_LONG
                                        ).show()
                                        Log.e(
                                            "FillingEntry",
                                            "Failed To Add Filling Entry",
                                            exception
                                        )
                                        progressDialog.dismiss()
                                    }
                            } else {
                                if (!extra) {
                                    updateTotal(filling)
                                    progressDialog.dismiss()
                                } else {
                                    updateTotal(filling, "Remaining")
                                    progressDialog.dismiss()
                                }
                            }
                        }
                    }
                    .addOnFailureListener { exception ->
                        progressDialog.dismiss()
                        Log.e("FillingEntry", "Failed to Add vehicle", exception)
                        // Handle failure
                    }
            } else {
                progressDialog.dismiss()
                Toast.makeText(applicationContext, "not authenticated user", Toast.LENGTH_LONG).show()

            }
        }

    }

    private fun updateTotal(filling: Double, specific:String = "") {
        progressDialog.show()

        //making photouri null so if someone adds new entry in same state without adding photo it will not add old photo in new entry
        photoUri = null
        var newTotal:Double?=null
        if (specific.equals("")) {
            newTotal = vehicle.totalFilling + filling
            sendStyledSMS(filling,newTotal)
        }else{
            newTotal = filling
        }
        val map = hashMapOf(
            "TotalFilling" to newTotal
        )
        db.collection("Profiles").document(userID).collection("Vehicles").document(vehicleNumber).update(map as Map<String, Any>)
            .addOnSuccessListener {
                vehicle.totalFilling = newTotal // Update the vehicle object
                textViewTotalFilling.text = "Total Filling: $newTotal"
                if (specific.equals("")) {
                    checkAndHandleGifting(newTotal)
                }
                progressDialog.dismiss()
                // Refresh the list of entries
                fetchEntries()
            }.addOnFailureListener {exception ->
                Toast.makeText(this, "Failed To updateEntry", Toast.LENGTH_LONG).show()
                progressDialog.dismiss()
                Log.e("FillingEntry", "Failed to Add vehicle", exception)

            }
    }


    private fun fetchEntries() {
        progressDialog.show()
        db.collection("Profiles").document(userID).collection("Vehicles").document(vehicleNumber).collection("FillingEntry")
            .orderBy("Date") // Replace "Date" with the correct field name in your Firestore documents
            .get()
            .addOnSuccessListener { result ->
                entries.clear()

                for (document in result) {
                    val date = document.getDate("Date")
                    val filling = document.getDouble("Filling")
                    val id = document.id // Get the document ID
                    val extra = document.getBoolean("extra")
                    val photoUrl =document.getString("PhotoUrl")
                    if (date != null && filling != null) {
                        entries.add(FillingEntryPage.FillingEntry(filling, date,id,extra,photoUrl))
                    }
                }


                entries.sortByDescending { it.date } // Sort entries by date
                adapter = AdapterFillingEntry(this,entries,vehicleNumber,userID,vehicle.totalFilling)
                adapter.setTotalFillingUpdateListener(this) // Set the listener
                recyclerView.adapter = adapter
                progressDialog.dismiss()
            }
            .addOnFailureListener { exception ->
                // Handle the failure
                progressDialog.dismiss()
                Log.e("FillingEntry", "Failed to Add vehicle", exception)

            }
    }


    // Inside FillingEntryPage class

    // Inside FillingEntryPage class

    private fun checkAndHandleGifting(totalFilling: Double) {
        progressDialog.show()
        if (totalFilling >= 2000) {
            // Add a document to "GiftedVehicles" collection
            val giftedVehicle = hashMapOf(
                "vehicleNumber" to vehicleNumber,
                "date" to FieldValue.serverTimestamp()
            )

            val batch = db.batch()

            val giftedVehicleRef = db.collection("Profiles").document(userID).collection("GiftedVehicles").document()
            batch.set(giftedVehicleRef, giftedVehicle)

            val entriesToMove = ArrayList<FillingEntryPage.FillingEntry>()

            db.collection("Profiles").document(userID).collection("Vehicles").document(vehicleNumber)
                .collection("FillingEntry")
                .orderBy("Date")
                .get()
                .addOnSuccessListener { result ->
                    for (document in result) {
                        val date = document.getDate("Date")
                        val filling = document.getDouble("Filling")
                        val id = document.id // Get the document ID
                        val extra = document.getBoolean("extra")
                        val photoUrl = document.getString("PhotoUrl")
                        if (date != null && filling != null) {
                            entriesToMove.add(FillingEntryPage.FillingEntry(filling, date, id,extra,photoUrl))
                        }
                    }

                    val giftedVehicleEntryRef = giftedVehicleRef.collection("Entries")
                    for (entry in entriesToMove) {
                        val entryDoc = giftedVehicleEntryRef.document(entry.id!!)
                        batch.set(entryDoc, entry)
                    }

                    // Delete entries from "FillingEntry" collection
                    for (entry in entriesToMove) {
                        val entryDoc = db.collection("Profiles").document(userID).collection("Vehicles").document(vehicleNumber)
                            .collection("FillingEntry")
                            .document(entry.id!!)
                        batch.delete(entryDoc)
                    }

                    // Commit the batched write operations
                    batch.commit()
                        .addOnSuccessListener {
                            GlobalScope.launch {
                                addEntry(
                                    totalFilling - 2000,
                                    null,
                                    true
                                ) // Add remaining Filling's entry
                            }
                        }
                        .addOnFailureListener { exception ->
                            Toast.makeText(this, "Failed To Handle Gifting", Toast.LENGTH_LONG).show()
                            Log.e("Gifting", "Failed to handle gifting", exception)
                        }
                    progressDialog.dismiss()
                }.addOnFailureListener{exception->
                    Log.e("FillingEntry", "Failed to Add vehicle", exception)
                }
            progressDialog.dismiss()
        }
        progressDialog.dismiss()
    }

    private fun sendStyledSMS(filling:Double,totalFilling: Double) {
        val smsManager = SmsManager.getDefault()
        val message = "Thank you for filling<br><br>  Today's filling: $filling Ltr <br><br> Total Filling: $totalFilling Ltr"


        // Create a styled message with a heading using HTML formatting
        val styledMessage = "<b>Sidhhi Vinayak Filing Station:</b><br><br>$message"

        // Send the SMS
        smsManager.sendTextMessage(vehiclePhoneNumber, null, Html.fromHtml(styledMessage).toString(), null, null)
    }

    private fun takePhoto() {
        val photoFileName = "filling_${System.currentTimeMillis()}.jpg"
        val photoFile = File(getExternalFilesDir(null), photoFileName)

        photoUri = FileProvider.getUriForFile(this, "com.example.svfs.fileprovider", photoFile)

        val takePhotoIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        takePhotoIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoUri)

        if (takePhotoIntent.resolveActivity(packageManager) != null) {
            startActivityForResult(takePhotoIntent, REQUEST_IMAGE_CAPTURE)
        } else {
            Toast.makeText(this, "Camera app not available", Toast.LENGTH_SHORT).show()
        }
    }

    // Function to convert image URI to byte array
    private fun getImageByteArray(uri: Uri): ByteArray {
        val inputStream = contentResolver.openInputStream(uri)
        return inputStream?.readBytes() ?: byteArrayOf()
    }


// ... Other functions ...

    // Handle the result of the photo capture
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {
            ivPhotoPreview.visibility = View.VISIBLE
            ivPhotoPreview.setImageURI(photoUri)
            btnRetakePhoto.visibility = View.VISIBLE
            btnClickPhoto.visibility = View.GONE
        }
    }

    private suspend fun compressImageToFile(photoUri: Uri): File? = withContext(Dispatchers.IO) {
        val originalBitmap = MediaStore.Images.Media.getBitmap(contentResolver, photoUri)

        val rotation = getRotationFromExif(photoUri)
        val rotatedBitmap = rotateBitmap(originalBitmap, rotation)

        val compressedImageFile = File.createTempFile("compressed", ".jpg", cacheDir)
        val outputStream = compressedImageFile.outputStream()

        rotatedBitmap.compress(Bitmap.CompressFormat.JPEG, 15, outputStream)
        outputStream.close()

        compressedImageFile
    }

    private fun getRotationFromExif(photoUri: Uri): Int {
        val inputStream = contentResolver.openInputStream(photoUri)
        val exif = inputStream?.use { ExifInterface(it) }
        return when (exif?.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
            ExifInterface.ORIENTATION_ROTATE_90 -> 90
            ExifInterface.ORIENTATION_ROTATE_180 -> 180
            ExifInterface.ORIENTATION_ROTATE_270 -> 270
            else -> 0
        }
    }

    private fun rotateBitmap(bitmap: Bitmap, degrees: Int): Bitmap {
        val matrix = Matrix()
        matrix.postRotate(degrees.toFloat())
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }
    override fun onTotalFillingUpdated(newTotalFilling: Double) {
        // Update the UI elements showing total filling value
        textViewTotalFilling.text = "Total Filling: $newTotalFilling"
        vehicle.totalFilling = newTotalFilling
        // Update any other UI elements or perform actions based on the updated total
    }

    private fun showDeleteEntryDialog() {
        val builder = androidx.appcompat.app.AlertDialog.Builder(this)
        builder.setTitle("Delete Entry")
        builder.setMessage("Are you sure you want to delete this entry?")

        builder.setPositiveButton("Delete") { _, _ ->
            deleteVehicle()
        }
        builder.setNegativeButton("Cancel") { dialogInterface, _ ->
            dialogInterface.dismiss()
        }

        builder.create().show()
    }

    fun deleteVehicle(){
        db.collection("Profiles").document(userID).collection("Vehicles").document(vehicleNumber)
            .delete()
            .addOnSuccessListener {
                // Delete associated images from storage
                val imagesToDelete = ArrayList<String>()
                for (entry in entries) {
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
                            Toast.makeText(this, "deletion completed", Toast.LENGTH_SHORT).show()
                            val i = Intent(this,MainActivity::class.java)
                            startActivity(i)

                        }
                        .addOnFailureListener { exception ->
                            // Handle image deletion error
                            Log.e("ImageDeleteError", "Error deleting image: ${exception.message}", exception)
                            Toast.makeText(this, "deletion failed", Toast.LENGTH_SHORT).show()

                        }
                }
            }
            .addOnFailureListener{
                Toast.makeText(this, "deletion failed", Toast.LENGTH_SHORT).show()
            }
    }



    data class FillingEntry(
        var filling: Double, val date: Date?, val id: String?,
        val extra: Boolean?, val photoUrl:String? = null
    )

}

