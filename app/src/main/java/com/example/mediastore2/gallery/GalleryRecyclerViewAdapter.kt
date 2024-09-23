package com.example.mediastore2.gallery

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.documentfile.provider.DocumentFile
import androidx.navigation.Navigation
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.mediastore2.R
import com.example.mediastore2.gallery.ImageViewerFragment.Companion.IMAGE_URI
import com.example.mediastore2.gallery.PlayerFragment.Companion.VIDEO_URI
import com.example.mediastore2.serializer.UriSerializer
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
            holder.ivVideoIcon.visibility = View.VISIBLE
            holder.itemView.setOnClickListener {
                val stringUri:String = UriSerializer.serialize(mList[position].uri)
                val bundle = Bundle()
                bundle.putString(VIDEO_URI,stringUri)
                val navController = Navigation.findNavController(holder.itemView)
                navController.navigate(R.id.action_galleryFragment_to_playerFragment,bundle)
            }
        }
        else{
            holder.ivVideoIcon.visibility = View.INVISIBLE
            holder.itemView.setOnClickListener {
                val stringUri:String = UriSerializer.serialize(mList[position].uri)
                val bundle = Bundle()
                bundle.putString(IMAGE_URI,stringUri)
                val navController = Navigation.findNavController(holder.itemView)
                navController.navigate(R.id.action_galleryFragment_to_imageViewerFragment,bundle)
            }
        }

        holder.tvSize.text = getImageSize(mList[position].length())


    }

    // Holds the views for adding it to image and text
    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val imageView: ImageView = itemView.findViewById(R.id.imageView)
        val ivVideoIcon: ImageView = itemView.findViewById(R.id.ivVideoIcon)
        val tvSize: TextView = itemView.findViewById(R.id.tvSize)

    }


    private fun getImageSize(sizeInBytes: Long): String {
        return when {
            sizeInBytes <= 102400 -> String.format("%.1f KB", sizeInBytes / 1024.0)
            sizeInBytes <= 1047527424 -> String.format("%.1f MB", sizeInBytes / (1024.0*1024.0))
            else -> String.format("%.1f GB", sizeInBytes / (1024.0*1024.0*1024.0))
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

}