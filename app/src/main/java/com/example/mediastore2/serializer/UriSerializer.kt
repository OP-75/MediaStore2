package com.example.mediastore2.serializer

import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import com.example.mediastore2.GsonSerializer.UriAdapter
import com.google.gson.Gson

class UriSerializer {
    companion object{
        fun serialize(uri: Uri):String{
            //Since Uri is a abstract class Gson() can serialize & deserialze it hence we need to add our own custom UriAdapter which does the serializing & deserailizing
            val gson = Gson().newBuilder().registerTypeHierarchyAdapter(Uri::class.java, UriAdapter()).create()
            return gson.toJson(uri)
        }

        fun deserialize(json:String): Uri {
            //Since Uri is a abstract class Gson() can serialize & deserialze it hence we need to add our own custom UriAdapter which does the serializing & deserailizing
            val gson = Gson().newBuilder().registerTypeHierarchyAdapter(Uri::class.java, UriAdapter()).create()
            return gson.fromJson(json, Uri::class.java)
        }
    }
}