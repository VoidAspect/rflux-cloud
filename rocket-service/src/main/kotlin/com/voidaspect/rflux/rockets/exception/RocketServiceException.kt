package com.voidaspect.rflux.rockets.exception

import org.springframework.http.HttpStatus
import org.springframework.web.server.ResponseStatusException
import java.lang.Exception

open class RocketServiceException : ResponseStatusException {

    constructor(
            status: HttpStatus = HttpStatus.INTERNAL_SERVER_ERROR,
            reason: String,
            cause: Exception
    ) : super(status, reason, cause)

    constructor(
            status: HttpStatus = HttpStatus.INTERNAL_SERVER_ERROR,
            cause: Exception
    ) : super(status, cause.message, cause)

    constructor(
            status: HttpStatus = HttpStatus.INTERNAL_SERVER_ERROR,
            reason: String
    ) : super(status, reason)

}
