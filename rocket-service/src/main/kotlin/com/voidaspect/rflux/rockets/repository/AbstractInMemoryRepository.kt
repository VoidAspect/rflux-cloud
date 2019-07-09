package com.voidaspect.rflux.rockets.repository

import com.voidaspect.rflux.rockets.model.Stored
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.util.concurrent.ConcurrentHashMap

abstract class AbstractInMemoryRepository<T, ID> : RepositoryOperations<T, ID> {

    private val store: MutableMap<ID, T> = ConcurrentHashMap()

    abstract fun initialize(entity: T): Stored<T, ID>

    override fun update(id: ID, entity: T): Mono<Stored<T, ID>> = if (store.containsKey(id)) {
        store[id] = entity
        Mono.just(Stored(id, entity))
    } else {
        Mono.error { NoSuchElementException("Can't update stored entity, id $id not found") }
    }

    override fun add(entity: T): Mono<Stored<T, ID>> = initialize(entity).let {
        val key = it.id
        if(store.containsKey(key)) {
            Mono.error { IllegalStateException("Can't store new entity: duplicate key $key") }
        } else {
            store[key] = it.value
            Mono.just(it)
        }
    }

    override operator fun get(id: ID): Mono<Stored<T, ID>> = Mono.justOrEmpty(store[id].toEntity(id))

    override fun remove(id: ID): Mono<Stored<T, ID>> = Mono.justOrEmpty(store.remove(id).toEntity(id))

    override fun findAll(): Flux<Stored<T, ID>> = Flux
            .fromIterable(store.entries)
            .map { Stored(it.key, it.value) }

    override fun contains(id: ID): Mono<Boolean> = Mono.just(store.contains(id))

    private fun T?.toEntity(id: ID): Stored<T, ID>? = this?.let { Stored(id, it) }

}