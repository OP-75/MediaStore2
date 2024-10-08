package com.example.mediastore2.media.importer

import android.Manifest.permission.READ_MEDIA_IMAGES
import android.Manifest.permission.READ_MEDIA_VIDEO
import android.app.Activity.RESULT_OK
import android.content.ContentResolver
import android.content.ContentUris
import android.content.Context.MODE_PRIVATE
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Bundle
import android.provider.BaseColumns
import android.provider.MediaStore
import android.provider.OpenableColumns
import android.provider.Settings
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.documentfile.provider.DocumentFile
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.anggrayudi.storage.file.child
import com.anggrayudi.storage.file.children
import com.anggrayudi.storage.file.makeFolder
import com.example.mediastore2.R
import com.example.mediastore2.databinding.FragmentFirstBinding
import com.example.mediastore2.serializer.UriSerializer
import com.vmadalin.easypermissions.EasyPermissions
import java.io.File
import java.io.IOException
import kotlin.io.path.Path


class FirstFragment : Fragment() {

    private var _binding: FragmentFirstBinding? = null
    private var selectedUris = mutableListOf<Uri>()

    private fun getHiddenFolder(baseDestinationUri:Uri): DocumentFile?{
        val destFolder = DocumentFile.fromTreeUri(requireContext(), baseDestinationUri)

        var destHiddenFolder:DocumentFile? = null

        if (destFolder?.child(requireContext(), HIDDEN_FOLDER_NAME)?.isDirectory == true){
            //scan if there is a hidden folder already in directory
            destHiddenFolder = destFolder?.child(requireContext(), HIDDEN_FOLDER_NAME)
        }
        else if (destFolder?.name.equals(HIDDEN_FOLDER_NAME)){
            // if selected folder = hidden folder
            destHiddenFolder = destFolder
        }
        else{
            // create new hidden folder
            destHiddenFolder = DocumentFile.fromTreeUri(requireContext(), baseDestinationUri)
                ?.createDirectory(HIDDEN_FOLDER_NAME) //creates numbered multiple if already exists
        }

        return  destHiddenFolder
    }


    private val pickFolder = registerForActivityResult(ActivityResultContracts.OpenDocumentTree()) { baseDestinationUri ->
        if (baseDestinationUri != null) {

            //take persistable permission so that permission maintains when device reboots or app closes, otherwise we cant write/copy images to path after reboot
            takePersistentPermission(baseDestinationUri)

            val destHiddenFolder = getHiddenFolder(baseDestinationUri)

           if (destHiddenFolder!=null){

               val isSavedDestination = saveDestinationUriToSP(destHiddenFolder.uri)
               if (!isSavedDestination){
                   Toast.makeText(requireContext(),"Couldn't save destination path SharedPreferences",Toast.LENGTH_SHORT).show()
               }

               moveImageToFolder(selectedUris, destHiddenFolder.uri)
           }
           else{
               Toast.makeText(requireContext(),"Error: destHiddenFolder = null",Toast.LENGTH_SHORT).show()
           }

        } else {
            Log.i(TAG, "No folder selected")
        }
    }

    private fun takePersistentPermission(uri: Uri) {
        //take persistable permission so that permission maintains when device reboots or app closes, otherwise we cant write/copy images to path after reboot
        val takeFlags: Int = Intent.FLAG_GRANT_READ_URI_PERMISSION or
                Intent.FLAG_GRANT_WRITE_URI_PERMISSION
        requireContext().contentResolver.takePersistableUriPermission(uri, takeFlags)
    }


    private fun saveDestinationUriToSP(uri:Uri):Boolean{

        try {
            val sharedPreferences: SharedPreferences =
                requireContext().getSharedPreferences(SHARED_PREFERENCES_NAME, MODE_PRIVATE) //SHARED_PREFERENCES_NAME is the file name in which the data is staored, one app can have multiple files with multiple data so make sure this name is same across the app for saving settings!!!

            val myEdit = sharedPreferences.edit()


            val uriJson = UriSerializer.serialize(uri)

            myEdit.putString(DESTINATION_URI_JSON_SP_KEY,uriJson)

            return myEdit.commit()
        }
        catch (e:Exception){
            Log.e(TAG, "saveDestinationUri: Error on saving destination path", e)
            return false
        }
    }

    private fun getDestinationUriFromSP():Uri? {
        try {
            val sharedPreferences: SharedPreferences =
                requireContext().getSharedPreferences(SHARED_PREFERENCES_NAME,MODE_PRIVATE)

            val uriJson = sharedPreferences.getString(DESTINATION_URI_JSON_SP_KEY, null)

            if(uriJson!=null){
                return  UriSerializer.deserialize(uriJson)
            }
            else{
                return null
            }

        }
        catch (e:Exception){
            Log.e(TAG, "getDestinationUriFromSP: Error getting destination path from SP", e)
            return null
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

                val destinationFolderUri = getDestinationUriFromSP()
                if (destinationFolderUri!=null){
                    moveImageToFolder(selectedUris,destinationFolderUri)
                }
                else{
                    pickFolder.launch(null) //this will call the move folder function inside its registerForActivityResult
                }


            } else {
                Log.i(TAG, "No media selected")
            }
        }
    }





    private fun moveImageToFolder(sourceUris: MutableList<Uri>, destinationHiddenFolderUri: Uri) {
        if (sourceUris == null || sourceUris.isEmpty()) {
            Log.e(TAG, "Source URIs are null or empty")
            return
        }

        if (!isDestinationValid(destinationHiddenFolderUri)){
            Toast.makeText(requireContext(), "Destination folder is invalid, please change it in settings", Toast.LENGTH_SHORT).show()
            Log.e(TAG, "Destination folder is invalid")
            return
        }

        //! Use coroutines here!!!!
        for (sourceUri in sourceUris){
            try {
                val contentResolver = requireContext().contentResolver

                // Get the name of the source file
                val sourceFileName = getFileName(sourceUri)
                Log.i(TAG, "sourceFileName: ${sourceFileName}")


//                 Create a new file in the destination folder
                val destFile = DocumentFile.fromTreeUri(requireContext(), destinationHiddenFolderUri)
                    ?.createFile("image/* video/*", sourceFileName)


                if (destFile == null) {
                    Log.e(TAG, "Failed to create destination file")
                    return
                }


                // Open input stream from source URI
                contentResolver.openInputStream(sourceUri)?.use { inputStream ->
                    // Open output stream to destination URI
                    contentResolver.openOutputStream(destFile.uri)?.use { outputStream ->
                        inputStream.copyTo(outputStream)
                        //the above function is blocking & uses small buffer size so it can be slow
                        //it also doesnt copy meta data
                        //you can try asking claude what its limitations are & how to fix them

                    }
                }


                Log.i(TAG, "Selected URI: $sourceUri")
                // After successful copy, delete the original file


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

        //if the function is still running that means not problem with try{}
        //! To delete an image we need a external Uri path ie a path in the format: content://media/external/images/media/1000050855
        // the last number at the end is the media unique ID (the Photo picker in Android docs/website doesnt give us this external uri it gives a uri with diffrent path but same media ID)
        deleteSelectedImage(sourceUris)


    }

    private fun isDestinationValid(destinationHiddenFolderUri: Uri) : Boolean {
        val dest = DocumentFile.fromTreeUri(requireContext(), destinationHiddenFolderUri)
        return dest!=null && dest.isDirectory && dest.canWrite() && dest.canRead()
    }


    var deleteResultLauncher: ActivityResultLauncher<IntentSenderRequest> =
        registerForActivityResult(
            ActivityResultContracts.StartIntentSenderForResult()
        ) { result: ActivityResult ->
            if (result.resultCode == RESULT_OK) {
                Toast.makeText(context, "Image deleted!", Toast.LENGTH_SHORT).show()
            }
        }


    private fun deleteSelectedImage(urisToDelete:MutableList<Uri>) {
        //! To delete an image we need a external Uri path ie a path in the format: content://media/external/images/media/1000050855
        // the last number at the end is the media unique ID (the Photo picker in Android docs/website doesnt give us this external uri it gives a uri with diffrent path but same media ID)
        if (urisToDelete==null || urisToDelete.isEmpty()){
            Log.e(TAG, "deleteSelectedImage(): urisToDelete is null or empty")
            return
        }

        val externalUriPathsToDelete = mutableListOf<Uri>()

        urisToDelete.forEach { uri ->
            externalUriPathsToDelete.add(getExternalUriPathWithId(uri))
        }

        if (externalUriPathsToDelete==null || externalUriPathsToDelete.isEmpty()){
            Log.e(TAG, "deleteSelectedImage(): urisToDelete is null or empty")
            return
        }




        try {
            val contentResolver = requireContext().contentResolver
            val urisToDelete = externalUriPathsToDelete.toList()

            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
                val pendingIntent = MediaStore.createDeleteRequest(contentResolver, urisToDelete)
                val intentSender = pendingIntent.intentSender
                val intentSenderRequest = IntentSenderRequest.Builder(intentSender).build()
                deleteResultLauncher.launch(intentSenderRequest)
            } else {
                externalUriPathsToDelete.forEach{uri->
                    // For API level < 30, we need to use the old method
                    val rowsDeleted = contentResolver.delete(uri, null, null)
                    if (rowsDeleted > 0) {
                        Log.i(TAG, "Image deleted successfully")
                        Toast.makeText(
                            requireContext(),
                            "Image deleted successfully",
                            Toast.LENGTH_SHORT
                        ).show()
                    } else {
                        Log.e(TAG, "Failed to delete image")
                        Toast.makeText(requireContext(), "Failed to delete image", Toast.LENGTH_SHORT)
                            .show()
                    }
                }
            }
        }
        catch (e: Exception) {
            Log.e(TAG, "Error deleting file", e)
            Toast.makeText(requireContext(), "Error deleting image: ${e.message}", Toast.LENGTH_SHORT).show()
        }



    }

    private fun getFileName(uri: Uri): String {
        var result: String? = null
        if (uri.scheme == "content") {
            val cursor = requireContext().contentResolver.query(uri, null, null, null, null)
            cursor?.use {
                if (it.moveToFirst()) {
                    val columnIndex = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (columnIndex != -1) {
                        result = it.getString(columnIndex)
                    }
                }
            }
        }
        if (result == null) {
            result = uri.path
            val cut = result?.lastIndexOf('/')
            if (cut != -1) {
                result = result?.substring(cut!! + 1)
            }
        }
        return result ?: "unknown_file"
    }





    fun getExternalUriPathWithId(pickerUri: Uri?): Uri {

        try {
            val context = requireContext()
            val projection = arrayOf(MediaStore.Images.Media._ID)
            val selection = MediaStore.Images.Media._ID + "=?"
            Log.i(TAG, "getExternalUriPathWithId: Last segment = ${pickerUri?.lastPathSegment}")
            val selectionArgs = arrayOf(pickerUri?.lastPathSegment ?: null)

            context.contentResolver.query(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                projection,
                selection,
                selectionArgs,
                null
            )?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val id = cursor.getLong(0)
                    return ContentUris.withAppendedId(
                        MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                        id
                    )
                }
            }
            throw IllegalStateException("Failed to retrieve IMAGE ID")
        } catch (e: Exception) {
            Log.e(TAG, "getExternalUriPathWithId(): ${e.message}", e)
        }

        try {
            val context = requireContext()
            val projection = arrayOf(MediaStore.Video.Media._ID)
            val selection = MediaStore.Video.Media._ID + "=?"
            Log.i(TAG, "getExternalUriPathWithId: Last segment = ${pickerUri?.lastPathSegment}")
            val selectionArgs = arrayOf(pickerUri?.lastPathSegment ?: null)

            context.contentResolver.query(
                MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                projection,
                selection,
                selectionArgs,
                null
            )?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val id = cursor.getLong(0)
                    return ContentUris.withAppendedId(
                        MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                        id
                    )
                }
            }
            throw IllegalStateException("Failed to retrieve VIDEO ID")
        } catch (e: Exception) {
            Log.e(TAG, "getExternalUriPathWithId(): ${e.message}", e)
        }

        Log.w(TAG, "getExternalUriPathWithId(): both contentResolver.query failed")
        return pickerUri!!


    }

    private fun getAllImage() {

        val imageResolver: ContentResolver = requireContext().contentResolver
        val imageUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        val imageCursor = imageResolver.query(imageUri, null, null, null, null)
        if (imageCursor != null && imageCursor.moveToFirst()) {
            // path uri
            val imageCol = imageCursor.getColumnIndex(MediaStore.Images.Media.DATA)
            do {
                val pathId = imageCursor.getString(imageCol)
                //content uri
                val id = imageCursor.getLong(imageCursor.getColumnIndexOrThrow(BaseColumns._ID))
                val uri = Uri.parse(pathId)
                val cUri =
                    ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id)
                Log.d("###qq", cUri.toString())
                Log.d("###qq", uri.toString())

            } while (imageCursor.moveToNext())
        }

    }





    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        _binding = FragmentFirstBinding.inflate(inflater, container, false)
        return binding.root

    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)


        binding.btnOpen.setOnClickListener {
            if (!EasyPermissions.hasPermissions(requireContext(),READ_MEDIA_IMAGES, READ_MEDIA_VIDEO)){
                EasyPermissions.requestPermissions(
                    this,
                    "Need to access photos & videos to delete them after copying",
                    0,
                    READ_MEDIA_IMAGES, READ_MEDIA_VIDEO
                )

                if(!MediaStore.canManageMedia(requireContext())){
                    requestManageMediaPermission() //This permission removes the "Delete" dialog that pops up
                }
            }
            else{

                imageChooser()
            }

        }

        binding.btnNext.setOnClickListener {
            findNavController().navigate(R.id.action_FirstFragment_to_SecondFragment)
        }

        binding.btnOpenGallery.setOnClickListener {
            findNavController().navigate(R.id.first_to_gallery_action)
        }

        binding.btnTest.setOnClickListener {
//            getAllImage()
//            scanFolder()
//            getFiles(".TestHiddenFolder/")
              Log.i(TAG, "Done")
        }
    }

    private fun requestManageMediaPermission() {
        val intent = Intent(Settings.ACTION_REQUEST_MANAGE_MEDIA)
        startActivity(intent)
    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object{
        const val  TAG = "MY_TAG"
        const val DESTINATION_URI_JSON_SP_KEY = "destination_path_sp_key"
        const val SHARED_PREFERENCES_NAME = "com.example.mediastore2"
        const val HIDDEN_FOLDER_NAME = ".TestHiddenFolder"
    }
}