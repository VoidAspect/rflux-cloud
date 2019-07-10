package com.voidaspect.rflux.keycloak

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll
import org.springframework.security.authentication.AbstractAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.oauth2.jwt.Jwt
import java.time.Instant

internal class KeycloakRolesConverterTest {

    private val converter = KeycloakRolesConverter()

    @Test
    fun `should extract scopes`() {
        val authToken = convert(mapOf("scope" to "email profile"))
        assertAuthorities(authToken, "SCOPE_email", "SCOPE_profile")
    }

    @Test
    fun `should extract empty authorities`() {
        assertAuthorities(convert(mapOf("preferred_username" to "rflux")))
        assertAuthorities(convert(mapOf(
                "realm_access" to emptyMap<String, Any?>(),
                "azp" to null,
                "resource_access" to emptyMap<String, Any?>()
        )))
        assertAuthorities(convert(mapOf(
                "realm_access" to emptyMap<String, Any?>(),
                "azp" to "client_id",
                "resource_access" to mapOf(
                        "account" to emptyMap<String, Any?>(),
                        "client_id" to emptyMap()
                )
        )))
        assertAuthorities(convert(mapOf(
                "realm_access" to mapOf("roles" to null),
                "azp" to "client_id",
                "resource_access" to mapOf(
                        "account" to mapOf("roles" to null),
                        "client_id" to mapOf("roles" to null)
                )
        )))
        assertAuthorities(convert(mapOf(
                "realm_access" to mapOf("roles" to emptyList<Nothing>()),
                "azp" to "client_id",
                "resource_access" to mapOf(
                        "account" to mapOf("roles" to emptyList<Nothing>()),
                        "client_id" to mapOf("roles" to emptyList())
                )
        )))
    }

    @Test
    fun `should extract realm access roles`() {
        val claims = mapOf(
                "resource_access" to mapOf("account" to mapOf("roles" to listOf("user", "admin")))
        )
        assertAuthorities(convert(claims), "ROLE_user", "ROLE_admin")
    }

    @Test
    fun `should extract client access roles`() {
        val azp = "rflux_client"
        val claims = mapOf(
                "azp" to azp,
                "resource_access" to mapOf(azp to mapOf("roles" to listOf("user", "admin")))
        )
        assertAuthorities(convert(claims), "ROLE_user", "ROLE_admin")
    }

    @Test
    fun `should extract account access roles`() {
        val claims = mapOf("realm_access" to mapOf("roles" to listOf("user", "admin")))
        assertAuthorities(convert(claims), "ROLE_user", "ROLE_admin")
    }

    @Test
    fun `should extract authorities from all sources`() {
        val azp = "rflux_client"
        val claims = mapOf(
                "azp" to azp,
                "realm_access" to mapOf("roles" to listOf("realm_user", "realm_admin")),
                "resource_access" to mapOf(
                        azp to mapOf("roles" to listOf("client_user", "client_admin")),
                        "account" to mapOf("roles" to listOf("account_user", "account_admin"))
                ),
                "scope" to "email profile"
        )
        assertAuthorities(convert(claims),
                "SCOPE_email", "SCOPE_profile",
                "ROLE_client_user", "ROLE_client_admin",
                "ROLE_account_user", "ROLE_account_admin",
                "ROLE_realm_user", "ROLE_realm_admin"
        )
    }

    private fun assertAuthorities(
            authToken: AbstractAuthenticationToken?,
            vararg authorities: String
    ) {
        assertNotNull(authToken)
        authToken?.run {
            assertAll({
                assertIterableEquals(
                        authorities.map { SimpleGrantedAuthority(it) },
                        authToken.authorities
                )
            }, {
                assertTrue(authToken.isAuthenticated)
            })
        }
    }

    private fun convert(claims: Map<String, Any?>) = converter.convert(jwt(claims))

    private fun jwt(
            claims: Map<String, Any?>,
            headers: Map<String, String> = mapOf(
                    "alg" to "RS256",
                    "typ" to "JWT",
                    "kid" to "knhddZkURFubF-LQSJIpZMkUHPPlPWYREx-W9hVd8m4"
            ),
            issuedAt: Instant = Instant.now(),
            expiresAt: Instant = issuedAt.plusSeconds(180)
    ) = Jwt("jwt.stub", issuedAt, expiresAt, headers, claims)
}