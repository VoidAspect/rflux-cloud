package com.voidaspect.rflux.rockets.handlers.rockets

import com.voidaspect.rflux.rockets.model.Status
import com.voidaspect.rflux.rockets.model.TargetCoordinates
import com.voidaspect.rflux.rockets.model.Warhead

data class AddRocketCommand(val warhead: Warhead, val target: TargetCoordinates)

data class UpdateRocketCommand(val warhead: Warhead, val status: Status, val target: TargetCoordinates)

data class ChangeWarheadCommand(val warhead: Warhead)

data class ChangeStatusCommand(val status: Status)

data class ChangeTargetCommand(val latitude: Double, val longitude: Double)
