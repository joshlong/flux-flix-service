package com.example;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Date;

@SpringBootApplication
public class FlixFluxClientApplication {

	@Bean
	WebClient client() {
		return WebClient.create();
	}

	@Bean
	CommandLineRunner demo(WebClient client) {
		return args -> {

			client.get().uri("http://localhost:8080/movies")
					.exchange()
					.flatMap(cr -> cr.bodyToFlux(Movie.class))
					.filter(movie -> movie.getTitle().equals("Flux Gordon"))
					.subscribe(
							movie -> client.get()
									.uri("http://localhost:8080/movies/{id}/events", movie.getId())
									.exchange()
									.flatMap(cr -> cr.bodyToFlux(MovieEvent.class))
									.subscribe(System.out::println));

		};
	}

	public static void main(String[] args) {
		SpringApplication.run(FlixFluxClientApplication.class, args);
	}
}


@Data
@AllArgsConstructor
@NoArgsConstructor
class MovieEvent {

	private Movie movie;
	private String user;
	private Date when;
}


@Data
@AllArgsConstructor
@NoArgsConstructor
class Movie {

	private String id;
	private String title, genre;
}