package com.voidaspect.rflux.gateway.routes

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties("rflux")
class RfluxProperties {

    var rocketService: Service? = null

    var authService: Service? = null

    class Service {
        lateinit var uri: String
        lateinit var id: String
        lateinit var path: String
    }

}