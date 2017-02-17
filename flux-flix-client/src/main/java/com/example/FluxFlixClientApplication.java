package com.example;

import lombok.Data;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriTemplate;

import java.util.Date;

@SpringBootApplication
public class FluxFlixClientApplication {


	@Bean
	WebClient webClient() {
		return WebClient.create();
	}

	@Bean
	CommandLineRunner client(WebClient webClient) {

		return arrrrgImAPirate ->
				webClient
						.get()
						.uri("http://localhost:8080/movies")
						.exchange()
						.flatMap(moviesCR -> moviesCR.bodyToFlux(Movie.class))
						.filter(m -> m.getTitle().equalsIgnoreCase("Flux Gordon"))
						.subscribe(fluxGordonMovie ->
								webClient
										.get()
										.uri(new UriTemplate("http://localhost:8080/movies/{id}/events").expand(fluxGordonMovie.getId()))
										.exchange()
										.flatMap(cr -> cr.bodyToFlux(MovieEvent.class)).subscribe(System.out::println));
	}

	public static void main(String[] args) {
		SpringApplication.run(FluxFlixClientApplication.class, args);
	}
}

@Data
class MovieEvent {
	private Date date;
	private Movie movie;
	private String user;
}

@Data
class Movie {
	private String genre, id, title;
}