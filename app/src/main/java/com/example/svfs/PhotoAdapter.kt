package com.example.svfs

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage


class PhotoAdapter : RecyclerView.Adapter<PhotoAdapter.PhotoViewHolder>() {

    private var photos = mutableListOf<GalleryPage.Photo>()
    private var userID = ""


    fun setPhotos(newPhotos: List<GalleryPage.Photo>,ID:String) {
        photos.clear()
        photos.addAll(newPhotos)
        userID = ID
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PhotoViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_photo, parent, false)
        return PhotoViewHolder(view,photos,userID,this)
    }

    override fun onBindViewHolder(holder: PhotoViewHolder, position: Int) {
        val photo = photos[position]
        holder.bind(photo)
    }

    override fun getItemCount(): Int {
        return photos.size
    }

    class PhotoViewHolder(itemView: View, private var photos: List<GalleryPage.Photo>, private val userID:String,private val adapter: PhotoAdapter) : RecyclerView.ViewHolder(itemView) {
        val photoImageView: ImageView = itemView.findViewById(R.id.gpimageView)

        init {
            // Set long click listener on the itemView
            itemView.setOnLongClickListener {
                showDeleteEntryDialog(photos[adapterPosition],position)
                true
            }
        }

        private fun showDeleteEntryDialog(photo: GalleryPage.Photo,position:Int) {
            val builder = AlertDialog.Builder(itemView.context)
            builder.setTitle("Delete Entry")
            builder.setMessage("Are you sure you want to delete this entry?")

            builder.setPositiveButton("Delete") { _, _ ->
//                deleteEntry(entry,position)
                deleteEntry(position)
            }
            builder.setNegativeButton("Cancel") { dialogInterface, _ ->
                dialogInterface.dismiss()
            }

            builder.create().show()
        }

        private fun deleteEntry(position: Int) {
            val db = FirebaseFirestore.getInstance()
            db.collection("Profiles").document(userID).collection("GiftPhotos").document(photos[position].dID)
                .delete().addOnSuccessListener {
                    val storage = FirebaseStorage.getInstance()
                    val imageRef = storage.getReferenceFromUrl(photos[position].photoUrl)
                    imageRef.delete()
                        .addOnSuccessListener {
                            Toast.makeText(itemView.context, "Deletion successful", Toast.LENGTH_LONG).show()
                            val newEntries = photos.toMutableList()
                            newEntries.removeAt(position)
                            photos = newEntries.toList()

                            // Notify the adapter of the removal
                            (itemView.context as? PhotoAdapter)?.removePhotoAt(position)
                            adapter.removePhotoAt(position)
                        }
                }
                .addOnFailureListener{
                    Toast.makeText(itemView.context, "Deletion Failed Failed", Toast.LENGTH_LONG).show()

                }


        }


        fun bind(photo: GalleryPage.Photo){
            Glide.with(itemView.context).load(photo.photoUrl).into(photoImageView) // Use Glide to load image from URL

        }

    }
    // Function to remove a photo at a specific position
    fun removePhotoAt(position: Int) {
        if (position in 0 until photos.size) {
            val newPhotos = photos.toMutableList()
            newPhotos.removeAt(position)
            photos = newPhotos.toList() as MutableList<GalleryPage.Photo>
            notifyItemRemoved(position)
        }
    }
}
