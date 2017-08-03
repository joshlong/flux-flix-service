package com.example.fluxflixclient;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.ExchangeFilterFunctions;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Date;

@SpringBootApplication
public class FluxFlixClientApplication {
    @Bean
    WebClient client() {
        return WebClient.create("http://localhost:8080/movies")
                .mutate()
                .filter(ExchangeFilterFunctions.basicAuthentication("rwinch", "password"))
                .build();
    }

    @Bean
    CommandLineRunner demo(WebClient client) {
        return strings -> {
            client.get()
                    .retrieve()
                    .bodyToFlux(Movie.class)
                    .filter(movie -> movie.getTitle().equalsIgnoreCase("silence of the lambdas"))
                    .flatMap(movie -> client.get()
                        .uri("/{id}/events", movie.getId())
                        .retrieve()
                        .bodyToFlux(MovieEvent.class))
                    .subscribe(System.out::println);
        };
    }

    public static void main(String[] args) {
        SpringApplication.run(FluxFlixClientApplication.class, args);
    }
}

@Data
@NoArgsConstructor
@AllArgsConstructor
class Movie {
    private String id, title;
}

@Data
@NoArgsConstructor
@AllArgsConstructor
class MovieEvent {
    private Date when;
    private Movie movie;
}