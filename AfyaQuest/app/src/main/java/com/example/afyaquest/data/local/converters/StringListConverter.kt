package com.example.afyaquest.data.local.converters

import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.lang.reflect.Type

/**
 * Type converter for List<String> in Room database.
 */
class StringListConverter {
    private val gson = Gson()

    private val listOfStringsType: Type =
        TypeToken.getParameterized(List::class.java, String::class.java).type

    @TypeConverter
    fun fromString(value: String?): List<String> {
        if (value == null) return emptyList()
        return gson.fromJson(value, listOfStringsType)
    }

    @TypeConverter
    fun fromList(list: List<String>?): String {
        return gson.toJson(list ?: emptyList<String>())
    }
}
