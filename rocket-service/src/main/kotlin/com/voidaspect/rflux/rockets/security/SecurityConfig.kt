package com.voidaspect.rflux.rockets.security

import com.voidaspect.rflux.keycloak.KeycloakRolesConverter
import com.voidaspect.rflux.rockets.exception.RocketServiceException
import org.springframework.boot.actuate.autoconfigure.security.reactive.EndpointRequest
import org.springframework.boot.actuate.health.HealthEndpoint
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Profile
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.HttpStatus.FORBIDDEN
import org.springframework.http.HttpStatus.UNAUTHORIZED
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity
import org.springframework.security.config.web.server.ServerHttpSecurity
import org.springframework.security.oauth2.server.resource.authentication.ReactiveJwtAuthenticationConverterAdapter
import org.springframework.security.web.server.SecurityWebFilterChain
import org.springframework.security.web.server.context.NoOpServerSecurityContextRepository
import reactor.core.publisher.Mono

@EnableWebFluxSecurity
class SecurityConfig {

    @Bean
    @Profile("basic-auth")
    fun basicAuthSecurity(security: ServerHttpSecurity): SecurityWebFilterChain {

        security.appDefault().httpBasic()

        return security.build()
    }

    @Bean
    @Profile("keycloak")
    fun oauth2ResourceServerSecurity(security: ServerHttpSecurity): SecurityWebFilterChain {

        security.appDefault()
                .oauth2ResourceServer().jwt()
                .jwtAuthenticationConverter(jwtAuthConverter())

        return security.build()
    }

}

private fun ServerHttpSecurity.appDefault(): ServerHttpSecurity = this
        .securityContextRepository(NoOpServerSecurityContextRepository.getInstance())
        .csrf().disable()
        .logout().disable()
        .authorizeExchange()
        .matchers(EndpointRequest.to(HealthEndpoint::class.java)).permitAll()
        .pathMatchers(HttpMethod.POST, "/api/launch/**")
        .hasRole("rockets_launch")
        .anyExchange().authenticated()
        .and()
        .exceptionHandling()
        .accessDeniedHandler { _, e -> rocketServiceError(FORBIDDEN, e) }
        .authenticationEntryPoint { _, e -> rocketServiceError(UNAUTHORIZED, e) }
        .and()

private fun rocketServiceError(status: HttpStatus, e: Exception): Mono<Void> = Mono
        .error(RocketServiceException(status, e))

private fun jwtAuthConverter() = ReactiveJwtAuthenticationConverterAdapter(KeycloakRolesConverter())


