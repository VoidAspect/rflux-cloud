package com.voidaspect.rflux.rockets.repository.rockets

import com.voidaspect.rflux.rockets.model.Stored
import com.voidaspect.rflux.rockets.model.Rocket
import com.voidaspect.rflux.rockets.model.RocketId
import com.voidaspect.rflux.rockets.repository.AbstractInMemoryRepository
import org.springframework.stereotype.Repository

@Repository
class InMemoryRocketsRepository : RocketsRepository, AbstractInMemoryRepository<Rocket, RocketId>() {

    override fun initialize(entity: Rocket) = Stored(RocketId.randomUUID(), entity)

}