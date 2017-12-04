package com.example.ms

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.MediaType
import org.springframework.web.reactive.function.server.ServerResponse.ok
import org.springframework.web.reactive.function.server.body
import org.springframework.web.reactive.function.server.router

//@Configuration
class KotlinRoute(private val movieService: MovieService) {

    @Bean
    fun routerFunction() = router {

        GET("/movies") { ok().body(movieService.allMovies)}

        GET("/movies/{id}") { ok().body(movieService.getMovieById(it.pathVariable("id")))}

        GET("/movies/{id}/events") { ok().contentType(MediaType.TEXT_EVENT_STREAM).body(movieService.getEvents(it.pathVariable("id")))}
    }
}