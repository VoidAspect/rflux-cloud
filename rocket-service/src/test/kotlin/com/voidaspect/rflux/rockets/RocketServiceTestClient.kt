package com.voidaspect.rflux.rockets

import com.voidaspect.rflux.rockets.handlers.launch.LaunchResponse
import com.voidaspect.rflux.rockets.handlers.launch.LaunchRocketCommand
import com.voidaspect.rflux.rockets.handlers.rockets.*
import com.voidaspect.rflux.rockets.model.LaunchId
import com.voidaspect.rflux.rockets.model.RocketId
import org.springframework.http.HttpMethod
import org.springframework.http.MediaType
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.test.web.reactive.server.returnResult
import org.springframework.web.reactive.function.client.ExchangeFilterFunctions.basicAuthentication

class RocketServiceTestClient(
        port: Int,
        username: String,
        password: String
) {

    private val rest: WebTestClient = WebTestClient.bindToServer()
            .baseUrl("http://localhost:$port")
            .filter(basicAuthentication(username, password))
            .build()

    //region Rockets API

    fun getRockets() = getJson(ROCKETS_API)
            .expectStatus().isOk
            .expectHeader().contentType(MediaType.APPLICATION_JSON_UTF8)

    fun getRocket(id: RocketId) = getJson("$ROCKETS_API/$id")

    fun getRocketSSE() = getSSE(ROCKETS_API).returnResult<RocketResponse>()

    fun deleteRocket(id: RocketId) = rest
            .delete().uri("$ROCKETS_API/$id")
            .accept(MediaType.APPLICATION_JSON_UTF8)
            .exchange()

    fun putRocket(
            rocketId: RocketId,
            body: UpdateRocketCommand
    ) = sendJson("$ROCKETS_API/$rocketId", body, HttpMethod.PUT)

    fun postRocket(
            body: AddRocketCommand
    ) = sendJson(ROCKETS_API, body, HttpMethod.POST)

    fun mergeRocket(
            rocketId: RocketId,
            body: MergeRocketCommand
    ) = sendJson("$ROCKETS_API/$rocketId", body, HttpMethod.PATCH)

    fun patchRocket(
            rocketId: RocketId,
            body: Any
    ) = sendJson("$ROCKETS_API/$rocketId/${when (body) {
        is ChangeStatusCommand -> "status"
        is ChangeTargetCommand -> "target"
        is ChangeWarheadCommand -> "warhead"
        else -> throw IllegalArgumentException("invalid body type ${body::class.qualifiedName}")
    }}", body, HttpMethod.PATCH)

    //endregion

    //region Launch API

    fun getLaunch(id: LaunchId) = getJson("$LAUNCH_API/$id")

    fun getLaunches() = getJson(LAUNCH_API)
            .expectStatus().isOk
            .expectHeader().contentType(MediaType.APPLICATION_JSON_UTF8)

    fun getLaunchesSSE() = getSSE(LAUNCH_API).returnResult<LaunchResponse>()

    fun postLaunch(
            rocket: RocketId
    ) = sendJson(LAUNCH_API, LaunchRocketCommand(rocket), HttpMethod.POST)

    //endregion

    private fun getJson(path: String): WebTestClient.ResponseSpec = rest
            .get().uri(path)
            .accept(MediaType.APPLICATION_JSON_UTF8)
            .exchange()

    private fun sendJson(
            path: String,
            body: Any,
            method: HttpMethod
    ): WebTestClient.ResponseSpec = rest
            .method(method).uri(path)
            .accept(MediaType.APPLICATION_JSON_UTF8)
            .contentType(MediaType.APPLICATION_JSON_UTF8)
            .syncBody(body)
            .exchange()

    private fun getSSE(path: String): WebTestClient.ResponseSpec = rest
            .get().uri(path)
            .accept(MediaType.TEXT_EVENT_STREAM)
            .exchange()
            .expectStatus().isOk
            .expectHeader()
            .contentType("${MediaType.TEXT_EVENT_STREAM_VALUE};charset=UTF-8")
}