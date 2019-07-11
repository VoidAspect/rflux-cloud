package com.voidaspect.rflux.rockets.routes

import com.voidaspect.rflux.rockets.handlers.launch.LaunchHandler
import com.voidaspect.rflux.rockets.handlers.rockets.RocketsHandler
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.MediaType.APPLICATION_JSON
import org.springframework.http.MediaType.TEXT_EVENT_STREAM
import org.springframework.web.reactive.function.server.router

@Configuration
class RoutesConfig(
        private val rocketsHandler: RocketsHandler,
        private val launchHandler: LaunchHandler
) {

    @Bean
    fun routes() = router {

        "/api".nest {

            "/rockets".nest {
                accept(APPLICATION_JSON).nest {
                    GET("/", rocketsHandler::findAll)
                    GET("/{id}", rocketsHandler::get)
                    DELETE("/{id}", rocketsHandler::remove)

                    contentType(APPLICATION_JSON).nest {
                        POST("/", rocketsHandler::add)
                        PUT("/{id}", rocketsHandler::update)
                        PATCH("/{id}/status", rocketsHandler::changeStatus)
                        PATCH("/{id}/warhead", rocketsHandler::changeWarhead)
                        PATCH("/{id}/target", rocketsHandler::changeTarget)
                    }
                }

                accept(TEXT_EVENT_STREAM).and(GET("/")).invoke(rocketsHandler::stream)
            }

            "/launch".nest {
                accept(APPLICATION_JSON).nest {
                    GET("/", launchHandler::findAll)
                    GET("/{id}", launchHandler::get)

                    contentType(APPLICATION_JSON).nest {
                        POST("/", launchHandler::launch)
                    }
                }

                accept(TEXT_EVENT_STREAM).and(GET("/")).invoke(launchHandler::stream)
            }

        }

    }

}