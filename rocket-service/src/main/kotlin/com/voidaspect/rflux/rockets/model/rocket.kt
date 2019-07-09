package com.voidaspect.rflux.rockets.model

import java.util.UUID

typealias RocketId = UUID

data class Rocket(
        val warhead: Warhead,
        val target: TargetCoordinates,
        val status: Status = Status.NOT_READY
)

data class TargetCoordinates(val latitude: Double, val longitude: Double)

enum class Warhead { CONVENTIONAL, NUCLEAR }

enum class Status { NOT_READY, READY, LAUNCHED }
