package com.example;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
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
public class FlixFluxServiceApplication implements CommandLineRunner {

	private final MovieRepository movieRepository;

	public static void main(String[] args) {
		SpringApplication.run(FlixFluxServiceApplication.class, args);
	}

	private Log log = LogFactory.getLog(getClass());

	@Override
	public void run(String... strings) throws Exception {

		this.movieRepository.deleteAll().block();

		Stream.of("AEon Flux", "The Fluxinator", "Flux Gordon", "Y Tu Mono Tambien",
				"Silence of the Lambdas", "Enter the Mono<Void>", "The Streaming")
				.map(title -> new Movie(UUID.randomUUID().toString(), title, randomGenre()))
				.forEach(m -> this.movieRepository.save(m).subscribe(this.log::info));
	}

	private String randomGenre() {
		String[] genres = "comedy,romcom,drama,action,horror,documentary".split(",");
		return genres[new Random().nextInt(genres.length)];
	}

	FlixFluxServiceApplication(MovieRepository movieRepository) {
		this.movieRepository = movieRepository;
	}


	//	@Bean
	RouterFunction<?> routerFunction(FluxFlixService service) {
		return
			route(
				GET("/movies/{id}"),
				request -> ServerResponse.ok().body(service.byId(request.pathVariable("id")), Movie.class)
			)
			.andRoute(
				GET("/movies"), request -> ServerResponse.ok().body(service.all(), Movie.class)
			)
			.andRoute(
				GET("/movies/{id}/events"),
				request -> ServerResponse.ok()
					.contentType(MediaType.TEXT_EVENT_STREAM)
					.body(service.byId(request.pathVariable("id"))
						.flatMap(service::crossTheStreams), MovieEvent.class)
			);
	}

}


@RestController
@RequestMapping("/movies")
class FluxFlixRestController {
	private final FluxFlixService fluxFlixService;

	FluxFlixRestController(FluxFlixService fluxFlixService) {
		this.fluxFlixService = fluxFlixService;
	}

	@GetMapping
	public Flux<Movie> all() {
		return this.fluxFlixService.all();
	}

	@GetMapping("/{movieId}")
	public Mono<Movie> byId(@PathVariable String movieId) {
		return this.fluxFlixService.byId(movieId);
	}

	@GetMapping(value = "/{movieId}/events", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
	public Flux<MovieEvent> streams(@PathVariable String movieId) {
		return this.fluxFlixService.byId(movieId).flatMap(fluxFlixService::crossTheStreams);
	}
}


@Service
class FluxFlixService {

	private final MovieRepository movieRepository;

	public Flux<Movie> all() {
		return this.movieRepository.findAll();
	}

	FluxFlixService(MovieRepository movieRepository) {
		this.movieRepository = movieRepository;
	}

	public Mono<Movie> byId(String id) {
		return this.movieRepository.findOne(id);
	}

	public Flux<Movie> byGenre(String genre) {
		return this.movieRepository.findByGenre(genre);
	}

	public Flux<MovieEvent> crossTheStreams(Movie movie) {
		Stream<MovieEvent> generate = Stream.generate(
				() -> new MovieEvent(movie, new Date(), Math.random() > .5 ? "jhoeller" : "jlong"));
		Flux<MovieEvent> streams = Flux.fromStream(generate);
		Flux<Long> intervals = Flux.interval(Duration.ofSeconds(1));
		return Flux.zip(streams, intervals).map(Tuple2::getT1);
	}
}

interface MovieRepository extends ReactiveMongoRepository<Movie, String> {

	Mono<Movie> findByTitle(String title);

	Flux<Movie> findByGenre(String genre);
}

@Data
@AllArgsConstructor
@NoArgsConstructor
class MovieEvent {

	private Movie movie;
	private Date when;
	private String user;
}

@Document
@Data
@AllArgsConstructor
@NoArgsConstructor
class Movie {

	@Id
	private String id;

	private String title, genre;
}