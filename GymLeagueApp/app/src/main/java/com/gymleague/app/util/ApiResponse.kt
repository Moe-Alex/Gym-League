package com.gymleague.app.util

import retrofit2.Response
import java.io.IOException

const val OFFLINE_MESSAGE = "You're offline. Connect to the internet and try again."

/**
 * Turns a Retrofit response into a Result without ever using !!.
 * A missing body counts as a failure, the same as an error code.
 */
fun <T> Response<T>.toApiResult(fallback: String): Result<T> {
    val body = body()

    if (isSuccessful && body != null) {
        return Result.success(body)
    }

    return Result.failure(ApiException(code(), "$fallback: ${code()}"))
}

/**
 * For actions that need the internet (create, join, accept...). If the phone is offline the user
 * sees a clear message instead of a technical one like "Unable to resolve host".
 */
fun <T> Exception.toFailure(): Result<T> {
    return if (this is IOException) {
        Result.failure(OfflineException(OFFLINE_MESSAGE))
    } else {
        Result.failure(this)
    }
}