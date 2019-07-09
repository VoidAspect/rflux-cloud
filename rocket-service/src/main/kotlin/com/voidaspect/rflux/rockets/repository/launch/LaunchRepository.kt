package com.voidaspect.rflux.rockets.repository.launch

import com.voidaspect.rflux.rockets.model.Launch
import com.voidaspect.rflux.rockets.model.LaunchId
import com.voidaspect.rflux.rockets.repository.RepositoryOperations

interface LaunchRepository : RepositoryOperations<Launch, LaunchId>