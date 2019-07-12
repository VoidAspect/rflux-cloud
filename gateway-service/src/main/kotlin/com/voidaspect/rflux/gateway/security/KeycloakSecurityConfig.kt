package com.voidaspect.rflux.gateway.security

import com.voidaspect.rflux.gateway.routes.RfluxProperties
import com.voidaspect.rflux.keycloak.KeycloakRolesConverter
import org.springframework.boot.actuate.autoconfigure.security.reactive.EndpointRequest
import org.springframework.boot.actuate.health.HealthEndpoint
import org.springframework.boot.actuate.info.InfoEndpoint
import org.springframework.cloud.gateway.actuate.GatewayControllerEndpoint
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Profile
import org.springframework.http.HttpStatus
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity
import org.springframework.security.config.web.server.ServerHttpSecurity
import org.springframework.security.oauth2.server.resource.authentication.ReactiveJwtAuthenticationConverterAdapter
import org.springframework.security.web.server.SecurityWebFilterChain
import org.springframework.security.web.server.context.NoOpServerSecurityContextRepository
import org.springframework.web.server.ResponseStatusException
import reactor.core.publisher.Mono

@EnableWebFluxSecurity
@Profile("keycloak")
class KeycloakSecurityConfig(private val rfluxProperties: RfluxProperties) {

    @Bean
    fun securityWebFilterChain(security: ServerHttpSecurity): SecurityWebFilterChain {

        security.authorizeExchange()
                // open basic actuator endpoints
                .matchers(EndpointRequest.to(HealthEndpoint::class.java, InfoEndpoint::class.java)).permitAll()
                // allow access to auth service
                .pathMatchers("/${rfluxProperties.authService!!.path}/**").permitAll()
                // restrict gateway actuator endpoint to users with actuator_routes role
                .matchers(EndpointRequest.to(GatewayControllerEndpoint::class.java)).hasRole("actuator_routes")
                // all other requests are to be performed by authenticated user
                .anyExchange().authenticated()

        // configure as Oauth2 resource server
        security.oauth2ResourceServer().jwt()
                .jwtAuthenticationConverter(jwtAuthConverter())

        // configure as a stateless service
        security.securityContextRepository(NoOpServerSecurityContextRepository.getInstance())
                .csrf().disable()
                .logout().disable()

        // configure exception handling - wrap all security-related errors to give them response body
        security.exceptionHandling()
                .accessDeniedHandler { _, e -> responseStatusError(HttpStatus.FORBIDDEN, e) }
                .authenticationEntryPoint { _, e -> responseStatusError(HttpStatus.UNAUTHORIZED, e) }

        return security.build()
    }

}

private fun jwtAuthConverter() = ReactiveJwtAuthenticationConverterAdapter(KeycloakRolesConverter())

private fun responseStatusError(status: HttpStatus, e: Exception): Mono<Void> = Mono
        .error(ResponseStatusException(status, e.message, e))
