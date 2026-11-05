package com.gymleague.app.util

import com.google.gson.Gson
import com.google.gson.JsonParseException
import com.google.gson.reflect.TypeToken

/**
 * Room stores simple columns, so a list inside a row (the exercises in a workout, the members
 * of a squad) is saved as JSON text and turned back into a list when it is read.
 */
object JsonLists {

    val gson = Gson()

    fun <T> encode(items: List<T>): String = gson.toJson(items)

    inline fun <reified T> decode(json: String?): List<T> {
        if (json.isNullOrBlank()) return emptyList()

        return try {
            val type = TypeToken.getParameterized(List::class.java, T::class.java).type
            gson.fromJson<List<T>>(json, type) ?: emptyList()
        } catch (e: JsonParseException) {
            emptyList()
        }
    }
}