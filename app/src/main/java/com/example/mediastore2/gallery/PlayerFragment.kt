package com.example.mediastore2.gallery

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ScaleGestureDetector
import android.view.View
import android.view.ViewGroup
import androidx.annotation.OptIn
import androidx.documentfile.provider.DocumentFile
import androidx.fragment.app.Fragment
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.example.mediastore2.databinding.FragmentPlayerBinding
import com.example.mediastore2.serializer.UriSerializer

class PlayerFragment : Fragment() {

    private var _binding: FragmentPlayerBinding? = null
    private val binding get() = _binding!!
    private lateinit var player: ExoPlayer
    private lateinit var scaleGestureDetector: ScaleGestureDetector



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentPlayerBinding.inflate(inflater, container, false)
        return binding.root
    }


    @OptIn(UnstableApi::class)
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {

        super.onViewCreated(view, savedInstanceState)

        player = ExoPlayer.Builder(requireContext()).build()

        val videoUriString:String? = requireArguments().getString(VIDEO_URI)
        val videoUri = UriSerializer.deserialize(videoUriString!!)


        val file = DocumentFile.fromSingleUri(requireContext(), videoUri)

        // Bind the player to the view.
        binding.playerView.player = player
        scaleGestureDetector =
            ScaleGestureDetector(requireContext(), CustomOnScaleGestureListener(binding.playerView))

        binding.playerView.setOnTouchListener { view, motionEvent ->
            binding.root.performClick()
            binding.playerView.onTouchEvent(motionEvent)
            scaleGestureDetector?.onTouchEvent(motionEvent)
            true
        }

        val mediaItem: MediaItem = MediaItem.fromUri(videoUri)
        player.setMediaItem(mediaItem)
        player.prepare()
        player.play()



    }




    override fun onDestroyView() {
        super.onDestroyView()
        player.release()
    }

    companion object{
        const val VIDEO_URI = "video_uri"
        const val TAG = "MY_TAG"
    }

}

private class CustomOnScaleGestureListener(
    private val player: PlayerView
) : ScaleGestureDetector.SimpleOnScaleGestureListener() {
    private var scaleFactor = 0f

    @OptIn(UnstableApi::class)
    override fun onScale(
        detector: ScaleGestureDetector
    ): Boolean {
        scaleFactor = detector.scaleFactor
        return true
    }

    @OptIn(UnstableApi::class)
    override fun onScaleBegin(
        detector: ScaleGestureDetector
    ) : Boolean{

        return true
    }

    @OptIn(UnstableApi::class)
    override fun onScaleEnd(detector: ScaleGestureDetector) {
        if (scaleFactor > 1) {
            player.resizeMode = AspectRatioFrameLayout.RESIZE_MODE_ZOOM
        } else {
            player.resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
        }
    }
}