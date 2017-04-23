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

import java.time.Duration;
import java.util.Date;
import java.util.Random;
import java.util.UUID;
import java.util.stream.Stream;

import static org.springframework.web.reactive.function.server.RequestPredicates.GET;
import static org.springframework.web.reactive.function.server.RouterFunctions.route;

@SpringBootApplication
public class FluxFlixServiceApplication {

	@Bean
	CommandLineRunner demoData(MovieRepository movieRepository) {
		return strings -> {
			movieRepository.deleteAll().block();
			Stream.of("AEon Flux", "Flux Gordon", "Enter the Mono<Void>", "The Silence of the Lambdas",
					"The Flux & The Furious", "Y Tu Mono Tambien", "Flux to the Future", "The Streaming",
					"Streaming Las Vegas", "The Fluxinator")
					.map(title -> new Movie(UUID.randomUUID().toString(), title, randomGenre()))
					.forEach(movie -> movieRepository.save(movie).subscribe(System.out::println));
		};
	}

	private String randomGenre() {
		String[] genres = "comedy, drama, romcom, documentary, horror, family".split(", ");
		return genres[new Random().nextInt(genres.length)];
	}

//    @Bean
//    RouterFunction<?> routerFunction(FluxFlixService fluxFlixService) {
//        return route(
//                GET("/movies/{id}"),
//                request -> ServerResponse.ok().body(fluxFlixService.movieById(request.pathVariable("id")), Movie.class))
//            .andRoute(
//                GET("/movies"), request -> ServerResponse.ok().body(fluxFlixService.allMovies(), Movie.class))
//            .andRoute(
//                GET("/movies/{id}/events"),
//                    request -> ServerResponse.ok()
//                        .contentType(MediaType.TEXT_EVENT_STREAM)
//                        .body(fluxFlixService.movieById(request.pathVariable("id"))
//                            .flatMap(fluxFlixService::eventsForMovie), MovieEvent.class));
//    }

	public static void main(String[] args) {
		SpringApplication.run(FluxFlixServiceApplication.class, args);
	}
}

@RestController
@RequestMapping("/movies")
class MovieController {
	private final FluxFlixService fluxFlixService;

	public MovieController(FluxFlixService fluxFlixService) {
		this.fluxFlixService = fluxFlixService;
	}

	@GetMapping
	public Flux<Movie> all() {
		return this.fluxFlixService.allMovies();
	}

	@GetMapping("/{id}")
	public Mono<Movie> byId(@PathVariable String id) {
		return this.fluxFlixService.movieById(id);
	}

	@GetMapping(value = "/{id}/events", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
	public Flux<MovieEvent> eventsForMovie(@PathVariable String id) {
		return this.fluxFlixService.movieById(id)
				.flatMapMany(this.fluxFlixService::eventsForMovie);
	}
}

@Service
class FluxFlixService {
	private final MovieRepository movieRepository;

	public FluxFlixService(MovieRepository movieRepository) {
		this.movieRepository = movieRepository;
	}

	public Flux<Movie> allMovies() {
		return this.movieRepository.findAll();
	}

	public Mono<Movie> movieById(String id) {
		return this.movieRepository.findOne(id);
	}

	public Mono<Movie> movieByTitle(String title) {
		return this.movieRepository.findByTitle(title);
	}

	public Flux<Movie> moviesByGenre(String genre) {
		return this.movieRepository.findByGenre(genre);
	}

	public Flux<MovieEvent> eventsForMovie(Movie movie) {
		Flux<MovieEvent> eventFlux = Flux.fromStream(
				Stream.generate(() -> new MovieEvent(Math.random() > .5 ? "Josh" : "Mark", movie, new Date())));
		return Flux.zip(eventFlux, Flux.interval(Duration.ofSeconds(1))).map(Tuple2::getT1);
	}
}

interface MovieRepository extends ReactiveMongoRepository<Movie, String> {
	Mono<Movie> findByTitle(String title);
	Flux<Movie> findByGenre(String genre);
}

@Document
@Data
@AllArgsConstructor
class Movie {
	@Id
	private String id;
	private String title, genre;
}

@Data
@AllArgsConstructor
class MovieEvent {
	private String user;
	private Movie movie;
	private Date date;
}