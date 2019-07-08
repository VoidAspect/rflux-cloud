package com.voidaspect.rflux.rockets.repository.launch

import com.voidaspect.rflux.rockets.model.Launch
import com.voidaspect.rflux.rockets.model.LaunchId
import com.voidaspect.rflux.rockets.repository.AbstractInMemoryRepository
import org.springframework.stereotype.Repository

@Repository
class InMemoryLaunchRepository :
        LaunchRepository,
        AbstractInMemoryRepository<Launch, Launch.Existing, Launch.New, LaunchId>() {

    override fun initialize(entity: Launch.New) = Launch.Existing(LaunchId.randomUUID(), entity.rocket, entity.time)

}