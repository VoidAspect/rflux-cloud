package com.voidaspect.rflux.rockets.handlers.launch

import com.voidaspect.rflux.rockets.exception.RocketServiceException
import com.voidaspect.rflux.rockets.exception.launch.LaunchFailedException
import com.voidaspect.rflux.rockets.exception.launch.LaunchRecordNotFoundException
import com.voidaspect.rflux.rockets.exception.rockets.RocketAlreadyLaunchedException
import com.voidaspect.rflux.rockets.exception.rockets.RocketNotFoundException
import com.voidaspect.rflux.rockets.exception.rockets.RocketNotReadyException
import com.voidaspect.rflux.rockets.handlers.toOkServerResponse
import com.voidaspect.rflux.rockets.logging.Log
import com.voidaspect.rflux.rockets.model.*
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

    fun findAll(request: ServerRequest) = flux()
            .map { it.toResponse() }
            .toOkServerResponse()

    fun stream(request: ServerRequest) = flux()
            .map { it.toResponse() }
            .let { ok().bodyToServerSentEvents(it) }

    fun get(request: ServerRequest) = request.toLaunchId()
            .let { launchRepository[it].throwNotFoundIfEmpty(it) }
            .map { it.toResponse() }
            .toOkServerResponse()

    fun launch(request: ServerRequest): Mono<ServerResponse> = request
            .bodyToMono<LaunchRocketsCommand>()
            .map(LaunchRocketsCommand::rocket).flatMap { rocketId ->
                rocketsRepository[rocketId].switchIfEmpty {
                    launchError("no rocket") { RocketNotFoundException(rocketId) }
                }.flatMap {
                    when (it.value.status) {
                        Status.LAUNCHED -> launchError("already launched") {
                            RocketAlreadyLaunchedException(rocketId)
                        }
                        Status.NOT_READY -> launchError("rocket not ready") {
                            RocketNotReadyException(rocketId)
                        }
                        Status.READY -> rocketsRepository.update(it.id, it.value.copy(status = Status.LAUNCHED))
                    }
                }
            }
            .doOnSuccess {
                if (log.isInfoEnabled) {
                    val rocket = it.value
                    val target = rocket.target
                    log.info("Launching rocket {}. Warhead: {}, target: (latitude: {}, longitude: {})",
                            it.id, rocket.status, target.latitude, target.longitude)
                }
            }
            .flatMap { launchRepository.add(Launch(it.value)) }
            .flatMap { created(URI.create("/api/launch/${it.id}")).syncBody(it.toResponse()) }

    //endregion

    //region retrieve data

    private fun flux() = launchRepository.findAll()

    //endregion

}

//region helpers & extensions

private fun ServerRequest.toLaunchId(): LaunchId = LaunchId.fromString(this.pathVariable("id"))

private fun <T> Mono<T>.throwNotFoundIfEmpty(launchId: LaunchId) = this
        .switchIfEmpty(Mono.error { LaunchRecordNotFoundException(launchId) })

private inline fun <reified T> launchError(
        message: String,
        crossinline cause: () -> RocketServiceException
): Mono<T> = Mono.error { LaunchFailedException("Launch failed: $message", cause()) }

private fun Stored<Launch, LaunchId>.toResponse() = LaunchResponse(this.id, this.value)

//endregion