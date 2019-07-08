package com.voidaspect.rflux.rockets.handlers

import org.reactivestreams.Publisher
import org.springframework.web.reactive.function.server.ServerResponse.ok
import org.springframework.web.reactive.function.server.body

internal inline fun <reified T : Any> Publisher<T>.toOkServerResponse() = ok().body(this)
