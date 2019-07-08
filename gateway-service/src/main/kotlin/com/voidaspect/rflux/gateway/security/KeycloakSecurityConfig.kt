package com.voidaspect.rflux.gateway.security

import com.voidaspect.rflux.keycloak.KeycloakRolesConverter
import org.springframework.boot.actuate.autoconfigure.security.reactive.EndpointRequest
import org.springframework.boot.actuate.health.HealthEndpoint
import org.springframework.boot.actuate.info.InfoEndpoint
import org.springframework.cloud.gateway.actuate.GatewayControllerEndpoint
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Profile
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity
import org.springframework.security.config.web.server.ServerHttpSecurity
import org.springframework.security.oauth2.server.resource.authentication.ReactiveJwtAuthenticationConverterAdapter
import org.springframework.security.web.server.SecurityWebFilterChain

@EnableWebFluxSecurity
@Profile("keycloak")
class KeycloakSecurityConfig {

    @Bean
    fun securityWebFilterChain(security: ServerHttpSecurity): SecurityWebFilterChain {

        security.authorizeExchange()
                .matchers(EndpointRequest.to(HealthEndpoint::class.java, InfoEndpoint::class.java))
                .permitAll()
                .matchers(EndpointRequest.to(GatewayControllerEndpoint::class.java))
                .hasRole("actuator_routes")
                .anyExchange().authenticated()
                .and()
                .oauth2ResourceServer().jwt()
                .jwtAuthenticationConverter(jwtAuthConverter())

        return security.build()
    }

    private fun jwtAuthConverter() = ReactiveJwtAuthenticationConverterAdapter(KeycloakRolesConverter())

}