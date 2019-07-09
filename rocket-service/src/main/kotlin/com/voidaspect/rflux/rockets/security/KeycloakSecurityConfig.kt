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

@Profile("keycloak")
@EnableWebFluxSecurity
class KeycloakSecurityConfig {

    @Bean
    fun securityWebFilterChain(security: ServerHttpSecurity): SecurityWebFilterChain {

        security.authorizeExchange()
                .matchers(EndpointRequest.to(HealthEndpoint::class.java)).permitAll()
                .pathMatchers(HttpMethod.POST, "/api/launch/**").hasRole("rockets_launch")
                .anyExchange().authenticated()
                .and()
                .oauth2ResourceServer().jwt()
                .jwtAuthenticationConverter(jwtAuthConverter())

        security.securityContextRepository(NoOpServerSecurityContextRepository.getInstance())
                .csrf().disable()
                .logout().disable()

        return security.build()
    }

    private fun jwtAuthConverter() = ReactiveJwtAuthenticationConverterAdapter(KeycloakRolesConverter())

}
