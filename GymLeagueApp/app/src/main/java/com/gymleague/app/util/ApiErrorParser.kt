package com.gymleague.app.util

import com.google.gson.JsonParser

/**
 * Turns the body of a failed API response into a short message the user can read.
 *
 * The API refuses bad requests in two shapes:
 *  - plain text, for example: You cannot accept your own challenge.
 *  - ASP.NET "problem details" JSON, for example {"title":"One or more validation errors occurred."}
 */
object ApiErrorParser {

    private const val MAX_MESSAGE_LENGTH = 160

    fun message(rawBody: String?, fallback: String): String {
        val text = rawBody?.trim().orEmpty()

        if (text.isEmpty()) {
            return fallback
        }

        if (text.startsWith("{")) {
            return fromJson(text) ?: fallback
        }

        val plain = text.removeSurrounding("\"")

        return if (plain.isNotBlank() && plain.length <= MAX_MESSAGE_LENGTH) plain else fallback
    }

    private fun fromJson(text: String): String? {
        return try {
            val json = JsonParser.parseString(text).asJsonObject

            // Validation errors: {"errors":{"Title":["The Title field is required."]}}
            val errors = json.getAsJsonObject("errors")
            val firstError = errors?.entrySet()?.firstOrNull()?.value?.asJsonArray?.firstOrNull()?.asString

            firstError
                ?: json.get("detail")?.asString
                ?: json.get("title")?.asString
        } catch (e: Exception) {
            null
        }
    }
}
