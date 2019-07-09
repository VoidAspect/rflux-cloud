package com.voidaspect.rflux.rockets.repository.launch

import com.voidaspect.rflux.rockets.model.Stored
import com.voidaspect.rflux.rockets.model.Launch
import com.voidaspect.rflux.rockets.model.LaunchId
import com.voidaspect.rflux.rockets.repository.AbstractInMemoryRepository
import org.springframework.stereotype.Repository

@Repository
class InMemoryLaunchRepository : LaunchRepository, AbstractInMemoryRepository<Launch, LaunchId>() {

    override fun initialize(entity: Launch) = Stored(LaunchId.randomUUID(), entity)

}