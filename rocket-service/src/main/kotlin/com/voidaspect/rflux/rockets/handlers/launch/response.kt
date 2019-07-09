package com.voidaspect.rflux.rockets.handlers.launch

import com.voidaspect.rflux.rockets.model.Launch
import com.voidaspect.rflux.rockets.model.LaunchId
import com.voidaspect.rflux.rockets.model.Rocket
import java.time.ZonedDateTime

data class LaunchResponse(val id: LaunchId, val rocket: Rocket, val time: ZonedDateTime) {
    constructor(id: LaunchId, launch: Launch) : this(id, launch.rocket, launch.time)
}