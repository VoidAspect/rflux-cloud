package com.voidaspect.rflux.rockets.security

import com.voidaspect.rflux.keycloak.KeycloakRolesConverter
import org.springframework.boot.actuate.autoconfigure.security.reactive.EndpointRequest
import org.springframework.boot.actuate.health.HealthEndpoint
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Profile
import org.springframework.http.HttpMethod
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity
import org.springframework.security.config.web.server.ServerHttpSecurity
import org.springframework.security.oauth2.server.resource.authentication.ReactiveJwtAuthenticationConverterAdapter
import org.springframework.security.web.server.SecurityWebFilterChain
import org.springframework.security.web.server.context.NoOpServerSecurityContextRepository

@Profile("basic-auth")
@EnableWebFluxSecurity
class HttpBasicSecurityConfig {

    @Bean
    fun securityWebFilterChain(security: ServerHttpSecurity): SecurityWebFilterChain {

        security.appDefault()
                .anyExchange().authenticated()
                .and()
                .httpBasic()

        return security.build()
    }

}

@Profile("keycloak")
@EnableWebFluxSecurity
class KeycloakSecurityConfig {

    @Bean
    fun securityWebFilterChain(security: ServerHttpSecurity): SecurityWebFilterChain {

        security.appDefault()
                .pathMatchers(HttpMethod.POST, "/api/launch/**")
                .hasRole("rockets_launch")
                .anyExchange().authenticated()
                .and()
                .oauth2ResourceServer().jwt()
                .jwtAuthenticationConverter(jwtAuthConverter())

        return security.build()
    }

    private fun jwtAuthConverter() = ReactiveJwtAuthenticationConverterAdapter(KeycloakRolesConverter())

}

private fun ServerHttpSecurity.appDefault() = this
        .securityContextRepository(NoOpServerSecurityContextRepository.getInstance())
        .csrf().disable()
        .logout().disable()
        .authorizeExchange()
        .matchers(EndpointRequest.to(HealthEndpoint::class.java)).permitAll()

