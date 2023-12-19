package com.example.svfs

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.MenuItem
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.FragmentTransaction
import com.google.android.material.navigation.NavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuth.AuthStateListener
import com.google.firebase.firestore.FirebaseFirestore

class MainActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {
    private lateinit var drawerlayout:DrawerLayout
    private lateinit var navigationView:NavigationView
    private lateinit var toolbar: Toolbar
    private lateinit var ft :LinearLayout
    var drawerUsername:TextView? = null
    var drawerEmail:TextView? = null

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private var mAuthListener: AuthStateListener? = null
    private lateinit var sharedPreferences: SharedPreferences

    private var FullName:String = ""
    private var phoneNo:String = ""
    private var userID:String = ""

    private val SEND_SMS_PERMISSION_REQUEST_CODE = 123 // You can use any unique value




    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        ft = findViewById(R.id.ft)
        drawerlayout = findViewById(R.id.mainDrawer)
        navigationView = findViewById(R.id.navigationView)
        toolbar = findViewById(R.id.Toolbar1)
        setSupportActionBar(toolbar)
        mAuthListener = AuthStateListener { firebaseAuth ->
            val user = firebaseAuth.currentUser
            if (user != null) {
            } else {
                // User is signed out
                // You can navigate to the login activity
                val i = Intent(this@MainActivity, LoginSystem::class.java)
                startActivity(i)
                finish()
                //                    progressDialog.show();
            }
        }
        auth.addAuthStateListener(mAuthListener!!)

        sharedPreferences = getSharedPreferences("MyPrefs", MODE_PRIVATE)
        FullName = sharedPreferences.getString("storedFullName", "FullName").toString()
        phoneNo = sharedPreferences.getString("storedPhoneNo", "Phone no.").toString()
        userID = sharedPreferences.getString("storedUserId", "userID").toString()

//         defining headerview of drawer layout
        val headerView = navigationView.getHeaderView(0)
        val drawerFullName = headerView.findViewById<TextView>(R.id.tvFullName)
        drawerFullName.text = FullName

        checkSendSmsPermission()


        navigationView.setNavigationItemSelectedListener(this)
        val toggle = ActionBarDrawerToggle(
            this,
            drawerlayout,
            toolbar ,
            R.string.navigation_drawer_open,
            R.string.navigation_drawer_close
        )

        drawerlayout.addDrawerListener(toggle)
        toggle.syncState()

        val homeFragment = HomeFragment();
        val ft:FragmentTransaction = supportFragmentManager.beginTransaction()
        ft.replace(R.id.ft,homeFragment)
        ft.commit()
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        val id:Int =item.itemId

        if (id==R.id.logout) {
            val sharedPreferences = getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)
            val editor = sharedPreferences.edit()
            editor.clear()
            editor.commit()
            auth.signOut()
            val intent = Intent(this,LoginSystem::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(intent)
            finish()
        }
        return false
    }

    private fun checkSendSmsPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS)
            != PackageManager.PERMISSION_GRANTED) {
            // Permission is not granted, request it
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.SEND_SMS),
                SEND_SMS_PERMISSION_REQUEST_CODE
            )
        } else {
            // Permission is already granted, proceed with sending SMS
            // Your SMS sending code here
        }
    }
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == SEND_SMS_PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission was granted, proceed with sending SMS
                // Your SMS sending code here
            } else {
                // Permission was denied, handle it (e.g., show a message to the user)
            }
        }
    }
}
