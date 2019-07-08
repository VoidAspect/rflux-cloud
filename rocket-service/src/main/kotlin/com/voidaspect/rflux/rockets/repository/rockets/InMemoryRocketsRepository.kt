package com.voidaspect.rflux.rockets.repository.rockets

import com.voidaspect.rflux.rockets.model.Rocket
import com.voidaspect.rflux.rockets.model.RocketId
import com.voidaspect.rflux.rockets.repository.AbstractInMemoryRepository
import org.springframework.stereotype.Repository

@Repository
class InMemoryRocketsRepository :
        RocketsRepository,
        AbstractInMemoryRepository<Rocket, Rocket.Existing, Rocket.New, RocketId>() {

    override fun initialize(entity: Rocket.New) = Rocket.Existing(
            RocketId.randomUUID(),
            entity.warhead,
            entity.target,
            entity.status
    )

}