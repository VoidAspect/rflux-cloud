package com.voidaspect.rflux.rockets

import com.voidaspect.rflux.rockets.logging.Log
import com.voidaspect.rflux.rockets.repository.launch.LaunchRepository
import com.voidaspect.rflux.rockets.repository.rockets.RocketsRepository
import org.springframework.stereotype.Component

@Component
internal class RocketServiceStateCleaner(
        private val launchRepository: LaunchRepository,
        private val rocketRepository: RocketsRepository
) {

    private val log by Log()

    internal fun clean() {
        log.info("Purging stored data for tests")
        rocketRepository.purge().block()
        launchRepository.purge().block()
        log.info("App state has been cleaned")
    }
}