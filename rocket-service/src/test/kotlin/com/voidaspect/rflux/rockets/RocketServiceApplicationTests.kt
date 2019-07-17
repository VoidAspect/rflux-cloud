package com.voidaspect.rflux.rockets

import com.voidaspect.rflux.rockets.handlers.launch.LaunchResponse
import com.voidaspect.rflux.rockets.handlers.rockets.*
import com.voidaspect.rflux.rockets.model.*
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.web.server.LocalServerPort
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.junit.jupiter.SpringExtension
import org.springframework.test.web.reactive.server.expectBody
import reactor.test.StepVerifier
import java.time.ZonedDateTime

@ExtendWith(SpringExtension::class)
@ActiveProfiles("basic-auth")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
internal class RocketServiceApplicationTests {

    private val uuidPattern = Regex(
            "[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}")

    @LocalServerPort
    private var port: Int = 0

    @Value("\${spring.security.user.name}")
    private lateinit var username: String

    @Value("\${spring.security.user.password}")
    private lateinit var password: String

    @Autowired
    private lateinit var cleaner: RocketServiceStateCleaner

    private lateinit var client: RocketServiceTestClient

    @BeforeEach
    internal fun setUp() {
        client = RocketServiceTestClient(port, username, password)
        cleaner.clean()
    }

    @Test
    fun `add rocket`() {
        addRocketAndVerify(Warhead.NUCLEAR, 0.10, 0.10)
    }

    @Test
    fun `get rockets empty`() {
        client.getRockets().expectBody().json("[]")

        client.getRocket(RocketId.randomUUID()).expectStatus().isNotFound
    }

    @Test
    fun `get rockets`() {
        val rocket1 = addRocketAndVerify(latitude = 0.10, longitude = 0.10)
        val rocket2 = addRocketAndVerify(Warhead.NUCLEAR, 0.10, 0.10)

        val result = client.getRockets()
                .expectBody<Array<RocketResponse>>()
                .returnResult()

        result.assertWithDiagnostics {
            assertEquals(setOf(rocket1, rocket2), result.responseBody!!.toSet())
        }

        client.getRocket(rocket1.id)
                .expectStatus().isOk
                .expectHeader().contentType(MediaType.APPLICATION_JSON_UTF8)
                .expectBody<RocketResponse>()
                .isEqualTo(rocket1)

        client.getRocket(rocket2.id)
                .expectStatus().isOk
                .expectHeader().contentType(MediaType.APPLICATION_JSON_UTF8)
                .expectBody<RocketResponse>()
                .isEqualTo(rocket2)
    }

    @Test
    fun `update rocket`() {
        val invalidId = RocketId.randomUUID()
        val update = UpdateRocketCommand(Warhead.NUCLEAR, Status.READY,
                TargetCoordinates(2.5, 2.5))

        client.putRocket(invalidId, update)
                .expectStatus().isNotFound

        val rocket = addRocketAndVerify()

        client.putRocket(rocket.id, update)
                .expectStatus().isOk
                .expectBody<RocketResponse>()
                .isEqualTo(rocket.copy(
                        warhead = update.warhead,
                        status = update.status,
                        target = update.target))

        client.putRocket(rocket.id, update.copy(status = Status.LAUNCHED))
                .expectStatus().isBadRequest
    }

    @Test
    fun `update rocket status`() {
        val invalidId = RocketId.randomUUID()
        val update = ChangeStatusCommand(Status.READY)

        client.patchRocket(invalidId, update)
                .expectStatus().isNotFound

        val rocket = addRocketAndVerify()

        client.patchRocket(rocket.id, update)
                .expectStatus().isOk
                .expectBody<RocketResponse>()
                .isEqualTo(rocket.copy(status = update.status))

        client.patchRocket(rocket.id, ChangeStatusCommand(Status.LAUNCHED))
                .expectStatus().isBadRequest
    }

    @Test
    fun `update rocket warhead`() {
        val invalidId = RocketId.randomUUID()
        val update = ChangeWarheadCommand(Warhead.NUCLEAR)

        client.patchRocket(invalidId, update)
                .expectStatus().isNotFound

        val rocket = addRocketAndVerify()

        client.patchRocket(rocket.id, update)
                .expectStatus().isOk
                .expectBody<RocketResponse>()
                .isEqualTo(rocket.copy(warhead = update.warhead))
    }

    @Test
    fun `update rocket target`() {
        val invalidId = RocketId.randomUUID()
        val update = ChangeTargetCommand(0.2, 0.2)

        client.patchRocket(invalidId, update)
                .expectStatus().isNotFound

        val rocket = addRocketAndVerify()

        client.patchRocket(rocket.id, update)
                .expectStatus().isOk
                .expectBody<RocketResponse>()
                .isEqualTo(rocket.copy(target = TargetCoordinates(
                        update.latitude, update.longitude)))
    }

    @Test
    fun `merge rocket`() {
        val invalidId = RocketId.randomUUID()
        val update = MergeRocketCommand(Warhead.NUCLEAR, Status.READY, TargetCoordinates(0.2, 0.2))

        client.mergeRocket(invalidId, update)
                .expectStatus().isNotFound

        val rocket = addRocketAndVerify()

        client.mergeRocket(rocket.id, update)
                .expectStatus().isOk
                .expectBody<RocketResponse>()
                .isEqualTo(rocket.copy(
                        warhead = update.warhead!!,
                        status = update.status!!,
                        target = update.target!!
                ))
    }

    @Test
    fun `merge rocket status`() {
        val rocket = addRocketAndVerify()

        val wrongStatus = Status.LAUNCHED
        client.mergeRocket(rocket.id, MergeRocketCommand(status = wrongStatus))
                .expectStatus().isBadRequest

        val status = Status.READY
        client.mergeRocket(rocket.id, MergeRocketCommand(status = status))
                .expectStatus().isOk
                .expectBody<RocketResponse>()
                .isEqualTo(rocket.copy(status = status))
    }


    @Test
    fun `merge rocket warhead`() {
        val rocket = addRocketAndVerify()

        val warhead = Warhead.NUCLEAR
        client.mergeRocket(rocket.id, MergeRocketCommand(warhead = warhead))
                .expectStatus().isOk
                .expectBody<RocketResponse>()
                .isEqualTo(rocket.copy(warhead = warhead))
    }

    @Test
    fun `merge rocket target`() {
        val rocket = addRocketAndVerify()

        val target = TargetCoordinates(0.2, 0.2)
        client.mergeRocket(rocket.id, MergeRocketCommand(target = target))
                .expectStatus().isOk
                .expectBody<RocketResponse>()
                .isEqualTo(rocket.copy(target = target))
    }

    @Test
    fun `remove rocket`() {
        client.deleteRocket(RocketId.randomUUID())
                .expectStatus().isNotFound

        val rocket = addRocketAndVerify()

        client.deleteRocket(rocket.id)
                .expectStatus().isOk
                .expectHeader().contentType(MediaType.APPLICATION_JSON_UTF8)
                .expectBody<RocketResponse>()
                .isEqualTo(rocket)

        client.getRocket(rocket.id)
                .expectStatus().isNotFound

        client.deleteRocket(rocket.id)
                .expectStatus().isNotFound
    }

    @Test
    fun `get rockets sse`() {

        StepVerifier.create(client.getRocketSSE().responseBody)
                .verifyComplete()

        val response1 = addRocketAndVerify(Warhead.NUCLEAR, 0.10, 0.10)
        val response2 = addRocketAndVerify()

        StepVerifier.create(client.getRocketSSE().responseBody)
                .recordWith { hashSetOf<RocketResponse>() }
                .expectNextCount(2)
                .expectRecordedMatches { it == setOf(response1, response2) }
                .verifyComplete()
    }

    @Test
    fun `launch rocket not ready`() {
        client.postLaunch(addRocketAndVerify().id)
                .expectStatus().isBadRequest
    }

    @Test
    fun `launch rocket not found`() {
        client.postLaunch(RocketId.randomUUID())
                .expectStatus().isNotFound
    }

    @Test
    fun `launch rocket`() {
        launchAndVerify()
    }

    @Test
    fun `should not launch rocket twice`() {
        val launch = launchAndVerify()
        client.postLaunch(launch.rocket.id)
                .expectStatus().isBadRequest
    }

    @Test
    fun `should not update rocket after launch`() {
        val launch = launchAndVerify()
        val rocket = launch.rocket

        assertAll({
            val update = UpdateRocketCommand(
                    rocket.warhead,
                    Status.NOT_READY,
                    rocket.target
            )
            client.putRocket(rocket.id, update).expectStatus().isBadRequest
        }, {
            client.patchRocket(rocket.id, ChangeStatusCommand(Status.NOT_READY))
                    .expectStatus().isBadRequest
        }, {
            client.patchRocket(rocket.id, ChangeWarheadCommand(Warhead.NUCLEAR))
                    .expectStatus().isBadRequest
        }, {
            client.patchRocket(rocket.id, ChangeTargetCommand(0.0, 0.0))
                    .expectStatus().isBadRequest
        }, {
            client.mergeRocket(rocket.id, MergeRocketCommand(status = Status.NOT_READY))
                    .expectStatus().isBadRequest
        })
    }

    @Test
    fun `get launches empty`() {
        client.getLaunches().expectBody().json("""[]""")

        client.getLaunch(LaunchId.randomUUID()).expectStatus().isNotFound
    }

    @Test
    fun `get launches`() {
        val launch1 = launchAndVerify()
        val launch2 = launchAndVerify()

        val result = client.getLaunches()
                .expectStatus().isOk
                .expectHeader().contentType(MediaType.APPLICATION_JSON_UTF8)
                .expectBody<Array<LaunchResponse>>()
                .returnResult()

        result.assertWithDiagnostics {
            assertEquals(setOf(launch1, launch2), result.responseBody!!.toSet())
        }

        client.getLaunch(launch1.id)
                .expectStatus().isOk
                .expectHeader().contentType(MediaType.APPLICATION_JSON_UTF8)
                .expectBody<LaunchResponse>()
                .isEqualTo(launch1)

        client.getLaunch(launch2.id)
                .expectStatus().isOk
                .expectHeader().contentType(MediaType.APPLICATION_JSON_UTF8)
                .expectBody<LaunchResponse>()
                .isEqualTo(launch2)
    }

    @Test
    fun `get launches sse`() {

        StepVerifier.create(client.getLaunchesSSE().responseBody)
                .verifyComplete()

        val response1 = launchAndVerify()
        val response2 = launchAndVerify()

        StepVerifier.create(client.getLaunchesSSE().responseBody)
                .recordWith { hashSetOf<LaunchResponse>() }
                .expectNextCount(2)
                .expectRecordedMatches { it == setOf(response1, response2) }
                .verifyComplete()
    }

    private fun readyRocket(
            rocketId: RocketId
    ): RocketResponse {
        val result = client.patchRocket(rocketId, ChangeStatusCommand(Status.READY))
                .expectStatus().isOk
                .expectHeader().contentType(MediaType.APPLICATION_JSON_UTF8)
                .expectBody<RocketResponse>()
                .returnResult()
        val response = result.responseBody!!
        result.assertWithDiagnostics {
            assertEquals(rocketId, response.id)
            assertEquals(Status.READY, response.status)
        }
        return response
    }

    private fun addRocketAndVerify(
            warhead: Warhead = Warhead.CONVENTIONAL,
            latitude: Double = 0.1,
            longitude: Double = 0.1
    ): RocketResponse {
        val rocket = AddRocketCommand(warhead, TargetCoordinates(latitude, longitude))
        val result = client.postRocket(rocket)
                .expectStatus().isCreated
                .expectHeader().contentType(MediaType.APPLICATION_JSON_UTF8)
                .expectHeader().valueMatches("Location", "/api/rockets/${uuidPattern.pattern}")
                .expectBody<RocketResponse>()
                .returnResult()
        val response = result.responseBody!!
        result.assertWithDiagnostics {
            assertEquals(Status.NOT_READY, response.status)
            assertEquals(rocket.target, response.target)
            assertEquals(rocket.warhead, response.warhead)
        }
        return response
    }

    private fun launchAndVerify(): LaunchResponse {
        val beforeLaunch = ZonedDateTime.now()
        val added = addRocketAndVerify()
        val ready = readyRocket(added.id)
        val result = client.postLaunch(ready.id)
                .expectStatus().isCreated
                .expectHeader().valueMatches("Location", "/api/launch/${uuidPattern.pattern}")
                .expectHeader().contentType(MediaType.APPLICATION_JSON_UTF8)
                .expectBody<LaunchResponse>()
                .returnResult()
        val launchResponse = result.responseBody!!
        result.assertWithDiagnostics {
            val (_, rocket, time) = launchResponse
            assertEquals(ready.copy(status = Status.LAUNCHED), rocket)
            assertTrue(time.isAfter(beforeLaunch))
        }
        return launchResponse
    }

}

