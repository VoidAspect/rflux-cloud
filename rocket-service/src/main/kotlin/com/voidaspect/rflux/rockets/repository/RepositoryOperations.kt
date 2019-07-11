package com.voidaspect.rflux.rockets.repository

import com.voidaspect.rflux.rockets.model.Stored
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

interface RepositoryOperations<T, ID> {

    fun findAll(): Flux<Stored<T, ID>>

    operator fun get(id: ID): Mono<Stored<T, ID>>

    fun contains(id: ID): Mono<Boolean>

    fun update(id: ID, entity: T): Mono<Stored<T, ID>>

    fun add(entity: T): Mono<Stored<T, ID>>

    fun remove(id: ID): Mono<Stored<T, ID>>

    fun purge(): Mono<Void>

}