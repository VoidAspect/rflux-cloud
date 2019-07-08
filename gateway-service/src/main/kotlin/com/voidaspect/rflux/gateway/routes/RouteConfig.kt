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
    fun routes(builder: RouteLocatorBuilder): RouteLocator = builder.routes()
            .service(rfluxProperties.rocketService)
            .build()

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

    private fun RouteLocatorBuilder.Builder.service(service: RfluxProperties.Service) = this
            .route(service.basePath) { it.rewriteServicePaths(service.basePath).uri(service.url) }

    private fun PredicateSpec.rewriteServicePaths(basePath: String) = this
            .path("/$basePath/**")
            .filters { gatewayFilterSpec ->
                gatewayFilterSpec
                        // rewrite e.g. /some-service/api -> /api
                        .rewritePath("/$basePath/", "/")
                        // rewrite e.g. /api/resource/id -> /some-service/api/resource/id
                        .rewriteResponseHeader(HttpHeaders.LOCATION, "^/", "/$basePath/")
            }

}