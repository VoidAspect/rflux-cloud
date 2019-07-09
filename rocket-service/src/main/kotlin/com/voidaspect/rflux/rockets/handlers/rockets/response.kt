package com.voidaspect.rflux.rockets.handlers.rockets

import com.voidaspect.rflux.rockets.model.*

data class RocketResponse(val id: RocketId, val warhead: Warhead, val status: Status, val target: TargetCoordinates) {
    constructor(id: RocketId, rocket: Rocket) : this(id, rocket.warhead, rocket.status, rocket.target)
}