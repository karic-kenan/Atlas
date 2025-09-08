package io.aethibo.core.exceptions

import java.io.IOException

/**
 * Base Class for handling errors/failures/exceptions.
 * Every feature specific failure should extend [FeatureFailure] class.
 */
sealed class Failure {
    data object NetworkConnection : Failure()
    data class Server(val code: Int, val message: String = "Server error") : Failure()
    data class Unknown(val message: String) : Failure()

    /** Extend this class for feature specific failures. */
    abstract class FeatureFailure : Failure()
}

// Extension function to convert exceptions to failures
fun Throwable.toFailure(): Failure = when (this) {
    is IOException -> Failure.NetworkConnection
    else -> Failure.Unknown(message ?: "Unknown error occurred")
}
