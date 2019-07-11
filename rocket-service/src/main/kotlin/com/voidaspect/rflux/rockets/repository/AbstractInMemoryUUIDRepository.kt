package com.voidaspect.rflux.rockets.repository

import com.voidaspect.rflux.rockets.model.Stored
import java.util.UUID

abstract class AbstractInMemoryUUIDRepository <T> : AbstractInMemoryRepository<T, UUID>() {
    override fun initialize(entity: T) = Stored(UUID.randomUUID(), entity)
}