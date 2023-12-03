package io.aethibo.core.exceptions

import com.auth0.jwt.exceptions.JWTVerificationException
import io.ktor.http.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.response.*
import io.ktor.server.util.*
import kotlinx.serialization.Serializable
import org.jetbrains.exposed.exceptions.ExposedSQLException
import org.slf4j.LoggerFactory

@Serializable
internal data class ErrorResponseMessage(val message: String)

@Serializable
internal data class ErrorResponse(
    val errors: Map<String, List<String?>> = emptyMap(),
    val internalErrors: List<ErrorResponseMessage> = emptyList()
)

object ErrorExceptionMapping {
    private val LOG = LoggerFactory.getLogger(ErrorExceptionMapping::class.java)

    fun register(app: StatusPagesConfig) {
        app.exception(Exception::class.java) { exception: Exception ->
            LOG.error("Exception occurred for req -> ${context.url()}", exception)
            val error = ErrorResponse(mapOf("Unknown Error" to listOf(exception.message ?: "Error occurred!")))
            context.respond(
                status = HttpStatusCode.InternalServerError,
                message = error
            )
        }
        app.exception(ExposedSQLException::class.java) { exception: Exception ->
            LOG.error("Exception occurred for req -> ${context.url()}", exception)
            val error = ErrorResponse(mapOf("Unknown Error" to listOf("Error occurred!")))
            context.respond(
                status = HttpStatusCode.InternalServerError,
                message = error
            )
        }
        app.exception(BadRequestResponse::class.java) { _: Exception ->
            LOG.warn("BadRequestResponse occurred for req -> ${context.url()}")
            val error = ErrorResponse(mapOf("body" to listOf("can't be empty or invalid")))
            context.respond(
                status = HttpStatusCode.UnprocessableEntity,
                message = error
            )
        }
        app.exception(UnauthorizedResponse::class.java) { _: Exception ->
            LOG.warn("UnauthorizedResponse occurred for req -> ${context.url()}")
            val error = ErrorResponse(mapOf("login" to listOf("User not authenticated!")))
            context.respond(
                status = HttpStatusCode.Unauthorized,
                message = error
            )
        }
        app.exception(ForbiddenResponse::class.java) { _: Exception ->
            LOG.warn("ForbiddenResponse occurred for req -> ${context.url()}")
            val error = ErrorResponse(mapOf("login" to listOf("User doesn't have permissions to perform the action!")))
            context.respond(
                status = HttpStatusCode.Forbidden,
                message = error
            )
        }
        app.exception(JWTVerificationException::class.java) { exception: Exception ->
            LOG.error("JWTVerificationException occurred for req -> ${context.url()}", exception)
            val error = ErrorResponse(mapOf("token" to listOf(exception.message ?: "Invalid JWT token!")))
            context.respond(
                status = HttpStatusCode.Unauthorized,
                message = error
            )
        }
        app.exception(NotFoundResponse::class.java) { _: Exception ->
            LOG.warn("NotFoundResponse occurred for req -> ${context.url()}")
            val error = ErrorResponse(mapOf("body" to listOf("Resource can't be found to fulfill the request.")))
            context.respond(
                status = HttpStatusCode.NotFound,
                message = error
            )
        }
        app.exception(HttpResponseException::class.java) { exception: Exception ->
            LOG.warn("HttpResponseException occurred for req -> ${context.url()}")
            val error = ErrorResponse(mapOf("body" to listOf(exception.message)))
            context.respond(
                status = HttpStatusCode.BadGateway,
                message = error
            )
        }
    }
}
