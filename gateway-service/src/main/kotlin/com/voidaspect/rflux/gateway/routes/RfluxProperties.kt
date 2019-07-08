package com.voidaspect.rflux.gateway.routes

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties("rflux")
class RfluxProperties {

    var rocketService = Service()

    class Service {
        lateinit var url: String
        lateinit var basePath: String
    }

}