package com.example

import org.apache.commons.logging.LogFactory
import org.springframework.boot.CommandLineRunner
import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.context.annotation.Bean
import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document
import org.springframework.data.mongodb.repository.ReactiveMongoRepository
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.server.ServerRequest
import org.springframework.web.reactive.function.server.ServerResponse.ok
import org.springframework.web.reactive.function.server.body
import org.springframework.web.reactive.function.server.router
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.time.Duration
import java.util.*
import java.util.stream.Stream

@SpringBootApplication
class FfsServiceKotlinApplication

fun main(args: Array<String>) {
    SpringApplication.run(FfsServiceKotlinApplication::class.java, *args)
}

@Component
class RouteHandler(val ffs: FluxFlixService) {

    fun byId(request: ServerRequest) = ok().body(ffs.byId(request.pathVariable("id")), Movie::class.java)

    fun events(request: ServerRequest) =
            ok()
            .contentType(MediaType.TEXT_EVENT_STREAM)
            .body(ffs.streamStreams(request.pathVariable("id")), MovieEvent::class.java)

    fun all(request: ServerRequest) = ok().body(ffs.all(), Movie::class.java)
}

@SpringBootApplication
class FfsServiceApplication(val rh: RouteHandler) {

    @Bean
    fun routes() = router {
        "/movies".nest {
            GET("/", rh::all)
            GET("/{id}/events", rh::events)
            GET("/{id}", rh::byId)
        }
    }
}


data class MovieEvent(val movie: Movie? = null,
                      val `when`: Date? = null,
                      val user: String? = null)

@Service
class FluxFlixService(val movieRepository: MovieRepository) {

    fun streamStreams(movieId: String): Flux<MovieEvent> {
        return byId(movieId).flatMapMany { movie ->
            val eventFlux = Flux.fromStream( Stream.generate { MovieEvent(movie, Date(), randomUser()) })
            val interval = Flux.interval(Duration.ofSeconds(1))
            Flux.zip(eventFlux, interval).map { it.t1 }
        }
    }

    private fun randomUser(): String {
        val users = "dsyer,sdeleuze,mkheck,jlong".split(",".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
        return users[Random().nextInt(users.size)]
    }

    fun all(): Flux<Movie> {
        return movieRepository.findAll()
    }

    fun byId(id: String): Mono<Movie> {
        return movieRepository.findOne(id)
    }
}


@Component
class MovieDataCLR(val movieRepository: MovieRepository) : CommandLineRunner {

    val log = LogFactory.getLog(javaClass)

    override fun run(vararg strings: String) {

        this.movieRepository
                .deleteAll()
                .subscribe(null, null) {
                    Stream.of("Flux Gordon", "Enter the Mono<Void>", "Back to the Future", "AEon Flux")
                            .map { title -> Movie(title, UUID.randomUUID().toString()) }
                            .forEach { movie -> movieRepository.save(movie).subscribe { m -> log.info(m.toString()) } }
                }
    }
}

interface MovieRepository : ReactiveMongoRepository<Movie, String>


@Document
data class Movie(var title: String? = null,
                 @Id var id: String? = null)