package com.voidaspect.rflux.rockets.security

import org.springframework.boot.actuate.autoconfigure.security.reactive.EndpointRequest
import org.springframework.boot.actuate.health.HealthEndpoint
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Profile
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity
import org.springframework.security.config.web.server.ServerHttpSecurity
import org.springframework.security.web.server.SecurityWebFilterChain
import org.springframework.security.web.server.context.NoOpServerSecurityContextRepository

@Profile("basic-auth")
@EnableWebFluxSecurity
class HttpBasicSecurityConfig {

    @Bean
    fun securityWebFilterChain(security: ServerHttpSecurity): SecurityWebFilterChain {

        security.authorizeExchange()
                .matchers(EndpointRequest.to(HealthEndpoint::class.java)).permitAll()
                .anyExchange().authenticated()
                .and()
                .httpBasic()

        security.securityContextRepository(NoOpServerSecurityContextRepository.getInstance())
                .csrf().disable()
                .logout().disable()

        return security.build()
    }

}