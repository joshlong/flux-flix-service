package com.example;

import lombok.Data;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Date;

import static org.springframework.web.reactive.function.client.ClientRequest.GET;

@SpringBootApplication
public class FluxFlixClientApplication {

	@Bean
	WebClient webClient() {
		return WebClient.create(new ReactorClientHttpConnector());
	}

	@Bean
	CommandLineRunner demo(WebClient client) {
		return args ->
				client.exchange(GET("http://localhost:8080/movies").build())
						.flatMap(m -> m.bodyToFlux(Movie.class))
						.filter(m -> m.getTitle().equalsIgnoreCase("flux gordon"))
						.subscribe(m -> client.exchange(GET("http://localhost:8080/movies/{id}/streams", m.getId()).build())
								.subscribe(cr -> cr.bodyToFlux(MovieStream.class).subscribe(System.out::println)));

	}

	public static void main(String[] args) {
		SpringApplication.run(FluxFlixClientApplication.class, args);
	}
}

@Data
class MovieStream {
	private Date when;
	private Movie movie;
	private String user;
}

@Data
class Movie {
	private String id, genre, title;
}