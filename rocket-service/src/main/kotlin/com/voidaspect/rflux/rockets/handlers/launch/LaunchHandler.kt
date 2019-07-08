package com.voidaspect.rflux.rockets.handlers.launch

import com.voidaspect.rflux.rockets.exception.RocketServiceException
import com.voidaspect.rflux.rockets.exception.launch.LaunchFailedException
import com.voidaspect.rflux.rockets.exception.launch.LaunchRecordNotFoundException
import com.voidaspect.rflux.rockets.exception.rockets.RocketAlreadyLaunchedException
import com.voidaspect.rflux.rockets.exception.rockets.RocketNotFoundException
import com.voidaspect.rflux.rockets.exception.rockets.RocketNotReadyException
import com.voidaspect.rflux.rockets.handlers.toOkServerResponse
import com.voidaspect.rflux.rockets.logging.Log
import com.voidaspect.rflux.rockets.model.Launch
import com.voidaspect.rflux.rockets.model.LaunchId
import com.voidaspect.rflux.rockets.model.Rocket
import com.voidaspect.rflux.rockets.model.Status
import com.voidaspect.rflux.rockets.repository.launch.LaunchRepository
import com.voidaspect.rflux.rockets.repository.rockets.RocketsRepository
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.server.ServerRequest
import org.springframework.web.reactive.function.server.ServerResponse
import org.springframework.web.reactive.function.server.ServerResponse.created
import org.springframework.web.reactive.function.server.ServerResponse.ok
import org.springframework.web.reactive.function.server.bodyToMono
import org.springframework.web.reactive.function.server.bodyToServerSentEvents
import reactor.core.publisher.Mono
import reactor.core.publisher.switchIfEmpty
import java.net.URI

@Component
class LaunchHandler(
        private val launchRepository: LaunchRepository,
        private val rocketsRepository: RocketsRepository
) {

    private val log by Log()

    //region public API

    fun findAll(request: ServerRequest) = flux().toOkServerResponse()

    fun stream(request: ServerRequest) = flux().let { ok().bodyToServerSentEvents(it) }

    fun get(request: ServerRequest) = request.toLaunchId()
            .let { launchRepository[it].throwNotFoundIfEmpty(it) }
            .toOkServerResponse()

    fun launch(request: ServerRequest): Mono<ServerResponse> = request
            .bodyToMono<LaunchRocketsCommand>()
            .map(LaunchRocketsCommand::rocket).flatMap { rocketId ->
                rocketsRepository[rocketId].switchIfEmpty {
                    launchError("no rocket") { RocketNotFoundException(rocketId) }
                }.flatMap {
                    when (it.status) {
                        Status.LAUNCHED -> launchError("already launched") {
                            RocketAlreadyLaunchedException(rocketId)
                        }
                        Status.NOT_READY -> launchError("rocket not ready") {
                            RocketNotReadyException(rocketId)
                        }
                        Status.READY -> rocketsRepository
                                .update(Rocket.Existing(it.id, it.warhead, it.target, Status.LAUNCHED))
                    }
                }
            }
            .doOnSuccess {
                log.info("Launching rocket {}. Warhead: {}, target: (latitude: {}, longitude: {})",
                        it.id, it.status, it.target.latitude, it.target.longitude)
            }
            .flatMap { launchRepository.add(Launch.New(it)) }
            .flatMap { created(URI.create("/api/launch/${it.id}")).syncBody(it) }

    //endregion

    //region retrieve data

    private fun flux() = launchRepository.findAll()

    //endregion

    //region helpers & extensions

    private fun ServerRequest.toLaunchId(): LaunchId = this.pathVariable("id").let { LaunchId.fromString(it) }

    private fun <T> Mono<T>.throwNotFoundIfEmpty(launchId: LaunchId) = this
            .switchIfEmpty(Mono.error { LaunchRecordNotFoundException(launchId) })

    private inline fun <reified T> launchError(
            message: String,
            crossinline cause: () -> RocketServiceException
    ): Mono<T> = Mono.error { LaunchFailedException("Launch failed: $message", cause()) }

    //endregion

}