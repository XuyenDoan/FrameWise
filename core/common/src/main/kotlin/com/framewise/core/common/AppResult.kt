package com.framewise.core.common

/**
 * Lightweight success/failure wrapper for operations that can fail in a way
 * the UI must react to (e.g. camera permission denied, no camera available).
 * Kept separate from Kotlin's stdlib `Result` so it plays nicely with sealed
 * `when` exhaustiveness checks across module boundaries.
 */
sealed interface AppResult<out T> {
    data class Success<T>(val data: T) : AppResult<T>
    data class Failure(val error: AppError) : AppResult<Nothing>
}

sealed interface AppError {
    data class CameraUnavailable(val reason: String) : AppError
    data class PermissionDenied(val permission: String) : AppError
    data class Unknown(val throwable: Throwable) : AppError
}

inline fun <T> AppResult<T>.onSuccess(action: (T) -> Unit): AppResult<T> {
    if (this is AppResult.Success) action(data)
    return this
}

inline fun <T> AppResult<T>.onFailure(action: (AppError) -> Unit): AppResult<T> {
    if (this is AppResult.Failure) action(error)
    return this
}
