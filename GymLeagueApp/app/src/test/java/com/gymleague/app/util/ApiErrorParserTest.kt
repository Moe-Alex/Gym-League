package com.gymleague.app.util

import org.junit.Assert.assertEquals
import org.junit.Test

class ApiErrorParserTest {

    private val fallback = "Something went wrong"

    @Test
    fun nullOrBlankBody_usesFallback() {
        assertEquals(fallback, ApiErrorParser.message(null, fallback))
        assertEquals(fallback, ApiErrorParser.message("   ", fallback))
    }

    @Test
    fun plainText_isShownAsIs() {
        assertEquals(
            "You cannot accept your own challenge.",
            ApiErrorParser.message("You cannot accept your own challenge.", fallback)
        )
    }

    @Test
    fun quotedPlainText_hasQuotesRemoved() {
        assertEquals("Challenge has ended.", ApiErrorParser.message("\"Challenge has ended.\"", fallback))
    }

    @Test
    fun validationErrors_showTheFirstMessage() {
        val body = """{"title":"One or more validation errors occurred.","errors":{"Title":["The Title field is required."]}}"""
        assertEquals("The Title field is required.", ApiErrorParser.message(body, fallback))
    }

    @Test
    fun problemDetails_fallBackToTitle() {
        val body = """{"title":"Not Found","status":404}"""
        assertEquals("Not Found", ApiErrorParser.message(body, fallback))
    }

    @Test
    fun brokenJson_usesFallback() {
        assertEquals(fallback, ApiErrorParser.message("{not json", fallback))
    }

    @Test
    fun veryLongText_usesFallback() {
        assertEquals(fallback, ApiErrorParser.message("x".repeat(500), fallback))
    }
}
