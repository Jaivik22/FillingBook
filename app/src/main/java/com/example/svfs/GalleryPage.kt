package com.example.svfs

import android.app.AlertDialog
import android.app.ProgressDialog
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.core.content.FileProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.Calendar
import kotlinx.coroutines.withContext


class GalleryPage : AppCompatActivity() {

    private lateinit var ivPhotoPreview: ImageView
    private lateinit var btnRetakePhoto: Button
    private lateinit var btnClickPhoto: Button
    private lateinit var progressDialog: ProgressDialog

    private val REQUEST_IMAGE_CAPTURE = 1
    private  var photoUri: Uri? = null

    private val storageRef = FirebaseStorage.getInstance().reference
    private val db = FirebaseFirestore.getInstance()
    private lateinit var sharedPreferences: SharedPreferences
    private var userID:String = ""
    private var phoneNo:String = ""

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: PhotoAdapter


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_gallery_page)
        val entryFab: MaterialButton = findViewById(R.id.galleryFab)

        progressDialog = ProgressDialog(this)
        progressDialog!!.setMessage("Please wait...")
        progressDialog!!.setCancelable(false)

        recyclerView = findViewById(R.id.gplr)
        recyclerView.layoutManager = LinearLayoutManager(this)
        adapter = PhotoAdapter()
        recyclerView.adapter = adapter

        sharedPreferences = getSharedPreferences("MyPrefs", MODE_PRIVATE)
        userID = sharedPreferences.getString("storedUserId", "userID").toString()
        phoneNo = sharedPreferences.getString("storedPhoneNo", "Phone no.").toString()
        Log.d("GalleryPage", "UserID: $userID")


        // Load and display the photos from Firebase
        loadPhotosFromFirebase()

        entryFab.setOnClickListener {
            showAddEntryDialog()
        }
    }

    private fun showAddEntryDialog() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Add Filling Entry")

        // Inflate the dialog layout
        val inflater: LayoutInflater = LayoutInflater.from(this)
        val dialogView: View = inflater.inflate(R.layout.dialog_add_photo, null)
        builder.setView(dialogView)




        ivPhotoPreview = dialogView.findViewById(R.id.givPhotoPreview)
        btnClickPhoto = dialogView.findViewById(R.id.gbtnClickPhoto)
        btnRetakePhoto = dialogView.findViewById(R.id.gbtnRetakePhoto)

        // ... Other dialog setup ...
        btnClickPhoto.setOnClickListener {
            takePhoto()
        }

        btnRetakePhoto.setOnClickListener {
            takePhoto()
        }

        builder.setPositiveButton("Add") { dialogInterface, which ->
            GlobalScope.launch {
                addPhoto(photoUri)
            }
        }
        builder.setNegativeButton("Cancel") { dialogInterface, which ->
            Toast.makeText(this, "Clicked Cancel", Toast.LENGTH_LONG).show()
        }

        val dialog = builder.create()
        dialog.setOnShowListener {

            val addButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
            // Initially, disable the Add button
//            addButton.isEnabled = false
        }
        dialog.show()
    }

    private suspend fun addPhoto(photoUri: Uri? = null) {
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
                                "PhotoUrl" to photoUrl.toString(),
                            )
                            db.collection("Profiles").document(userID).collection("GiftPhotos")
                                    .add(entryMap).addOnSuccessListener {
                                        Toast.makeText(
                                            applicationContext,
                                            "Photo Added Successfully",
                                            Toast.LENGTH_LONG
                                        )
                                            .show()
                                    progressDialog.dismiss()
                                    loadPhotosFromFirebase()

                                    }
                                    .addOnFailureListener { exception ->
                                        Toast.makeText(
                                            applicationContext,
                                            "Failed To Add photo",
                                            Toast.LENGTH_LONG
                                        ).show()
                                        Log.e(
                                            "FillingEntry",
                                            "Failed To Add photo",
                                            exception
                                        )
                                        progressDialog.dismiss()
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

    private fun loadPhotosFromFirebase() {
        val userDocRef = db.collection("Profiles").document(userID)
        val giftPhotosCollectionRef = userDocRef.collection("GiftPhotos")

        giftPhotosCollectionRef
            .orderBy("Date", Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { result ->
                val photos = mutableListOf<Photo>()
                for (document in result) {
                    val photoUrl = document.getString("PhotoUrl")
                    Log.e("photourl1", photoUrl.toString())
                    val photoID = document.id

                            photos.add(Photo(photoID, photoUrl.toString()))
                            Log.e("photourl", photoUrl.toString())

                }
                adapter.setPhotos(photos,userID)
            }
            .addOnFailureListener { exception ->
                Toast.makeText(this, "Failed to fetch photos", Toast.LENGTH_LONG).show()
                Log.e("GalleryPage", "Failed to fetch photos", exception)
            }
    }


    data class Photo(val dID:String,val photoUrl: String)
}


