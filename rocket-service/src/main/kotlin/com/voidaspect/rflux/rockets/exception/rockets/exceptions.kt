package com.voidaspect.rflux.rockets.exception.rockets

import com.voidaspect.rflux.rockets.exception.RocketServiceException
import com.voidaspect.rflux.rockets.model.RocketId
import org.springframework.http.HttpStatus

class RocketNotFoundException(id: RocketId) : RocketServiceException(
        status = HttpStatus.NOT_FOUND,
        reason = "Rocket with id $id was not found"
)

class RocketNotReadyException(id: RocketId) : RocketServiceException(
        status = HttpStatus.BAD_REQUEST,
        reason = "Rocket with id $id was is not ready for launch"
)

class RocketAlreadyLaunchedException(id: RocketId) : RocketServiceException(
        status = HttpStatus.BAD_REQUEST,
        reason = "Rocket with id $id was has already been launched"
)

class CannotChangeStatusToLaunchedException : RocketServiceException(
        status = HttpStatus.BAD_REQUEST,
        reason = "Status can't be set to LAUNCHED through /rockets API. Use /launch API instead"
)