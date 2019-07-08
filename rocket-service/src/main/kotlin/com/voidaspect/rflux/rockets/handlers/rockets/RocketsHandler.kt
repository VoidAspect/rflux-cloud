package com.voidaspect.rflux.rockets.handlers.rockets

import com.voidaspect.rflux.rockets.exception.rockets.CannotChangeStatusToLaunchedException
import com.voidaspect.rflux.rockets.exception.rockets.RocketNotFoundException
import com.voidaspect.rflux.rockets.handlers.toOkServerResponse
import com.voidaspect.rflux.rockets.logging.Log
import com.voidaspect.rflux.rockets.model.Rocket
import com.voidaspect.rflux.rockets.model.RocketId
import com.voidaspect.rflux.rockets.model.Status
import com.voidaspect.rflux.rockets.model.TargetCoordinates
import com.voidaspect.rflux.rockets.repository.rockets.RocketsRepository
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.server.ServerRequest
import org.springframework.web.reactive.function.server.ServerResponse
import org.springframework.web.reactive.function.server.ServerResponse.created
import org.springframework.web.reactive.function.server.ServerResponse.ok
import org.springframework.web.reactive.function.server.bodyToMono
import org.springframework.web.reactive.function.server.bodyToServerSentEvents
import reactor.core.publisher.Mono
import java.net.URI

@Component
class RocketsHandler(private val rocketsRepository: RocketsRepository) {

    private val log by Log()

    //region public API

    fun findAll(request: ServerRequest) = flux().toOkServerResponse()

    fun stream(request: ServerRequest) = flux().let { ok().bodyToServerSentEvents(it) }

    fun get(request: ServerRequest) = mono(request.toRocketId()).toOkServerResponse()

    fun add(request: ServerRequest): Mono<ServerResponse> = request.bodyToMono<AddRocketCommand>()
            .doOnSuccess {
                log.info("Adding new rocket. Warhead: {}, target: (latitude: {}, longitude: {})",
                        it.warhead, it.target.latitude, it.target.longitude)
            }
            .flatMap { rocketsRepository.add(Rocket.New(it.warhead, it.target)) }
            .flatMap { created(URI.create("/api/rockets/${it.id}")).syncBody(it) }

    fun update(request: ServerRequest) = request.toRocketId().let { rocketId ->

        request.bodyToMono<UpdateRocketCommand>()
                .flatMapValidateStatusChange { it.status }
                .doOnSuccess {
                    log.info("Updating rocket {}. Warhead: {}, target: (latitude: {}, longitude: {})",
                            rocketId, it.warhead, it.target.latitude, it.target.longitude)
                }
                .filterWhen { rocketsRepository.contains(rocketId) }
                .throwNotFoundIfEmpty(rocketId)
                .map { Rocket.Existing(rocketId, it.warhead, it.target, it.status) }

    }.toUpdateResponse()

    fun changeStatus(request: ServerRequest) = request.toRocketId().let { rocketId ->

        request.bodyToMono<ChangeStatusCommand>()
                .doOnSuccess { log.info("Changing status of rocket {} to {}", rocketId, it.status) }
                .flatMapValidateStatusChange { it.status }
                .flatMap { command ->
                    mono(rocketId).map { Rocket.Existing(it.id, it.warhead, it.target, command.status) }
                }
    }.toUpdateResponse()

    fun changeWarhead(request: ServerRequest) = request.toRocketId().let { rocketId ->

        request.bodyToMono<ChangeWarheadCommand>()
                .doOnSuccess { log.info("Changing warhead of rocket {} to {}", rocketId, it.warhead) }
                .flatMap { command ->
                    mono(rocketId).map { Rocket.Existing(it.id, command.warhead, it.target, it.status) }
                }
    }.toUpdateResponse()

    fun changeTarget(request: ServerRequest) = request.toRocketId().let { rocketId ->

        request.bodyToMono<ChangeTargetCommand>()
                .doOnSuccess {
                    log.info("Changing target of rocket {} to (latitude: {}, longitude: {})",
                            rocketId, it.latitude, it.longitude)
                }
                .flatMap { command ->
                    mono(rocketId).map {
                        Rocket.Existing(
                                it.id,
                                it.warhead,
                                TargetCoordinates(command.latitude, command.longitude),
                                it.status)
                    }
                }
    }.toUpdateResponse()

    fun remove(request: ServerRequest) = request.toRocketId()
            .let { rocketsRepository.remove(it).throwNotFoundIfEmpty(it) }
            .toOkServerResponse()

    //endregion

    //region retrieve data

    private fun mono(rocketId: RocketId) = rocketsRepository[rocketId].throwNotFoundIfEmpty(rocketId)

    private fun flux() = rocketsRepository.findAll()

    //endregion

    //region extensions & helpers

    private fun ServerRequest.toRocketId(): RocketId = RocketId.fromString(this.pathVariable("id"))

    private fun Mono<Rocket.Existing>.toUpdateResponse() = this
            .flatMap(rocketsRepository::update)
            .toOkServerResponse()

    private fun <T> Mono<T>.throwNotFoundIfEmpty(rocketId: RocketId) = this
            .switchIfEmpty(Mono.error { RocketNotFoundException(rocketId) })

    private inline fun <reified T> Mono<T>.flatMapValidateStatusChange(
            crossinline status: (T) -> Status
    ) = this.flatMap {
        when (status(it)) {
            Status.LAUNCHED -> Mono.error { CannotChangeStatusToLaunchedException() }
            else -> Mono.just(it)
        }
    }

    //endregion

}
