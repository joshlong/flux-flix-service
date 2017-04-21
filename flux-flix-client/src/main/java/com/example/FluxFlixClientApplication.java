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
public class FluxFlixClientApplication {
	@Bean
	WebClient client() {
		return WebClient.create("http://localhost:8080/movies");
	}

	@Bean
	CommandLineRunner demo(WebClient client) {
		return strings -> {
			client.get()
					.uri("")
					.exchange()
					.flatMapMany(clientResponse -> clientResponse.bodyToFlux(Movie.class))
					.filter(movie -> movie.getTitle().equalsIgnoreCase("aeon flux"))
					.subscribe(movie -> client.get()
							.uri("/{id}/events", movie.getId())
							.exchange()
							.flatMapMany(clientResponse -> clientResponse.bodyToFlux(MovieEvent.class))
							.subscribe(System.out::println));
		};
	}

	public static void main(String[] args) {
		SpringApplication.run(FluxFlixClientApplication.class, args);
	}
}

@Data
@AllArgsConstructor
class Movie {
	private String id, title, genre;
}

@Data
@AllArgsConstructor
class MovieEvent {
	private String user;
	private Movie movie;
	private Date date;
}