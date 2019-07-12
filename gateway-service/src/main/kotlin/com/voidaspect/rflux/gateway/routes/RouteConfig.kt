package com.voidaspect.rflux.gateway.routes

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.cloud.gateway.filter.GlobalFilter
import org.springframework.cloud.gateway.route.RouteLocator
import org.springframework.cloud.gateway.route.builder.PredicateSpec
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpHeaders
import reactor.core.publisher.Mono

@Configuration
@EnableConfigurationProperties(RfluxProperties::class)
class RouteConfig(private val rfluxProperties: RfluxProperties) {

    private val log: Logger = LoggerFactory.getLogger(javaClass)

    @Bean
    fun loggingFilter() = GlobalFilter { exchange, chain ->
        // Print Request
        log.info("Method: {}; URI: {}",
                exchange.request.method,
                exchange.request.uri
        )

        chain.filter(exchange).then(Mono.fromRunnable {
            // Print Response Code
            log.info("Status Code: {}; Headers: {}", exchange.response.statusCode, exchange.response.headers)
        })
    }

    @Bean
    fun routes(builder: RouteLocatorBuilder): RouteLocator = builder.routes()
            .service(rfluxProperties.rocketService)
            .service(rfluxProperties.authService)
            .build()

    private fun RouteLocatorBuilder.Builder.service(
            service: RfluxProperties.Service?
    ) = service?.let {
        this.route(service.id) { it.rewriteServicePaths(service.path).uri(service.uri) }
    } ?: this

    private fun PredicateSpec.rewriteServicePaths(path: String) = this
            .path("/$path/**")
            .filters { gatewayFilterSpec ->
                gatewayFilterSpec
                        // rewrite e.g. /some-service/api -> /api
                        .rewritePath("/$path", "")
                        // rewrite e.g. /api/resource/id -> /some-service/api/resource/id
                        .rewriteResponseHeader(HttpHeaders.LOCATION, "^/", "/$path/")
            }

}