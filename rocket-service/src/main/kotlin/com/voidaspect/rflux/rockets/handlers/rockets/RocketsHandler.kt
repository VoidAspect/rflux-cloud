package com.voidaspect.rflux.rockets.handlers.rockets

import com.voidaspect.rflux.rockets.exception.rockets.CannotChangeStatusToLaunchedException
import com.voidaspect.rflux.rockets.exception.rockets.RocketAlreadyLaunchedException
import com.voidaspect.rflux.rockets.exception.rockets.RocketNotFoundException
import com.voidaspect.rflux.rockets.handlers.toOkServerResponse
import com.voidaspect.rflux.rockets.logging.Log
import com.voidaspect.rflux.rockets.model.*
import com.voidaspect.rflux.rockets.repository.rockets.RocketsRepository
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.server.ServerRequest
import org.springframework.web.reactive.function.server.ServerResponse
import org.springframework.web.reactive.function.server.ServerResponse.*
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

    fun get(request: ServerRequest) = mono(request.toRocketId()).toOkRocketResponse()

    fun add(request: ServerRequest): Mono<ServerResponse> = request.bodyToMono<AddRocketCommand>()
            .doOnSuccess {
                log.info("Adding new rocket. Warhead: {}, target: (latitude: {}, longitude: {})",
                        it.warhead, it.target.latitude, it.target.longitude)
            }
            .flatMap { rocketsRepository.add(Rocket(it.warhead, it.target)) }
            .flatMap { created(URI.create("/api/rockets/${it.id}")).syncBody(it.toResponse()) }

    fun update(request: ServerRequest) = request.toRocketId().let { rocketId ->
        val validatedRequest = request
                .bodyToMono<UpdateRocketCommand>()
                .validateStatusChange { it.status }
        Mono.zip(validatedRequest, notYetLaunched(rocketId)) { command, _ ->
            Rocket(command.warhead, command.target, command.status)
        }.doOnSuccess {
            log.info("Updating rocket {}. Warhead: {}, target: (latitude: {}, longitude: {})",
                    rocketId, it.warhead, it.target.latitude, it.target.longitude)
        }.toUpdateResponse(rocketId)
    }

    fun changeStatus(request: ServerRequest) = request.toRocketId().let { rocketId ->
        val validatedRequest = request
                .bodyToMono<ChangeStatusCommand>()
                .validateStatusChange { it.status }
        Mono.zip(validatedRequest, notYetLaunched(rocketId)) { command, stored ->
            stored.value.copy(status = command.status)
        }.doOnSuccess {
            log.info("Changing status of rocket {} to {}", rocketId, it.status)
        }.toUpdateResponse(rocketId)
    }

    fun changeWarhead(request: ServerRequest) = request.toRocketId().let { rocketId ->
        val requestBody = request.bodyToMono<ChangeWarheadCommand>()
        requestBody.zipWith(notYetLaunched(rocketId)) { command, stored ->
            stored.value.copy(warhead = command.warhead)
        }.doOnSuccess {
            log.info("Changing warhead of rocket {} to {}", rocketId, it.warhead)
        }.toUpdateResponse(rocketId)
    }

    fun changeTarget(request: ServerRequest) = request.toRocketId().let { rocketId ->
        val requestBody = request.bodyToMono<ChangeTargetCommand>()
        requestBody.zipWith(notYetLaunched(rocketId)) { command, stored ->
            stored.value.copy(target = TargetCoordinates(command.latitude, command.longitude))
        }.doOnSuccess {
            log.info("Changing target of rocket {} to (latitude: {}, longitude: {})",
                    rocketId, it.target.latitude, it.target.longitude)
        }.toUpdateResponse(rocketId)
    }

    fun remove(request: ServerRequest) = request.toRocketId()
            .let { rocketsRepository.remove(it).throwNotFoundIfEmpty(it) }
            .toOkRocketResponse()

    //endregion

    //region retrieve data

    private fun mono(rocketId: RocketId) = rocketsRepository[rocketId].throwNotFoundIfEmpty(rocketId)

    private fun notYetLaunched(rocketId: RocketId) = mono(rocketId).doOnNext {
        if (it.value.status == Status.LAUNCHED) throw RocketAlreadyLaunchedException(it.id)
    }

    private fun flux() = rocketsRepository.findAll()

    //endregion

    private fun Mono<Rocket>.toUpdateResponse(id: RocketId) = this
            .flatMap { rocketsRepository.update(id, it) }
            .toOkRocketResponse()
}

private fun ServerRequest.toRocketId(): RocketId = RocketId.fromString(this.pathVariable("id"))

private fun <T> Mono<T>.throwNotFoundIfEmpty(rocketId: RocketId) = this
        .switchIfEmpty(Mono.error { RocketNotFoundException(rocketId) })

private inline fun <reified T> Mono<T>.validateStatusChange(
        crossinline status: (T) -> Status
) = this.doOnNext {
    if (status(it) == Status.LAUNCHED) throw CannotChangeStatusToLaunchedException()
}

private fun Stored<Rocket, RocketId>.toResponse() = RocketResponse(this.id, this.value)

private fun Mono<Stored<Rocket, RocketId>>.toOkRocketResponse() = this
        .map { it.toResponse() }
        .toOkServerResponse()
