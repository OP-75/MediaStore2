package com.example.mediastore2

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.findNavController
import com.example.mediastore2.databinding.FragmentSecondBinding
import android.Manifest.permission.READ_MEDIA_IMAGES
import android.Manifest.permission.READ_MEDIA_VIDEO
import android.app.Activity.RESULT_OK
import android.content.ContentResolver
import android.content.ContentUris
import android.content.Intent
import android.net.Uri
import android.provider.BaseColumns
import android.provider.MediaStore
import android.provider.OpenableColumns
import android.util.Log
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.documentfile.provider.DocumentFile
import com.anggrayudi.storage.SimpleStorageHelper
import com.anggrayudi.storage.callback.MultipleFilesConflictCallback
import com.anggrayudi.storage.callback.SingleFileConflictCallback
import com.anggrayudi.storage.callback.SingleFolderConflictCallback
import com.anggrayudi.storage.extension.fromTreeUri
import com.anggrayudi.storage.extension.toDocumentFile
import com.anggrayudi.storage.file.fullName
import com.anggrayudi.storage.file.moveFileTo
import com.anggrayudi.storage.file.moveFolderTo
import com.anggrayudi.storage.media.MediaFile
import com.anggrayudi.storage.result.SingleFileResult
import com.example.mediastore2.FirstFragment.Companion.TAG
import com.vmadalin.easypermissions.EasyPermissions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.launch
import java.io.File
import java.io.IOException
import kotlin.coroutines.cancellation.CancellationException


/**
 * A simple [Fragment] subclass as the second destination in the navigation.
 */
class SecondFragment : Fragment() {

    private var _binding: FragmentSecondBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    private var selectedUris = mutableListOf<Uri>()

    private val job = Job()
    private val ioScope = CoroutineScope(Dispatchers.IO + job)
    private val uiScope = CoroutineScope(Dispatchers.Main + job)

    private  lateinit var targetUri:Uri
    private val pickFolder = registerForActivityResult(ActivityResultContracts.OpenDocumentTree()) { destUri ->
        if (destUri != null) {
            targetUri = destUri
            moveImageToFolder(selectedUris, destUri)
        } else {
            Log.i(TAG, "No folder selected")
        }
    }



    fun imageChooser() {
        // create an instance of the
        // intent of the type image

        val i = Intent()
        i.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
        i.setType("image/* video/*")
        i.setAction(Intent.ACTION_PICK) //for opening gallery
//        i.setAction(Intent.ACTION_GET_CONTENT) //open the basic pop up selector, clicking on 3 dots/options->"Browse" opens the inbuilt file selector with option for selecting from Diffrent gallery apps


        // pass the constant to compare it
        // with the returned requestCode
        getContent.launch(i)
    }

    // Registers a photo picker activity launcher in single-select mode.
    private val getContent = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { data:ActivityResult ->
        // Callback is invoked after the user selects a media item or closes the
        // photo picker.
        if (data.resultCode== RESULT_OK){

            //use below if i.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, false)
//            val uri: Uri? = data.data?.data

            //use "clipdata" for multiple selected items, "data" only gives u 1 item

            selectedUris = mutableListOf<Uri>() //reset var

            if (data.data?.clipData != null) {
                val clipData = data.data?.clipData!!
                for (index in 0..clipData.itemCount-1){
                    selectedUris.add(clipData.getItemAt(index).uri)
                }
                Log.i(TAG, "getContent() clipdata: ${selectedUris.toString()}")
                pickFolder.launch(null)
            } else {
                Log.i(TAG, "No media selected")
            }
        }
    }





    private fun moveImageToFolder(sourceUris: MutableList<Uri>, destinationFolderUri: Uri) {
        if (sourceUris == null || sourceUris.isEmpty()) {
            Log.e(TAG, "Source URIs are null or empty")
            return
        }



        for (sourceUri in sourceUris){

            try {
                val source:DocumentFile = sourceUri.toDocumentFile(requireContext())!!
                val targetFolder:DocumentFile = destinationFolderUri.toDocumentFile(requireContext())!!

                Log.i(TAG, "moveImageToFolder: ${source.isFile},${source.fullName}")
                Log.i(TAG, "moveImageToFolder: ${targetFolder.isDirectory},${targetFolder.canWrite()}")

                ioScope.launch {


                }


            } catch (e: IOException) {
                Log.e(TAG, "Error moving file", e)
                Toast.makeText(requireContext(), "Error moving image", Toast.LENGTH_SHORT).show()
                return
            }
            catch (e: Exception){
                Log.e(TAG, "Error moving file", e)
                Toast.makeText(requireContext(), "Exception: ", Toast.LENGTH_SHORT).show()
                return
            }


        }

    }


    private fun createFileCallback(): SingleFileConflictCallback<DocumentFile> = object : SingleFileConflictCallback<DocumentFile>(uiScope) {
        override fun onFileConflict(destinationFile: DocumentFile, action: FileConflictAction) {
            Toast.makeText(requireContext(),"onFileConflict",Toast.LENGTH_SHORT).show()
        }
    }



    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        _binding = FragmentSecondBinding.inflate(inflater, container, false)
        return binding.root

    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.buttonSecond.setOnClickListener {
            findNavController().navigate(R.id.action_SecondFragment_to_FirstFragment)
        }

        binding.btnMoveSimpleStorage.setOnClickListener {
            imageChooser()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}