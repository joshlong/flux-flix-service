package com.example;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.java.Log;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.server.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuple2;

import java.time.Duration;
import java.util.Date;
import java.util.Random;
import java.util.UUID;
import java.util.stream.Stream;

import static org.springframework.web.reactive.function.server.RequestPredicates.GET;
import static org.springframework.web.reactive.function.server.RouterFunctions.*;
import static org.springframework.web.reactive.function.server.ServerResponse.*;

@SpringBootApplication
public class FfsServiceApplication {

    @Bean
    RouterFunction<?> routerFunction(RouteHandler rh) {
        return route(GET("/movies/"), rh::all)
                .andRoute(GET("/movies/{id}"), rh::byId)
                .andRoute(GET("/movies/{id}/events"), rh::events);
    }

    public static void main(String[] args) {
        SpringApplication.run(FfsServiceApplication.class, args);
    }
}

@Component
class RouteHandler {

    private final FluxFlixService ffs;

    RouteHandler(FluxFlixService ffs) {
        this.ffs = ffs;
    }

    Mono<ServerResponse> all(ServerRequest request) {
        return ok().body(ffs.all(), Movie.class);
    }

    Mono<ServerResponse> byId(ServerRequest request) {
        return ok().body(ffs.byId(request.pathVariable("id")), Movie.class);
    }

    Mono<ServerResponse> events(ServerRequest request) {
        return ok()
                .contentType(MediaType.TEXT_EVENT_STREAM)
                .body(ffs.streamStreams(request.pathVariable("id")), MovieEvent.class);
    }
}

@Data
@AllArgsConstructor
@NoArgsConstructor
class MovieEvent {
    private Movie movie;
    private Date when;
    private String user;
}

/*

@RestController
@RequestMapping("/movies")
class FluxFlixRestController {

    private final FluxFlixService fluxFlixService;

    FluxFlixRestController(FluxFlixService fluxFlixService) {
        this.fluxFlixService = fluxFlixService;
    }

    @GetMapping(value = "/{id}/events", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    Flux<MovieEvent> crossTheStreams(@PathVariable String id) {
        return fluxFlixService.streamStreams(id);
    }

    @GetMapping("/{id}")
    Mono<Movie> byId(@PathVariable String id) {
        return fluxFlixService.byId(id);
    }

    @GetMapping
    Flux<Movie> all() {
        return fluxFlixService.all();
    }

}
*/

@Service
class FluxFlixService {

    private final MovieRepository movieRepository;

    FluxFlixService(MovieRepository movieRepository) {
        this.movieRepository = movieRepository;
    }

    public Flux<MovieEvent> streamStreams(String movieId) {
        return byId(movieId).flatMapMany(movie -> {

            Flux<MovieEvent> eventFlux = Flux.fromStream(
                    Stream.generate(() -> new MovieEvent(movie, new Date(), randomUser())));

            Flux<Long> interval = Flux.interval(Duration.ofSeconds(1));

            return Flux.zip(eventFlux, interval).map(Tuple2::getT1);
        });
    }

    private String randomUser() {
        String users[] = "dsyer,sdeleuze,mkheck,jlong".split(",");
        return users[new Random().nextInt(users.length)];
    }

    public Flux<Movie> all() {
        return movieRepository.findAll();
    }

    public Mono<Movie> byId(String id) {
        return movieRepository.findById(id);
    }
}

@Log
@Component
class MovieDataCLR implements CommandLineRunner {

    private final MovieRepository movieRepository;

    MovieDataCLR(MovieRepository movieRepository) {
        this.movieRepository = movieRepository;
    }

    @Override
    public void run(String... strings) throws Exception {

        this.movieRepository
                .deleteAll()
                .subscribe(null, null, () ->
                        Stream.of("Flux Gordon", "Enter the Mono<Void>", "Back to the Future", "AEon Flux")
                                .map(title -> new Movie(title, UUID.randomUUID().toString()))
                                .forEach(movie -> movieRepository.save(movie).subscribe(m -> log.info(m.toString()))));
    }
}

interface MovieRepository extends ReactiveMongoRepository<Movie, String> {
}

@AllArgsConstructor
@Data
@Document
class Movie {

    private String title;

    @Id
    private String id;
}