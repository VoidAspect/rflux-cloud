package com.voidaspect.rflux.rockets.model

import java.util.UUID

typealias RocketId = UUID

sealed class Rocket {

    abstract val warhead: Warhead

    abstract val target: TargetCoordinates

    abstract val status: Status

    data class Existing(
            override val id: RocketId,
            override val warhead: Warhead,
            override val target: TargetCoordinates,
            override val status: Status
    ) : Rocket(), Identity<Rocket, LaunchId>

    data class New(
            override val warhead: Warhead,
            override val target: TargetCoordinates,
            override val status: Status = Status.NOT_READY
    ) : Rocket()

}

data class TargetCoordinates(val latitude: Double, val longitude: Double)

enum class Warhead { CONVENTIONAL, NUCLEAR }

enum class Status { NOT_READY, READY, LAUNCHED }
