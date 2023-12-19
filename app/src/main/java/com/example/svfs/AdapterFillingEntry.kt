import android.content.Context
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.svfs.FillingEntryPage
import com.example.svfs.R
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale

class AdapterFillingEntry(private val context: Context,private var entries: List<FillingEntryPage.FillingEntry>,
                          private val vehicleNumber:String, private val userID:String,private val totalFilling:Double) :


    RecyclerView.Adapter<AdapterFillingEntry.ViewHolder>() {
    private val db = FirebaseFirestore.getInstance()
    private var totalFillingUpdateListener: TotalFillingUpdateListener? = null




    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view =
            LayoutInflater.from(parent.context).inflate(R.layout.item_filling_entry, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val entry = entries[position]
        holder.bind(entry)

    }

    override fun getItemCount(): Int {
        return entries.size
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val fillingTextView: TextView = itemView.findViewById(R.id.fillingTextView)
        val dateTextView: TextView = itemView.findViewById(R.id.dateTextView)
        val extraTextView:TextView = itemView.findViewById(R.id.extraTextView)
        val editIocn:ImageView = itemView.findViewById(R.id.editIcon)
        private val context: Context = itemView.context
        init {
            // Set long click listener on the itemView
            itemView.setOnLongClickListener {
                showDeleteEntryDialog(entries[adapterPosition],position)
                true
            }
        }


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
            editIocn.setOnClickListener { showUpdateEntryDialog(entry.filling, entry.id.toString(),position) }

        }
        private fun showDeleteEntryDialog(entry: FillingEntryPage.FillingEntry,position: Int) {
            val builder = AlertDialog.Builder(context)
            builder.setTitle("Delete Entry")
            builder.setMessage("Are you sure you want to delete this entry?")

            builder.setPositiveButton("Delete") { _, _ ->
                deleteEntry(entry,position)
            }
            builder.setNegativeButton("Cancel") { dialogInterface, _ ->
                dialogInterface.dismiss()
            }

            builder.create().show()
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

    interface TotalFillingUpdateListener {
        fun onTotalFillingUpdated(newTotalFilling: Double)

    }

    fun showUpdateEntryDialog(filling:Double,dID:String,position: Int) {
        val builder = android.app.AlertDialog.Builder(context)
        builder.setTitle("Update Filling Entry")

        // Inflate the dialog layout
        val inflater: LayoutInflater = LayoutInflater.from(context)
        val dialogView: View = inflater.inflate(R.layout.dialog_add_filling_entry, null)
        builder.setView(dialogView)

        val etfillingEntry = dialogView.findViewById<EditText>(R.id.etFillingEntry)
        val btnClickPhoto = dialogView.findViewById<Button>(R.id.btnClickPhoto)
        btnClickPhoto.visibility = View.GONE

        etfillingEntry.text = filling.toString().toEditable()


        builder.setPositiveButton("Update") { dialogInterface, which ->
            val Newfilling = etfillingEntry.text.toString().toLowerCase().trim()
            val dfilling = Newfilling.toDouble()
            val newTotal = totalFilling-filling+dfilling
            GlobalScope.launch {
                updateEntry(dfilling,newTotal,dID,position)
            }
        }
        builder.setNegativeButton("Cancel") { dialogInterface, which ->
            Toast.makeText(context, "Clicked Cancel", Toast.LENGTH_LONG).show()
        }

        val dialog = builder.create()
        dialog.setOnShowListener {

            val addButton = dialog.getButton(android.app.AlertDialog.BUTTON_POSITIVE)
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

    fun updateEntry(newFilling:Double,newTotal:Double,dID:String,position: Int){
        val entryMap = hashMapOf(
            "Filling" to newFilling,
        )
        db.collection("Profiles").document(userID).collection("Vehicles")
            .document(vehicleNumber).collection("FillingEntry").document(dID).update(entryMap as Map<String, Any>)
            .addOnSuccessListener {
                entries[position].filling =newFilling
                notifyDataSetChanged()
                notifyItemChanged(position)
                updateTotal(newTotal)
            }
            .addOnFailureListener{exception ->
                Toast.makeText(context, "Error... Try again", Toast.LENGTH_LONG).show()

            }
    }

    private fun updateTotal(newTotal: Double) {
        val map = hashMapOf(
            "TotalFilling" to newTotal
        )
        db.collection("Profiles").document(userID).collection("Vehicles").document(vehicleNumber).update(map as Map<String, Any>)
            .addOnSuccessListener {
                totalFillingUpdateListener?.onTotalFillingUpdated(newTotal)
                Toast.makeText(context, "Updated Successfully", Toast.LENGTH_LONG).show()

            }
            .addOnFailureListener{
                Toast.makeText(context, "Updated Failed", Toast.LENGTH_LONG).show()

            }


    }
    fun setTotalFillingUpdateListener(listener: TotalFillingUpdateListener) {
        totalFillingUpdateListener = listener
    }

    fun deleteEntry(entry: FillingEntryPage.FillingEntry,position: Int){
        db.collection("Profiles").document(userID).collection("Vehicles")
            .document(vehicleNumber).collection("FillingEntry").document(entry.id.toString())
            .delete().addOnSuccessListener {
                val storage = FirebaseStorage.getInstance()
                val imageRef = storage.getReferenceFromUrl(entry.photoUrl.toString())
                imageRef.delete()
                    .addOnSuccessListener {
                        val deletedEntry = entries[position]
                        // Remove the entry from the list
                        val newEntries = entries.toMutableList()
                        newEntries.removeAt(position)
                        entries = newEntries.toList()

                        // Notify the adapter of the removal
                        notifyItemRemoved(position)
                        updateTotal(totalFilling-entry.filling)
                    }
                    .addOnFailureListener{
                        Toast.makeText(context, "Deletion Failed Failed", Toast.LENGTH_LONG).show()

                    }

            }
            .addOnFailureListener{
                Toast.makeText(context, "Deletion Failed", Toast.LENGTH_LONG).show()
            }
    }


    fun String.toEditable(): Editable =  Editable.Factory.getInstance().newEditable(this)


}

