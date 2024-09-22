package com.example.mediastore2

import android.content.Context.MODE_PRIVATE
import android.content.SharedPreferences
import android.database.Cursor
import android.graphics.Rect
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.documentfile.provider.DocumentFile
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ItemDecoration
import com.anggrayudi.storage.file.getAbsolutePath
import com.example.mediastore2.FirstFragment.Companion.DESTINATION_URI_JSON_SP_KEY
import com.example.mediastore2.FirstFragment.Companion.SHARED_PREFERENCES_NAME
import com.example.mediastore2.FirstFragment.Companion.TAG
import com.example.mediastore2.GsonSerializer.UriAdapter
import com.example.mediastore2.databinding.FragmentGalleryBinding
import com.example.mediastore2.serializer.UriSerializer
import com.google.gson.Gson
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


class GalleryFragment : Fragment() {

    private var _binding: FragmentGalleryBinding? = null
    private val binding get() = _binding!!



    private fun getDestinationUriFromSP(): Uri? {
        try {
            val sharedPreferences: SharedPreferences =
                requireContext().getSharedPreferences(SHARED_PREFERENCES_NAME, MODE_PRIVATE)

            val uriJson = sharedPreferences.getString(DESTINATION_URI_JSON_SP_KEY, null)

            if (uriJson != null) {
                return UriSerializer.deserialize(uriJson)
            } else {
                return null
            }

        } catch (e: Exception) {
            Log.e(TAG, "getDestinationUriFromSP: Error getting destination path from SP", e)
            return null
        }
    }

    private fun scanFolder(): Array<DocumentFile>? {

        val files =
            DocumentFile.fromTreeUri(requireContext(), getDestinationUriFromSP()!!)?.listFiles()

        files?.forEach {

            Log.i(TAG, "File: ${it.getAbsolutePath(requireContext())}")
            Log.i(TAG, "File: ${it.uri.toString()}")

        }

        return files

    }

    private fun getFiles(directory_name: String) {
        var selection: String
        selection = MediaStore.Files.FileColumns.RELATIVE_PATH + " =? "
        var selectionArgs = arrayOf(directory_name)
        val contentResolver = requireContext().contentResolver
        val cursor: Cursor? = contentResolver.query(
            MediaStore.Files.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY),
            null,
            selection,
            selectionArgs,
            null
        )

        if (cursor != null && cursor.moveToFirst()) {

            do {
                Log.i(TAG, "getFiles: ${cursor}")

            } while (cursor.moveToNext())
        }

        cursor?.close()


    }


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentGalleryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val recyclerView = binding.recyclerView


        val span = 3
        val spacing = 40
        val includeEdge = true
        recyclerView.layoutManager = GridLayoutManager(requireContext(), span)
        recyclerView.addItemDecoration(GridSpacingItemDecoration(span, spacing, includeEdge))

        CoroutineScope(Dispatchers.IO).launch {
            val files = scanFolder()
            val adapter = GalleryRecyclerViewAdapter(files!!)

            withContext(Dispatchers.Main){
                recyclerView.adapter = adapter
                binding.btnTest.visibility = View.INVISIBLE
            }

        }


        binding.btnTest.setOnClickListener {

        }
    }
}


private class GridSpacingItemDecoration(
    private val spanCount: Int,
    private val spacing: Int,
    private val includeEdge: Boolean
) :
    ItemDecoration() {
    override fun getItemOffsets(
        outRect: Rect,
        view: View,
        parent: RecyclerView,
        state: RecyclerView.State
    ) {
        val position = parent.getChildAdapterPosition(view) // item position
        val column = position % spanCount // item column

        if (includeEdge) {
            outRect.left =
                spacing - column * spacing / spanCount // spacing - column * ((1f / spanCount) * spacing)
            outRect.right =
                (column + 1) * spacing / spanCount // (column + 1) * ((1f / spanCount) * spacing)

            if (position < spanCount) { // top edge
                outRect.top = spacing
            }
            outRect.bottom = spacing // item bottom
        } else {
            outRect.left = column * spacing / spanCount // column * ((1f / spanCount) * spacing)
            outRect.right =
                spacing - (column + 1) * spacing / spanCount // spacing - (column + 1) * ((1f /    spanCount) * spacing)
            if (position >= spanCount) {
                outRect.top = spacing // item top
            }
        }
    }
}