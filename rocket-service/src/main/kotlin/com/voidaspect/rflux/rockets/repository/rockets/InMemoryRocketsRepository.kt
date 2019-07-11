package com.voidaspect.rflux.rockets.repository.rockets

import com.voidaspect.rflux.rockets.model.Rocket
import com.voidaspect.rflux.rockets.repository.AbstractInMemoryUUIDRepository
import org.springframework.stereotype.Repository

@Repository
class InMemoryRocketsRepository : RocketsRepository, AbstractInMemoryUUIDRepository<Rocket>()