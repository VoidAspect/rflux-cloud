package com.voidaspect.rflux.rockets.model

import java.time.ZonedDateTime
import java.util.UUID

typealias LaunchId = UUID

data class Launch(
        val rocket: Rocket,
        val time: ZonedDateTime = ZonedDateTime.now()
)