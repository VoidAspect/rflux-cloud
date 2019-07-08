package com.voidaspect.rflux.rockets

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication


@SpringBootApplication
class RocketServiceApplication

fun main(args: Array<String>) {
    runApplication<RocketServiceApplication>(*args)
}