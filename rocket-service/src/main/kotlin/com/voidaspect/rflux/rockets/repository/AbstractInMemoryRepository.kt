package com.voidaspect.rflux.rockets.repository

import com.voidaspect.rflux.rockets.model.Identity
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.util.concurrent.ConcurrentHashMap

abstract class AbstractInMemoryRepository<T, E : Identity<T, ID>, N : T, ID> : RepositoryOperations<T, E, N, ID> {

    private val store: MutableMap<ID, E> = ConcurrentHashMap()

    abstract fun initialize(entity: N): E

    override fun update(entity: E): Mono<E> = if (store.containsKey(entity.id)) {
        store[entity.id] = entity
        Mono.just(entity)
    } else {
        Mono.error { NoSuchElementException("Can't update stored entity, id ${entity.id} not found") }
    }

    override fun add(entity: N): Mono<E> = initialize(entity)
            .also { store[it.id] = it }
            .let { Mono.just(it) }

    override operator fun get(id: ID): Mono<E> = Mono.justOrEmpty(store[id])

    override fun remove(id: ID): Mono<E> = Mono.justOrEmpty(store.remove(id))

    override fun findAll(): Flux<E> = Flux.fromIterable(store.values)

    override fun contains(id: ID): Mono<Boolean> = Mono.just(store.contains(id))
}