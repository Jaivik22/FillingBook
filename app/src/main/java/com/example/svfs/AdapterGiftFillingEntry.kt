package com.example.svfs

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import java.text.SimpleDateFormat
import java.util.Locale

class AdapterGiftFillingEntry (private val entries: List<FillingEntryPage.FillingEntry>):
    RecyclerView.Adapter<AdapterGiftFillingEntry.ViewHolder>() {


        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view =
                LayoutInflater.from(parent.context).inflate(R.layout.item_gift_filling_entry, parent, false)
            return ViewHolder(view)
        }


    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val entry = entries[position]
            holder.bind(entry)

        }

        override fun getItemCount(): Int {
            return entries.size
        }

        class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val fillingTextView: TextView = itemView.findViewById(R.id.gfillingTextView)
            val dateTextView: TextView = itemView.findViewById(R.id.gdateTextView)
            val extraTextView:TextView = itemView.findViewById(R.id.gextraTextView)
            private val context: Context = itemView.context

            fun bind(entry: FillingEntryPage.FillingEntry) {
                val dateFormatter = SimpleDateFormat("dd/MM/yy", Locale.getDefault())
                val formattedDate = dateFormatter.format(entry.date)

                fillingTextView.text = "Filling: ${entry.filling}"
                dateTextView.text = "Date: $formattedDate"
                if (entry.extra==true){
                    extraTextView.text = "Extra"
                }


                itemView.setOnClickListener {
                    showImageDialog(entry.photoUrl)
                }

            }
            private fun showImageDialog(photoUrl: String?) {
                val builder = AlertDialog.Builder(context)
                val inflater = LayoutInflater.from(context)
                val dialogView = inflater.inflate(R.layout.dialog_image_preview, null)

                val imageView: ImageView = dialogView.findViewById(R.id.imageView)
                val placeholderImage = R.drawable.ganesha // Replace with your default image resource

                if (photoUrl != null && photoUrl.isNotBlank()) {
                    Glide.with(context).load(photoUrl).into(imageView) // Use Glide to load image from URL
                } else {
                    imageView.setImageResource(placeholderImage)
                }

                builder.setView(dialogView)
                builder.create().show()
            }
        }

}