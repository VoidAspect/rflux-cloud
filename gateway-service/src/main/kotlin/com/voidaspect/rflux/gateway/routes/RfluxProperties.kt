package com.voidaspect.rflux.gateway.routes

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties("rflux")
class RfluxProperties {

    var rocketService = Service()

    var authService = Service()

    class Service {
        lateinit var url: String
        lateinit var id: String
    }

}