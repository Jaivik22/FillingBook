package com.example.svfs

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.GridView

class HomeFragment : Fragment() {
    private lateinit var lv:GridView

    private val img = intArrayOf(R.drawable.svfs_add,R.drawable.svfs_vehicles,R.drawable.svfs_gift,R.drawable.sreport2)
    private val name = arrayOf("Add Vehicle","Vehicles", "Gift","Gallery")


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val v =  inflater.inflate(R.layout.fragment_home, container, false)

        lv = v.findViewById(R.id.listView)

        val adapter = AdapterHome(requireActivity(),name,img)
        lv.adapter = adapter

        return v;
    }


}