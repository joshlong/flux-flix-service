package com.example;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.java.Log;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.web.reactive.function.client.ExchangeFilterFunctions;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Date;

@Log
@SpringBootApplication
public class FfsClientApplication {

    @Bean
    WebClient webClient() {
        return WebClient
                .create("http://localhost:8080/movies")
                .mutate()
                .filter(ExchangeFilterFunctions.basicAuthentication("jlong", "password"))
                .build();
    }

    @Bean
    CommandLineRunner demo(WebClient client) {
        return strings ->
                client
                        .get()
                        .uri("")
                        .retrieve()
                        .bodyToFlux(Movie.class)
                        .filter(movie -> movie.getTitle().equalsIgnoreCase("aeon flux"))
                        .flatMap(movie ->
                                client.get()
                                        .uri("/{id}/events", movie.getId())
                                        .retrieve()
                                        .bodyToFlux(MovieEvent.class))
                        .subscribe(movieEvent -> log.info(movieEvent.toString()));
    }

    public static void main(String[] args) {
        SpringApplication.run(FfsClientApplication.class, args);
    }
}


@Data
@AllArgsConstructor
class MovieEvent {
    private Movie movie;
    private Date when;
}

@Data
@AllArgsConstructor
class Movie {
    private String id;
    private String title;
}