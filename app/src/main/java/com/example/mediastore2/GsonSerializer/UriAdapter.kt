package com.example.mediastore2.GsonSerializer

import android.net.Uri
import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.google.gson.JsonPrimitive
import com.google.gson.JsonSerializationContext
import com.google.gson.JsonSerializer
import java.lang.reflect.Type

class UriAdapter : JsonDeserializer<Uri?>, JsonSerializer<Uri?> {
    override fun deserialize(json: JsonElement, type: Type?, context: JsonDeserializationContext?): Uri = runCatching {
        Uri.parse(json.asString)
    }.getOrDefault(Uri.EMPTY)

    override fun serialize(src: Uri?, type: Type?, context: JsonSerializationContext?): JsonElement =
        JsonPrimitive(src.toString())
}