package com.example.svfs

import android.app.ProgressDialog
import android.content.Context
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
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthProvider
import com.google.firebase.auth.PhoneAuthProvider.ForceResendingToken
import com.google.firebase.auth.PhoneAuthProvider.OnVerificationStateChangedCallbacks
import com.google.firebase.firestore.FirebaseFirestore
import java.util.concurrent.TimeUnit

class RegisterPage : AppCompatActivity() {

    private lateinit var enterPhonenumber:EditText
    private lateinit var enterFullname:EditText
    private lateinit var enterOTP:EditText
    private lateinit var loginTXT:TextView
    private lateinit var registerBtn:Button
    private lateinit var progressDialog :ProgressDialog
    private var storedVerificationId:String = ""
    private var phoneNumber:String = ""
    private var fullName:String = ""

    private val mAuth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register_page)
        enterPhonenumber = findViewById(R.id.rpPhoneNumber)
        enterFullname = findViewById(R.id.rpFullname)
        enterOTP = findViewById(R.id.rpOTP)
        loginTXT = findViewById(R.id.rpLogin)

        progressDialog = ProgressDialog(this)
        registerBtn = findViewById(R.id.rpRegister)
        var verificationCode = ""

        progressDialog.setMessage("Please wait...")
        progressDialog.setCancelable(false)

        loginTXT.setOnClickListener(View.OnClickListener {
            val i = Intent(this@RegisterPage, LoginSystem::class.java)
            startActivity(i)
        })


        registerBtn.setOnClickListener(View.OnClickListener {
            verificationCode = enterOTP.getText().toString()
            phoneNumber = "+91" + enterPhonenumber.getText().toString()
            fullName = enterFullname.getText().toString()
            if (verificationCode == "") {
                signInWithOutOTP(phoneNumber)
                progressDialog.show()
            } else {
                verificationCode = enterOTP.getText().toString()
                signInWithOTP(verificationCode, storedVerificationId)
                progressDialog.show()
            }
        })
    }

    private fun signInWithOutOTP(phoneNumber: String) {
        PhoneAuthProvider.getInstance().verifyPhoneNumber(
            phoneNumber,
            60,
            TimeUnit.SECONDS,
            this,
            object : OnVerificationStateChangedCallbacks() {
                override fun onVerificationCompleted(credential: PhoneAuthCredential) {
                    // This callback will be invoked in case of instant verification.
                    signInWithPhoneAuthCredential(credential)
                }

                override fun onVerificationFailed(e: FirebaseException) {
                    // Handle verification failure
                    Toast.makeText(this@RegisterPage, e.message.toString(), Toast.LENGTH_SHORT)
                        .show()
                    Log.e("VerificationFailed", e.message.toString())
                }

                override fun onCodeSent(verificationId: String, token: ForceResendingToken) {
                    // Save the verification ID and token to use in the signInWithPhoneAuthCredential method
                    // You can store it in shared preferences or any other way that suits your app's architecture
                    Toast.makeText(this@RegisterPage, "Otpsent", Toast.LENGTH_SHORT).show()
                    storedVerificationId = verificationId
                    val storedToken = token
                    progressDialog.dismiss()
                    enterOTP.setVisibility(View.VISIBLE)
                }
            }
        )
    }

    private fun signInWithOTP(verificationCode: String, storedVerificationId: String) {
        val credential = PhoneAuthProvider.getCredential(storedVerificationId, verificationCode)
        signInWithPhoneAuthCredential(credential)
    }

    private fun signInWithPhoneAuthCredential(credential: PhoneAuthCredential) {
        mAuth.signInWithCredential(credential)
            .addOnCompleteListener(this
            ) { task ->
                if (task.isSuccessful) {
                    // Sign-in successful
                    val user: FirebaseUser? = mAuth.currentUser
                    var userID = user?.uid
                    val map = hashMapOf(
                        "FullName" to fullName,
                        "PhoneNo" to phoneNumber
                    )
                    if (userID != null) {
                        db.collection("Profiles").document(userID).set(map)
                            .addOnSuccessListener {
                                val sharedPreferences = getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)
                                val editor = sharedPreferences.edit()
                                editor.putString("storedFullName", fullName)
                                editor.putString("storedPhoneNo", phoneNumber)
                                editor.putString("storedUserId", userID)
                                editor.commit()
                                Toast.makeText(
                                    this@RegisterPage,
                                    "Login Successful",
                                    Toast.LENGTH_SHORT
                                ).show()
                                val intent = Intent(this,MainActivity::class.java)
                                startActivity(intent)
                            }
                            .addOnFailureListener{
                                Toast.makeText(
                                    this@RegisterPage,
                                    it.message.toString(),
                                    Toast.LENGTH_SHORT
                                ).show()
                                Log.e("TAG", it.message.toString())
                            }
                    }


                    // Proceed with your app's logic for signed-in users
                } else {
                    // Sign-in failed
                    if (task.exception is FirebaseAuthInvalidCredentialsException) {
                        // Handle invalid verification code
                        Toast.makeText(this@RegisterPage, "Wrong OTP", Toast.LENGTH_SHORT)
                            .show()
                        progressDialog.dismiss()
                    } else {
                        // Handle other errors
                        Toast.makeText(
                            this@RegisterPage,
                            task.exception!!.message.toString(),
                            Toast.LENGTH_SHORT
                        ).show()
                        Log.e("TAG", task.exception!!.message.toString())
                        progressDialog.dismiss()
                    }
                }
            }
    }
}