package com.example;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
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
        return WebClient.create("http://localhost:8080/movies")
                .filter(ExchangeFilterFunctions.basicAuthentication("jlong", "password"));
    }

    @Bean
    CommandLineRunner demo(WebClient webClient) {

        return args ->
                webClient
                        .get()
                        .uri("")
                        .retrieve()
                        .bodyToFlux(Movie.class)
                        .filter(movie -> movie.getTitle().equalsIgnoreCase("aeon flux"))
                        .subscribe(movie ->
                                webClient
                                        .get()
                                        .uri("/{id}/events", movie.getId())
                                        .retrieve()
                                        .bodyToFlux(MovieEvent.class)
                                        .subscribe(movieEvent -> log.info(movieEvent.toString())));

    }

    public static void main(String[] args) {
        SpringApplication.run(FfsClientApplication.class, args);
    }
}


@Data
@AllArgsConstructor
@NoArgsConstructor
class MovieEvent {
    private Movie movie;
    private Date when;
}

@AllArgsConstructor
@Data
class Movie {
    private String title;
    private String id;
}