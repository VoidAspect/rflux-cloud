package com.voidaspect.rflux.rockets.logging

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

/**
 * Delegate for obtaining sl4j logger
 */
class Log : ReadOnlyProperty<Any, Logger> {

    private lateinit var log: Logger

    override fun getValue(thisRef: Any, property: KProperty<*>): Logger {
        if (!this::log.isInitialized) {
            log = LoggerFactory.getLogger(thisRef.javaClass)
        }
        return log
    }

}