package com.example.mediastore2

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.documentfile.provider.DocumentFile
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import java.net.URLConnection




class GalleryRecyclerViewAdapter(private val mList: Array<DocumentFile>) : RecyclerView.Adapter<GalleryRecyclerViewAdapter.ViewHolder>() {

    private lateinit var context: Context

        // create new views
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        // inflates the card_view_design view
        // that is used to hold list item
        val view = LayoutInflater.from(parent.context).inflate(R.layout.gallery_item, parent, false)
        context = parent.context
        return ViewHolder(view)
    }

    // binds the list items to a view
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {

//        holder.imageView.setImageResource(R.drawable.ic_launcher_foreground)

        Glide.with(context)
            .load(mList[position].uri) // or URI/path
            .fitCenter()
            .placeholder(R.drawable.baseline_image_24)
            .into(holder.imageView); //imageview to set thumbnail to

        if(isVideoFile(mList[position].uri.toString())){
            holder.itemView.findViewById<ImageView>(R.id.ivVideoIcon).visibility = View.VISIBLE
        }


    }

    // return the number of the items in the list
    override fun getItemCount(): Int {
        return mList.size
    }

    fun isVideoFile(path: String?): Boolean {
        val mimeType = URLConnection.guessContentTypeFromName(path)
        return mimeType != null && mimeType.startsWith("video")
    }



    // Holds the views for adding it to image and text
    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val imageView: ImageView = itemView.findViewById(R.id.imageView)

    }
}
