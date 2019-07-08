package com.voidaspect.rflux.rockets.exception.launch

import com.voidaspect.rflux.rockets.exception.RocketServiceException
import com.voidaspect.rflux.rockets.model.LaunchId
import org.springframework.http.HttpStatus

class LaunchRecordNotFoundException(launchId: LaunchId) : RocketServiceException(
        status = HttpStatus.NOT_FOUND,
        reason = "Launch record with id $launchId was not found"
)

class LaunchFailedException(reason: String, cause: RocketServiceException) : RocketServiceException(
        status = cause.status,
        reason = "$reason (${cause.reason})",
        cause = cause
)