package com.example;

import com.mongodb.reactivestreams.client.MongoClients;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.data.mongo.MongoDataAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.config.AbstractReactiveMongoConfiguration;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.data.mongodb.repository.config.EnableReactiveMongoRepositories;
import org.springframework.http.codec.ServerSentEvent;
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


@SpringBootApplication(exclude = MongoDataAutoConfiguration.class)
public class FluxFlixServiceApplication {

	@Bean
	CommandLineRunner demoData(MovieRepository movieRepository) {
		return args -> {
			movieRepository.deleteAll().block();
			Flux<Movie> flux = Flux.fromStream(Stream.of("Y Tu Mono Tambien", "Flux Gordon", "Silence of the Lambdas", "FluxMan vs Superman",
					"The Fluxinator", "Enter the Mono<Void>", "Hot Flux", "Streaming Las Vegas",
					"Flux to the Future", "AEon Flux", "The Streaming", "Flux and Furious", "The Mighty Flux")
					.map(title -> new Movie(title, randomGenre())));
			movieRepository.save(flux).subscribe(System.out::println);
		};
	}

	private String randomGenre() {
		String genres[] = "drama,romcom,documentary,horror,comedy".split(",");
		return genres[new Random().nextInt(genres.length)];
	}

	public static void main(String[] args) {
		SpringApplication.run(FluxFlixServiceApplication.class, args);
	}
}

@RestController
@RequestMapping("/movies")
class MovieRestController {

	private final FluxFlixService fluxFlixService;
	private final MovieRepository movieRepository;

	public MovieRestController(FluxFlixService fluxFlixService, MovieRepository movieRepository) {
		this.fluxFlixService = fluxFlixService;
		this.movieRepository = movieRepository;
	}

	@GetMapping
	public Flux<Movie> all() {
		return this.movieRepository.findAll();
	}

	@GetMapping("/{id}/streams")
	public Flux<ServerSentEvent<MovieStream>> streamsFor(@PathVariable String id) {
		return this.movieRepository.findOne(id)
				.flatMap(this.fluxFlixService::streamStreamsFor)
				.map(m -> ServerSentEvent.builder(m).build());
	}

	@GetMapping("/{id}")
	public Mono<Movie> byId(@PathVariable String id) {
		return this.movieRepository.findOne(id);
	}
}

@Service
class FluxFlixService {

	private final MovieRepository movieRepository;

	public FluxFlixService(MovieRepository movieRepository) {
		this.movieRepository = movieRepository;
	}

	public Flux<MovieStream> streamStreamsFor(Movie movie) {
		Flux<Long> durationFlux = Flux.interval(Duration.ofSeconds(1));
		Flux<MovieStream> streamFlux = Flux.fromStream(Stream.generate(() -> new MovieStream("starbuxman", movie, new Date())));
		return Flux.zip(durationFlux, streamFlux).map(Tuple2::getT2);
	}

	public Mono<Movie> findByTitle(String title) {
		return this.movieRepository.findByTitle(title);
	}

	public Flux<Movie> findRelatedMovies(Movie m) {
		return this.movieRepository.findByGenre(m.getGenre());
	}
}

interface MovieRepository extends ReactiveMongoRepository<Movie, String> {
	Mono<Movie> findByTitle(String title);

	Flux<Movie> findByGenre(String genre);
}

@Configuration
@EnableReactiveMongoRepositories
class ReactiveMongoConfiguration extends AbstractReactiveMongoConfiguration {

	@Override
	public com.mongodb.reactivestreams.client.MongoClient mongoClient() {
		return MongoClients.create();
	}

	@Override
	protected String getDatabaseName() {
		return "movies";
	}
}

@Data
@NoArgsConstructor
@AllArgsConstructor
class MovieStream {
	private String user;
	private Movie movie;
	private Date when;
}

@Data
@Document
@NoArgsConstructor
@AllArgsConstructor
class Movie {

	@Id
	private String id;
	private String genre, title;

	public Movie(String title, String genre) {
		this.genre = genre;
		this.title = title;
	}
}