package com.voidaspect.rflux.rockets.repository.rockets

import com.voidaspect.rflux.rockets.model.Rocket
import com.voidaspect.rflux.rockets.model.RocketId
import com.voidaspect.rflux.rockets.repository.RepositoryOperations

interface RocketsRepository : RepositoryOperations<Rocket, RocketId>