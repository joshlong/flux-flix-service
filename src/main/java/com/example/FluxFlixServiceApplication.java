package com.example;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuple2;

import java.awt.*;
import java.time.Duration;
import java.util.Date;
import java.util.Random;
import java.util.stream.Stream;

import static org.springframework.web.reactive.function.server.RequestPredicates.GET;
import static org.springframework.web.reactive.function.server.RouterFunctions.route;

@SpringBootApplication
public class FluxFlixServiceApplication {
    @Bean
    CommandLineRunner demoData(MovieRepository movieRepository) {
        return strings -> {
            movieRepository.deleteAll().block();

            Stream.of("Flux Gordon", "Meet the Fluxers", "The Flux & the Furious", "Y Tu Mono Tambien",
                    "The Silence of the Lambdas", "Fluxman vs. Superman", "AEon Flux")
                    .map(title -> new Movie(title, randomGenre()))
                    .forEach(movie -> movieRepository.save(movie).subscribe(System.out::println));
        };
    }

    private String randomGenre() {
        String[] genres = "romcom, comedy, horror, scifi, family, documentary, drama".split(", ");
        return genres[new Random().nextInt(genres.length)];
    }

//    @Bean
//    RouterFunction<?> routerFunction(FluxFlixService fluxFlixService) {
//        return route(
//                GET("/movies/{id}"),
//                request -> ServerResponse.ok().body(fluxFlixService.findById(request.pathVariable("id")), Movie.class))
//            .andRoute(
//                GET("/movies"), request -> ServerResponse.ok().body(fluxFlixService.findAllMovies(), Movie.class))
//            .andRoute(
//                GET("/movies/{id}/events"),
//                    request -> ServerResponse.ok()
//                        .contentType(MediaType.TEXT_EVENT_STREAM)
//                        .body(fluxFlixService.findById(request.pathVariable("id"))
//                            .flatMapMany(fluxFlixService::findMovieEvents), MovieEvent.class));
//    }


    public static void main(String[] args) {
        SpringApplication.run(FluxFlixServiceApplication.class, args);
    }
}

@RestController
@RequestMapping("/movies")
class MovieController {
    private FluxFlixService fluxFlixService;

    public MovieController(FluxFlixService fluxFlixService) {
        this.fluxFlixService = fluxFlixService;
    }

    @GetMapping
    Flux<Movie> all() {
        return this.fluxFlixService.findAllMovies();
    }

    @GetMapping("/{id}")
    Mono<Movie> movieById(@PathVariable String id) {
        return this.fluxFlixService.findById(id);
    }

    @GetMapping(value = "/{id}/events", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    Flux<MovieEvent> movieEvents(@PathVariable String id) {
        return this.fluxFlixService.findById(id)
                .flatMapMany(this.fluxFlixService::findMovieEvents);
    }
}

@Service
class FluxFlixService {
    private MovieRepository movieRepository;

    public FluxFlixService(MovieRepository movieRepository) {
        this.movieRepository = movieRepository;
    }

    Flux<Movie> findAllMovies() {
        return this.movieRepository.findAll();
    }

    Mono<Movie> findById(String id) {
        return this.movieRepository.findOne(id);
    }

    Mono<Movie> findByTitle(String title) {
        return this.movieRepository.findByTitle(title);
    }

    Flux<Movie> findRelatedMovies(Movie movie) {
        return this.movieRepository.findByGenre(movie.getGenre());
    }

    Flux<MovieEvent> findMovieEvents(Movie movie) {
        Flux<MovieEvent> eventFlux = Flux.fromStream(
                Stream.generate(() -> new MovieEvent(Math.random() > .5 ? "Josh": "Mark", movie, new Date())));
        Flux<Long> longFlux = Flux.interval(Duration.ofSeconds(1));
        return Flux.zip(eventFlux, longFlux).map(Tuple2::getT1);
    }
}

interface MovieRepository extends ReactiveMongoRepository<Movie, String> {
    Mono<Movie> findByTitle(String title);
    Flux<Movie> findByGenre(String genre);
}

@Data
@NoArgsConstructor
@AllArgsConstructor
class MovieEvent {
    private String user;
    private Movie movie;
    private Date when;
}

@Document
@Data
@NoArgsConstructor
@AllArgsConstructor
class Movie {
    @Id
    private String id;
    private String title, genre;

    public Movie(String title, String genre) {
        this.title = title;
        this.genre = genre;
    }
}