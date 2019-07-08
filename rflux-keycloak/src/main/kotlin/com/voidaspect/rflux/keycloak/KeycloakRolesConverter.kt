package com.voidaspect.rflux.keycloak

import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter

/**
 * Converts Keycloak resource access claims to Spring security roles
 */
class KeycloakRolesConverter : JwtAuthenticationConverter() {

    private companion object {

        //region Keycloak JWT claims

        private const val AUTHORIZED_PARTY = "azp"

        private const val RESOURCE_ACCESS = "resource_access"

        private const val REALM_ACCESS = "realm_access"

        private const val ACCOUNT = "account"

        private const val ROLES = "roles"

        //endregion

        private const val ROLE_AUTHORITY_PREFIX = "ROLE_"

    }

    override fun extractAuthorities(jwt: Jwt): MutableCollection<GrantedAuthority> {
        val authorities = super.extractAuthorities(jwt)

        val clientId = jwt.getClaimAsString(AUTHORIZED_PARTY)

        val resourceAccess: Map<String, Any>? = jwt.getClaimAsMap(RESOURCE_ACCESS)
        val clientAccess = resourceAccess?.get(clientId) as Map<*, *>?
        val accountAccess = resourceAccess?.get(ACCOUNT) as Map<*, *>?
        val realmAccess: Map<String, Any>? = jwt.getClaimAsMap(REALM_ACCESS)

        fun extractRoles(map: Map<*, *>?): List<String> = map?.let { access ->
            val roles = access[ROLES] as List<*>?
            roles?.filterIsInstance<String>()
        } ?: emptyList()

        val roles = extractRoles(clientAccess) + extractRoles(accountAccess) + extractRoles(realmAccess)

        val clientAuthorities = roles
                .map { ROLE_AUTHORITY_PREFIX + it } // spring ROLE_ convention
                .map { SimpleGrantedAuthority(it) }

        authorities.addAll(clientAuthorities)

        return authorities
    }
}