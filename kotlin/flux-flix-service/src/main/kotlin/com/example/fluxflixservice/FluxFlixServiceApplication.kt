package com.example.fluxflixservice

import org.springframework.boot.ApplicationRunner
import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document
import org.springframework.data.mongodb.repository.ReactiveMongoRepository
import org.springframework.http.MediaType
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity
import org.springframework.security.core.userdetails.MapUserDetailsRepository
import org.springframework.security.core.userdetails.User
import org.springframework.stereotype.Service
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.reactive.function.server.RequestPredicates
import org.springframework.web.reactive.function.server.RouterFunctions
import org.springframework.web.reactive.function.server.ServerResponse
import org.springframework.web.reactive.function.server.router
import reactor.core.publisher.Flux
import reactor.core.publisher.SynchronousSink
import java.time.Duration
import java.util.*

@SpringBootApplication
class FluxFlixServiceApplication {

    @Bean
    fun runner(mr: MovieRepository) = ApplicationRunner {
        val movies = Flux.just("Silence of the Lambdas", "AEon Flux", "Back to the Future")
                .flatMap { mr.save(Movie(title = it)) }
        mr
                .deleteAll()
                .thenMany(movies)
                .thenMany(mr.findAll())
                .subscribe({ println(it) })
    }
}

@Service
class MovieService(private val mr: MovieRepository) {

    fun all() = mr.findAll()

    fun byId(id: String) = mr.findById(id)

    fun events(id: String) = Flux
            .generate({ sink: SynchronousSink<MovieEvent> -> sink.next(MovieEvent(id, Date())) })
            .delayElements(Duration.ofSeconds(1L))
}

@Configuration
@EnableWebFluxSecurity
class SecurityConfiguration {

    @Bean
    fun users() = MapUserDetailsRepository(User.withUsername("rwinch").password("pw").roles("ADMIN", "USER").build(),
            User.withUsername("jlong").password("pw").roles("USER").build())

}

@Configuration
class WebConfiguration(val ms: MovieService) {

    @Bean
    fun routes() = router {
        GET("/movies", { ServerResponse.ok().body(ms.all(), Movie::class.java) })
        GET("/movies/{id}", { ServerResponse.ok().body(ms.byId(it.pathVariable("id")), Movie::class.java) })
        GET("/movies/{id}/events", { ServerResponse.ok().contentType(MediaType.TEXT_EVENT_STREAM).body(ms.events(it.pathVariable("id")), MovieEvent::class.java) })
    }
}

/*
@RestController
class MovieRestController(var ms: MovieService) {

    @GetMapping("/movies")
    fun all() = ms.all()

    @GetMapping("/movies/{id}")
    fun byId(@PathVariable id: String) = ms.byId(id)

    @GetMapping("/movies/{id}/events", produces = arrayOf(MediaType.TEXT_EVENT_STREAM_VALUE))
    fun events(@PathVariable id: String) = ms.events(id)
}
*/

data class MovieEvent(var movieId: String? = null, var date: Date? = null)

fun main(args: Array<String>) {
    SpringApplication.run(FluxFlixServiceApplication::class.java, *args)
}


interface MovieRepository : ReactiveMongoRepository<Movie, String>

@Document
data class Movie(@Id var id: String? = null, var title: String? = null)
