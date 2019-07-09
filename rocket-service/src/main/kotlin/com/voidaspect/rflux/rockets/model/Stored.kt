package com.voidaspect.rflux.rockets.model

/**
 * Generic stored unit with identity
 */
data class Stored<T, ID>(
        val id: ID,
        val value: T
)