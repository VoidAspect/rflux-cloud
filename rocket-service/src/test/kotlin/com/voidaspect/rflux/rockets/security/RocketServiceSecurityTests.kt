package com.voidaspect.rflux.rockets.security

import com.voidaspect.rflux.rockets.LAUNCH_API
import com.voidaspect.rflux.rockets.ROCKETS_API
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.web.server.LocalServerPort
import org.springframework.http.HttpMethod
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.reactive.server.WebTestClient


@ActiveProfiles("basic-auth")
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = [
            "spring.security.user.roles=USER"
        ]
)
internal class RocketServiceSecurityTests {

    @LocalServerPort
    private var port: Int = 0

    @Value("\${spring.security.user.name}")
    private lateinit var username: String

    @Value("\${spring.security.user.password}")
    private lateinit var password: String

    private lateinit var rest: WebTestClient

    @BeforeEach
    internal fun setUp() {
        rest = WebTestClient
                .bindToServer()
                .baseUrl("http://localhost:$port")
                .build()
    }

    @Test
    fun `context loads`() {
        assertNotEquals(0, port)
        assertTrue(username.isNotBlank())
        assertTrue(password.isNotBlank())
    }

    @Test
    fun `health endpoint open`() {
        rest.get()
                .uri("/actuator/health")
                .exchange()
                .expectStatus().isOk
                .expectBody()
                .json("""{"status":"UP"}""")
    }

    @TestFactory
    fun `unauthorized requests to be rejected`() = listOf(
            "/",
            "/api",
            LAUNCH_API,
            ROCKETS_API
    ).flatMap { uri ->
        listOf(
                HttpMethod.POST,
                HttpMethod.GET,
                HttpMethod.PUT,
                HttpMethod.PATCH,
                HttpMethod.DELETE
        ).map { method ->
            DynamicTest.dynamicTest("unauthorized $method to $uri") {
                rest.method(method)
                        .uri(uri)
                        .exchange()
                        .expectStatus().isUnauthorized
            }
        }
    }

    @Test
    fun `write access to launch should be restricted to role rockets_launch`() {
        rest.post()
                .uri(LAUNCH_API)
                .headers { it.setBasicAuth(username, password) }
                .exchange()
                .expectStatus().isForbidden
        rest.get()
                .uri(LAUNCH_API)
                .headers { it.setBasicAuth(username, password) }
                .exchange()
                .expectStatus().isOk
    }
}