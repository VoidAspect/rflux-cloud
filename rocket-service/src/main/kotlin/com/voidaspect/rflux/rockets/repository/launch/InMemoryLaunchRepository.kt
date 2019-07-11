package com.voidaspect.rflux.rockets.repository.launch

import com.voidaspect.rflux.rockets.model.Launch
import com.voidaspect.rflux.rockets.repository.AbstractInMemoryUUIDRepository
import org.springframework.stereotype.Repository

@Repository
class InMemoryLaunchRepository : LaunchRepository, AbstractInMemoryUUIDRepository<Launch>()