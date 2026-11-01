package com.kotlinide.shahid.app.util

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Robust Result wrapper for all repository, file, and network operations.
 * Prevents unhandled exceptions from propagating to the UI or killing the app.
 */
sealed class AppResult<out T> {
    data class Success<T>(val data: T) : AppResult<T>()
    data class Error(val message: String, val cause: Throwable? = null) : AppResult<Nothing>()
    data object Loading : AppResult<Nothing>()

    val isSuccess: Boolean get() = this is Success
    val isError: Boolean get() = this is Error
    val isLoading: Boolean get() = this is Loading

    fun getOrNull(): T? = when (this) {
        is Success -> data
        else -> null
    }

    fun getOrDefault(defaultValue: @UnsafeVariance T): T = when (this) {
        is Success -> data
        else -> defaultValue
    }

    inline fun onSuccess(action: (T) -> Unit): AppResult<T> {
        if (this is Success) action(data)
        return this
    }

    inline fun onError(action: (message: String, cause: Throwable?) -> Unit): AppResult<T> {
        if (this is Error) action(message, cause)
        return this
    }
}

/**
 * Safely executes a suspend block on Dispatchers.IO, catching any Throwable
 * (except Coroutine CancellationException) and recording it in CrashReportingHelper.
 */
suspend fun <T> safeIoCall(
    operationName: String,
    block: suspend () -> T
): AppResult<T> = withContext(Dispatchers.IO) {
    try {
        val result = block()
        AppResult.Success(result)
    } catch (e: CancellationException) {
        throw e // Do not intercept coroutine cancellations
    } catch (t: Throwable) {
        CrashReportingHelper.logHandledException("safeIoCall:$operationName", t.message ?: "Unknown error", t)
        val readableMessage = when {
            t.message != null && t.message!!.isNotBlank() -> t.message!!
            else -> "Operation '$operationName' failed (${t.javaClass.simpleName})"
        }
        AppResult.Error(readableMessage, t)
    }
}
