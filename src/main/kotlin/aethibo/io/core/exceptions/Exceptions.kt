package aethibo.io.core.exceptions

import io.ktor.http.*

open class HttpResponseException(
    val status: Int,
    message: String,
    val details: Map<String, String> = mapOf()
) : RuntimeException(message)

class OkResponse(
    status: Int = HttpStatusCode.OK.value,
    message: String = "Ok",
    details: Map<String, String> = mapOf()
) : HttpResponseException(status, message, details)

class RedirectResponse(
    status: Int = HttpStatusCode.Found.value,
    message: String = "Redirected",
    details: Map<String, String> = mapOf()
) : HttpResponseException(status, message, details)


class BadRequestResponse(
    message: String = "Bad request",
    details: Map<String, String> = mapOf()
) : HttpResponseException(HttpStatusCode.BadRequest.value, message, details)

class UnauthorizedResponse(
    message: String = "Unauthorized",
    details: Map<String, String> = mapOf()
) : HttpResponseException(HttpStatusCode.Unauthorized.value, message, details)

class ForbiddenResponse(
    message: String = "Forbidden",
    details: Map<String, String> = mapOf()
) : HttpResponseException(HttpStatusCode.Forbidden.value, message, details)

class NotFoundResponse(
    message: String = "Not found",
    details: Map<String, String> = mapOf()
) : HttpResponseException(HttpStatusCode.NotFound.value, message, details)

class MethodNotAllowedResponse(
    message: String = "Method not allowed",
    details: Map<String, String> = mapOf()
) : HttpResponseException(HttpStatusCode.MethodNotAllowed.value, message, details)

class ConflictResponse(
    message: String = "Conflict",
    details: Map<String, String> = mapOf()
) : HttpResponseException(HttpStatusCode.Conflict.value, message, details)

class GoneResponse(
    message: String = "Gone",
    details: Map<String, String> = mapOf()
) : HttpResponseException(HttpStatusCode.Gone.value, message, details)

class InternalServerErrorResponse(
    message: String = "Internal server error",
    details: Map<String, String> = mapOf()
) : HttpResponseException(HttpStatusCode.InternalServerError.value, message, details)

class BadGatewayResponse(
    message: String = "Bad gateway",
    details: Map<String, String> = mapOf()
) : HttpResponseException(HttpStatusCode.BadGateway.value, message, details)

class ServiceUnavailableResponse(
    message: String = "Service unavailable",
    details: Map<String, String> = mapOf()
) : HttpResponseException(HttpStatusCode.ServiceUnavailable.value, message, details)

class GatewayTimeoutResponse(
    message: String = "Gateway timeout",
    details: Map<String, String> = mapOf()
) : HttpResponseException(HttpStatusCode.GatewayTimeout.value, message, details)

class NotAcceptableResponse(
    message: String = "Not acceptable",
    details: Map<String, String> = mapOf(),
) : HttpResponseException(HttpStatusCode.NotAcceptable.value, message, details)
