package com.voidaspect.rflux.rockets.handlers.rockets

import com.voidaspect.rflux.rockets.exception.rockets.CannotChangeStatusToLaunchedException
import com.voidaspect.rflux.rockets.exception.rockets.RocketNotFoundException
import com.voidaspect.rflux.rockets.handlers.toOkServerResponse
import com.voidaspect.rflux.rockets.logging.Log
import com.voidaspect.rflux.rockets.model.*
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

    fun findAll(request: ServerRequest) = flux()
            .map { it.toResponse() }
            .toOkServerResponse()

    fun stream(request: ServerRequest) = flux()
            .map { it.toResponse() }
            .let { ok().bodyToServerSentEvents(it) }

    fun get(request: ServerRequest) = mono(request.toRocketId())
            .map { it.toResponse() }
            .toOkServerResponse()

    fun add(request: ServerRequest): Mono<ServerResponse> = request.bodyToMono<AddRocketCommand>()
            .doOnSuccess {
                log.info("Adding new rocket. Warhead: {}, target: (latitude: {}, longitude: {})",
                        it.warhead, it.target.latitude, it.target.longitude)
            }
            .flatMap { rocketsRepository.add(Rocket(it.warhead, it.target)) }
            .flatMap { created(URI.create("/api/rockets/${it.id}")).syncBody(it.toResponse()) }

    fun update(request: ServerRequest) = request.toRocketId().let { rocketId ->

        request.bodyToMono<UpdateRocketCommand>().doOnSuccess {
            log.info("Updating rocket {}. Warhead: {}, target: (latitude: {}, longitude: {})",
                    rocketId, it.warhead, it.target.latitude, it.target.longitude)
        }.flatMapValidateStatusChange { it.status }.filterWhen {
            rocketsRepository.contains(rocketId)
        }.throwNotFoundIfEmpty(rocketId).map {
            Rocket(it.warhead, it.target, it.status)
        }.toUpdateResponse(rocketId)
    }

    fun changeStatus(request: ServerRequest) = request.toRocketId().let { rocketId ->

        request.bodyToMono<ChangeStatusCommand>().doOnSuccess {
            log.info("Changing status of rocket {} to {}", rocketId, it.status)
        }.flatMapValidateStatusChange { it.status }.flatMap { command ->
            mono(rocketId).map { it.value.copy(status = command.status) }
        }.toUpdateResponse(rocketId)
    }

    fun changeWarhead(request: ServerRequest) = request.toRocketId().let { rocketId ->

        request.bodyToMono<ChangeWarheadCommand>().doOnSuccess {
            log.info("Changing warhead of rocket {} to {}", rocketId, it.warhead)
        }.flatMap { command ->
            mono(rocketId).map { it.value.copy(warhead = command.warhead) }
        }.toUpdateResponse(rocketId)
    }

    fun changeTarget(request: ServerRequest) = request.toRocketId().let { rocketId ->

        request.bodyToMono<ChangeTargetCommand>().doOnSuccess {
            log.info("Changing target of rocket {} to (latitude: {}, longitude: {})",
                    rocketId, it.latitude, it.longitude)
        }.flatMap { command ->
            mono(rocketId).map { it.value.copy(target = TargetCoordinates(command.latitude, command.longitude)) }
        }.toUpdateResponse(rocketId)
    }

    fun remove(request: ServerRequest) = request.toRocketId()
            .let { rocketsRepository.remove(it).throwNotFoundIfEmpty(it) }
            .map { it.toResponse() }
            .toOkServerResponse()

    //endregion

    //region retrieve data

    private fun mono(rocketId: RocketId) = rocketsRepository[rocketId].throwNotFoundIfEmpty(rocketId)

    private fun flux() = rocketsRepository.findAll()

    //endregion

    private fun Mono<Rocket>.toUpdateResponse(id: RocketId) = this
            .flatMap { rocketsRepository.update(id, it) }
            .map { it.toResponse() }
            .toOkServerResponse()
}

private fun ServerRequest.toRocketId(): RocketId = RocketId.fromString(this.pathVariable("id"))

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

private fun Stored<Rocket, RocketId>.toResponse() = RocketResponse(this.id, this.value)
