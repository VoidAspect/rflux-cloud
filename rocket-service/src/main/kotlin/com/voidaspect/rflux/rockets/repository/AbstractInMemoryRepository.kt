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
        contains(key).map { present ->
            if (present) {
                throw IllegalStateException("Can't store new entity: duplicate key $key")
            } else {
                store[key] = it.value
                it
            }
        }
    }

    override operator fun get(id: ID): Mono<Stored<T, ID>> = store[id].mono(id)

    override fun remove(id: ID): Mono<Stored<T, ID>> = store.remove(id).mono(id)

    override fun findAll(): Flux<Stored<T, ID>> = Flux
            .fromIterable(store.entries)
            .map { Stored(it.key, it.value) }

    override fun contains(id: ID): Mono<Boolean> {
        val contains = store.contains(id)
        return Mono.just(contains)
    }

    private fun T?.mono(id: ID): Mono<Stored<T, ID>> = Mono.justOrEmpty(this?.stored(id))

    private fun T.stored(id: ID): Stored<T, ID> = Stored(id, this)

    override fun purge(): Mono<Void> = Mono.fromRunnable { store.clear() }

}