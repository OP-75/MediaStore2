package com.example.mediastore2.gallery

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.example.mediastore2.R
import com.example.mediastore2.databinding.FragmentFirstBinding
import com.example.mediastore2.databinding.FragmentImageViewerBinding
import com.example.mediastore2.gallery.PlayerFragment.Companion.VIDEO_URI
import com.example.mediastore2.serializer.UriSerializer


class ImageViewerFragment : Fragment() {

    private var _binding: FragmentImageViewerBinding? = null
    private val binding get() = _binding!!


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding =FragmentImageViewerBinding.inflate(layoutInflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val imageUriString:String? = requireArguments().getString(IMAGE_URI)
        val imageUri: Uri = UriSerializer.deserialize(imageUriString!!)


        Glide.with(requireContext())
            .load(imageUri) // or URI/path
            .fitCenter()
            .placeholder(R.drawable.baseline_image_24)
            .into(binding.imageView) //imageview to set thumbnail to


    }

    companion object{
        const val IMAGE_URI = "image_uri"
    }

}