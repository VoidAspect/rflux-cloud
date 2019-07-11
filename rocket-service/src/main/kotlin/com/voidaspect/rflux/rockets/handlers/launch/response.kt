package com.voidaspect.rflux.rockets.handlers.launch

import com.voidaspect.rflux.rockets.handlers.rockets.RocketResponse
import com.voidaspect.rflux.rockets.model.Launch
import com.voidaspect.rflux.rockets.model.LaunchId
import java.time.ZonedDateTime

data class LaunchResponse(
        val id: LaunchId,
        val rocket: RocketResponse,
        val time: ZonedDateTime
) {
    constructor(
            id: LaunchId,
            launch: Launch
    ) : this(
            id,
            RocketResponse(launch.rocket.id, launch.rocket.value),
            launch.time
    )
}