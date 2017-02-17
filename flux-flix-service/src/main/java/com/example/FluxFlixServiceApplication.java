package com.example;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuple2;

import java.time.Duration;
import java.util.Date;
import java.util.Random;
import java.util.stream.Stream;

@SpringBootApplication
public class FluxFlixServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(FluxFlixServiceApplication.class, args);
	}
}

@Component
class MovieDataCLR implements CommandLineRunner {

	private final MovieRepository movieRepository;

	MovieDataCLR(MovieRepository movieRepository) {
		this.movieRepository = movieRepository;
	}

	private String randomGenre() {
		String types[] = "romcom,documentary,horror,family,drama".split(",");
		return types[new Random().nextInt(types.length)];
	}

	private Movie randomMovie(String title) {
		return new Movie(title, randomGenre());
	}

	@Override
	public void run(String... strings) throws Exception {

		movieRepository.deleteAll().block();

		Stream<String> titles = Stream.of("Y Tu Mono Tambien", "The Mighty Flux", "Meet the Fluxers", "Fluxman vs Superman",
				"Flux to the Future", "Enter the Mono<Void>", "The Streaming", "AEon Flux", "the Flux and the Furious",
				"Flux Gordon", "Silence of the Lambdas");

		Flux<Movie> titlesFlux = Flux.fromStream(titles).map(this::randomMovie);

		movieRepository.save(titlesFlux).subscribe(System.out::println);
	}
}

@RestController
@RequestMapping("/movies")
class MovieRestController {

	private final MovieRepository movieRepository;
	private final FluxFlixService fluxFlixService;

	MovieRestController(
			MovieRepository movieRepository,
			FluxFlixService fluxFlixService) {
		this.movieRepository = movieRepository;
		this.fluxFlixService = fluxFlixService;
	}


	@GetMapping("/{id}")
	public Mono<Movie> id(@PathVariable String id) {
		return this.movieRepository.findOne(id);
	}

	@GetMapping(value = "/{id}/events", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
	public Flux<MovieEvent> events(@PathVariable String id) {
		return this.movieRepository.findOne(id).flatMap(fluxFlixService::streamStreams);
	}

	@GetMapping
	public Flux<Movie> all() {
		return this.movieRepository.findAll();
	}
}

@Service
class FluxFlixService {

	private final MovieRepository movieRepository;

	FluxFlixService(MovieRepository movieRepository) {
		this.movieRepository = movieRepository;
	}

	public Flux<MovieEvent> streamStreams(Movie movie) {
		Flux<Long> interval = Flux.interval(Duration.ofSeconds(1));
		Flux<MovieEvent> continuousMovies = Flux.fromStream(
				Stream.generate(() -> new MovieEvent(movie, Math.random() > .5 ? "dsyer" : "pwebb", new Date())));
		return Flux.zip(interval, continuousMovies).map(Tuple2::getT2);
	}

	public Flux<Movie> relatedMovies(Movie movie) {
		return this.movieRepository.findByGenre(movie.getGenre());
	}

	public Mono<Movie> byTitle(String title) {
		return this.movieRepository.findByTitle(title);
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
	private String user;
	private Date date;
}


@Data
@AllArgsConstructor
@NoArgsConstructor
@Document
class Movie {

	@Id
	private String id;
	private String title, genre;

	Movie(String t, String g) {
		this.title = t;
		this.genre = g;
	}
}