package com.voidaspect.rflux.rockets.model

import java.time.ZonedDateTime
import java.util.UUID

typealias LaunchId = UUID

sealed class Launch {

    abstract val rocket: Rocket

    abstract val time: ZonedDateTime

    data class Existing(
            override val id: LaunchId,
            override val rocket: Rocket,
            override val time: ZonedDateTime
    ) : Launch(), Identity<Launch, LaunchId>

    data class New(
            override val rocket: Rocket,
            override val time: ZonedDateTime = ZonedDateTime.now()
    ) : Launch()

}