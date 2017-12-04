package com.example.ms;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@SpringBootApplication
public class Application {

    @Bean
    CommandLineRunner demoData(MovieRepository movieRepository) {
        return args -> {
            movieRepository.deleteAll().thenMany(
                    Flux.just("Silence of the Lambdas", "A", "B", "C")
                            .map(Movie::new)
                            .flatMap(movieRepository::save))
                    .thenMany(movieRepository.findAll())
                    .subscribe(System.out::println);
        };
    }

    /*@Bean
    RouterFunction<?> routerFunction(MovieService movieService) {
        return route(GET("/movies"), req -> ok().body(movieService.getAllMovies(), Movie.class))
                .andRoute(GET("/movies/{id}"), req -> ok().body(movieService.getMovieById(req.pathVariable("id")), Movie.class))
                .andRoute(GET("/movies/{id}/events"), req -> ok().contentType(MediaType.TEXT_EVENT_STREAM).body(movieService.getEvents(req.pathVariable("id")), MovieEvent.class));
    }*/

    public static void main(String[] args) {
        SpringApplication.run(Application.class);
    }
}

@RestController
class MovieRestController {

    private final MovieService service;

    MovieRestController(MovieService service) {
        this.service = service;
    }

    @GetMapping("/movies")
    Flux<Movie> all() {
        return this.service.getAllMovies();
    }

    @GetMapping("/movies/{id}")
    Mono<Movie> byId(@PathVariable String id) {
        return this.service.getMovieById(id);
    }

    @GetMapping(value = "/movies/{id}/events", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    Flux<MovieEvent> events(@PathVariable String id) {
        return this.service.getEvents(id);
    }
}

interface MovieRepository extends ReactiveCrudRepository<Movie, String> {

}

