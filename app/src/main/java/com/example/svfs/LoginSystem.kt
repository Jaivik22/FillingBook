package com.example.svfs

import android.app.ProgressDialog
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.FirebaseException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import java.util.concurrent.TimeUnit

class LoginSystem : AppCompatActivity() {
    private lateinit var phoneNumberEditText: EditText
    private lateinit var otpEditText: EditText
    private lateinit var loginButton: Button
    private lateinit var registerTextView: TextView

    private lateinit var mAuth: FirebaseAuth
    private var verificationId: String = ""
    var storedUserId: String? = null
    var storedFullName:String? = null
    var storedPhoneNo:String? = null
    private var progressDialog: ProgressDialog? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login_system)

        phoneNumberEditText = findViewById(R.id.lsphoneNumber)
        otpEditText = findViewById(R.id.lspOTP)
        loginButton = findViewById(R.id.lspLogin)
        registerTextView = findViewById(R.id.lspRegister)

        mAuth = FirebaseAuth.getInstance()

        progressDialog = ProgressDialog(this)
        progressDialog?.setMessage("Please wait...")
        progressDialog?.setCancelable(false)


        loginButton.setOnClickListener {
            if (otpEditText.visibility == View.VISIBLE) {
                // User clicked on "Login" after entering OTP
                val otpCode = otpEditText.text.toString().trim()
                if (otpCode.isNotEmpty()) {
                    val credential = PhoneAuthProvider.getCredential(verificationId, otpCode)
                    signInWithPhoneAuthCredential(credential)
                }
            } else {
                // User clicked on "Login" without entering OTP
                val phoneNumber = "+91"+phoneNumberEditText.text.toString().trim()
                if (phoneNumber.isNotEmpty()) {
                    checkUserExists(phoneNumber)
                }
            }
        }

        registerTextView.setOnClickListener {
            // Handle registration here
            val i = Intent(this,RegisterPage::class.java)
            startActivity(i)
        }
    }

    private fun sendVerificationCode(phoneNumber: String) {
        progressDialog?.show()
        PhoneAuthProvider.getInstance().verifyPhoneNumber(
            phoneNumber,
            60,
            TimeUnit.SECONDS,
            this,
            object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
                override fun onVerificationCompleted(credential: PhoneAuthCredential) {
                    signInWithPhoneAuthCredential(credential)
                }

                override fun onVerificationFailed(e: FirebaseException) {
                    // Handle verification failure
                    Log.e("VerificationFailed",e.message.toString())
                    progressDialog?.dismiss()
                }

                override fun onCodeSent(
                    verificationId: String,
                    token: PhoneAuthProvider.ForceResendingToken
                ) {
                    this@LoginSystem.verificationId = verificationId
                    otpEditText.visibility = View.VISIBLE
                    loginButton.text = "Verify OTP"
                    progressDialog?.dismiss()
                }
            }
        )
    }

    private fun signInWithPhoneAuthCredential(credential: PhoneAuthCredential) {
        mAuth.signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    // Sign in success
//                    Toast.makeText(this,"LoginSuccessfull",Toast.LENGTH_SHORT).show()
                    storeUserDetails()
                } else {
                    if (task.exception is FirebaseAuthInvalidCredentialsException) {
                        // Invalid verification code
                    }
                }
            }
    }

    fun storeUserDetails() {
        progressDialog?.show()
        val user = mAuth.currentUser
        storedUserId = user!!.uid
        val db = FirebaseFirestore.getInstance()
        val documentReference = db.collection("Profiles").document(storedUserId!!)
        documentReference.get().addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val document = task.result
                if (document.exists()) {
                    // User data found, access it here
                    storedFullName = document.getString("FullName")
                    storedPhoneNo = document.getString("PhoneNo")
                    //                        storedAccountTotal  =document.getString("AccountTotal");
                    Toast.makeText(this@LoginSystem, "Login Successful", Toast.LENGTH_SHORT).show()
                    val sharedPreferences =
                        getSharedPreferences("MyPrefs", MODE_PRIVATE)
                    val editor = sharedPreferences.edit()
                    editor.putString("storedFullName", storedFullName)
                    editor.putString("storedPhoneNo", storedPhoneNo)
                    //                        editor.putString("storedAccountTotal", storedAccountTotal);
                    editor.putString("storedUserId", storedUserId)
                    editor.commit()
                    Toast.makeText(this,"LoginSuccessfull",Toast.LENGTH_SHORT).show()
                    val i = Intent(this@LoginSystem, MainActivity::class.java)
                    startActivity(i)
                    //                        progressDialog.show();
                } else {
                    // User data not found
                    Log.d("Firestore", "User data not found")
                }
            } else {
                // Error occurred while retrieving data
                Log.e("Firestore", "Error: " + task.exception!!.message)
            }
        }
    }

    private fun checkUserExists(phoneNumber: String) {
        progressDialog?.show()
        val db = FirebaseFirestore.getInstance()
        val userCollection = db.collection("Profiles")

        userCollection
            .whereEqualTo("PhoneNo", phoneNumber)
            .get()
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val documents = task.result?.documents
                    if (!documents.isNullOrEmpty()) {
                        // User with the provided phone number exists in the database
                        // Send OTP and proceed with the login process
                        sendVerificationCode(phoneNumber)
                    } else {
                        // User does not exist in the database
                        // Display an appropriate message to the user
                        Toast.makeText(this, "User does not exist", Toast.LENGTH_SHORT).show()
                        val i = Intent(this,RegisterPage::class.java)
                        startActivity(i)
                    }
                } else {
                    // Error occurred while checking user existence
                    Log.e("Firestore", "Error: " + task.exception?.message)
                }
            }
    }
}