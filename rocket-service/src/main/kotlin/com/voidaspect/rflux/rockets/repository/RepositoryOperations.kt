package com.voidaspect.rflux.rockets.repository

import com.voidaspect.rflux.rockets.model.Identity
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

interface RepositoryOperations
<TYPE, EXISTING : Identity<TYPE, ID>, NEW : TYPE, ID> {

    fun findAll(): Flux<EXISTING>

    fun update(entity: EXISTING): Mono<EXISTING>

    fun add(entity: NEW): Mono<EXISTING>

    operator fun get(id: ID): Mono<EXISTING>

    fun remove(id: ID): Mono<EXISTING>

    fun contains(id: ID): Mono<Boolean>

}