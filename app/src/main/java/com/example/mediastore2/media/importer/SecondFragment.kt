package com.example.mediastore2.media.importer

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.findNavController
import com.example.mediastore2.databinding.FragmentSecondBinding
import android.app.Activity.RESULT_OK
import android.content.Intent
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.documentfile.provider.DocumentFile
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.list.listItems
import com.anggrayudi.storage.callback.SingleFileConflictCallback
import com.anggrayudi.storage.extension.toDocumentFile
import com.anggrayudi.storage.file.fullName
import com.anggrayudi.storage.file.moveFileTo
import com.anggrayudi.storage.result.SingleFileResult
import com.example.mediastore2.R
import com.example.mediastore2.media.importer.FirstFragment.Companion.TAG
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.launch
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
//        i.setAction(Intent.ACTION_PICK) //for opening gallery
//        i.setAction(Intent.ACTION_GET_CONTENT) //open the basic pop up selector, clicking on 3 dots/options->"Browse" opens the inbuilt file selector with option for selecting from Diffrent gallery apps
        i.setAction(Intent.ACTION_OPEN_DOCUMENT) //return URI in format of content://com.android.providers.media.documents/document/image%3A1000061726
                                                //only this documents URI works with DocumentFile.moveTo()

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
            }
            else if (data.data?.data != null){
                selectedUris.add(data.data?.data!!)
                Log.i(TAG, "getContent() data: ${selectedUris.toString()}")
                pickFolder.launch(null)
            }
            else {
                Log.i(TAG, "No media selected")
            }
        }
    }





    private fun moveImageToFolder(sourceUris: MutableList<Uri>, destinationFolderUri: Uri) {
//        sourceUris should documents URI (documents provider URI???)
//        Try using i.setAction(Intent.ACTION_OPEN_DOCUMENT) for selecting media
//        An example documents URI is:content://com.android.providers.media.documents/document/image%3A1000061726

        if (sourceUris == null || sourceUris.isEmpty()) {
            Log.e(TAG, "Source URIs are null or empty")
            return
        }




        for (sourceUri in sourceUris){


            ioScope.launch {

                try {
                    val source: DocumentFile =
                        DocumentFile.fromSingleUri(requireContext(), sourceUri)!!
                    val targetFolder: DocumentFile =
                        destinationFolderUri.toDocumentFile(requireContext())!!

//                    if (!DocumentFile.isDocumentUri(requireContext(),sourceUri)){
//                        throw IllegalArgumentException("sourceUri is not documents URI\nTry using i.setAction(Intent.ACTION_OPEN_DOCUMENT) for selecting media\nAn example documents URI is:content://com.android.providers.media.documents/document/image%3A1000061726")
//                    }

                    Log.i(
                        TAG,
                        "moveImageToFolder: ${source.isFile},${source.fullName},${source.uri}"
                    )
                    Log.i(
                        TAG,
                        "moveImageToFolder: ${targetFolder.isDirectory},${targetFolder.canWrite()},${targetFolder.uri}"
                    )


                    Log.i(TAG, "Started...")
                    source.moveFileTo(
                        requireContext().applicationContext,
                        targetFolder,
                        onConflict = createFileCallback()
                    ).onCompletion {
                        if (it is CancellationException) {
                            Log.d(TAG, "File move is aborted")
                        }
                        Log.i(TAG, "Ended...")
                    }.collect { result ->
                        //not writing .collect or .onCompletion will not move the file below is the reason why
                        // https://chatgpt.com/share/66ebf06d-9120-8006-b4e9-8079fe9fca9a
                        when (result) {
                            is SingleFileResult.Validating -> Log.d(TAG, "Validating...")
                            is SingleFileResult.Preparing -> Log.d(TAG, "Preparing...")
                            is SingleFileResult.CountingFiles -> Log.d(TAG, "Counting files...")
                            is SingleFileResult.DeletingConflictedFile -> Log.d(TAG, "Deleting conflicted file...")
                            is SingleFileResult.Starting -> Log.d(TAG, "Starting...")
                            is SingleFileResult.InProgress -> {
                                Log.d(TAG, "Progress: ${result.progress.toInt()}%")
                            }
                            is SingleFileResult.Completed -> {
                                Log.d(TAG, "Completed")
                            }
                            is SingleFileResult.Error -> {
                                Log.e(TAG, "An error has occurred: ${result.errorCode.name}")
                            }
                        }
                    }


                } catch (e: IOException) {
                    Log.e(TAG, "Error moving file", e)
                    Toast.makeText(requireContext(), "Error moving image", Toast.LENGTH_SHORT)
                        .show()

                } catch (e: Exception) {
                    Log.e(TAG, "Error moving file", e)
                    Toast.makeText(requireContext(), "Exception: ", Toast.LENGTH_SHORT).show()

                }
            }


        }

    }




    private fun createFileCallback() = object : SingleFileConflictCallback<DocumentFile>(uiScope) {
        override fun onFileConflict(destinationFile: DocumentFile, action: FileConflictAction) {
            handleFileConflict(action)
        }
    }

    private fun handleFileConflict(action: SingleFileConflictCallback.FileConflictAction) {
        MaterialDialog(requireContext())
            .cancelable(false)
            .title(text = "Conflict Found")
            .message(text = "What do you want to do with the file already exists in destination?")
            .listItems(items = listOf("Replace", "Create New", "Skip Duplicate")) { _, index, _ ->
                val resolution = SingleFileConflictCallback.ConflictResolution.entries[index]
                action.confirmResolution(resolution)
                if (resolution == SingleFileConflictCallback.ConflictResolution.SKIP) {
                    Toast.makeText(requireContext(), "Skipped duplicate file", Toast.LENGTH_SHORT).show()
                }
            }
            .show()
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