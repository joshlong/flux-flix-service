package com.example.fluxflixclient;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.web.reactive.function.client.ExchangeFilterFunctions;
import org.springframework.web.reactive.function.client.ExchangeFunctions;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Date;

@SpringBootApplication
public class FluxFlixClientApplication {

    @Bean
    WebClient webClient() {
        return WebClient
                .create("http://localhost:8080/movies")
                .mutate()
                .filter(ExchangeFilterFunctions.basicAuthentication("jlong", "pw"))
                .build();
    }

    @Bean
    ApplicationRunner runner(WebClient wc) {
        return args -> wc.get()
                .uri("")
                .retrieve()
                .bodyToFlux(Movie.class)
                .filter(m -> m.getTitle().equalsIgnoreCase("Silence of the Lambdas"))
                .flatMap(m -> wc.get().uri("/{id}/events", m.getId())
                        .retrieve()
                        .bodyToFlux(MovieEvent.class))
                .subscribe(System.out::println);
    }

    public static void main(String[] args) {
        SpringApplication.run(FluxFlixClientApplication.class, args);
    }
}

@Data
@NoArgsConstructor
@AllArgsConstructor
class MovieEvent {
    private String movieId;
    private Date date;
}

@Data
@AllArgsConstructor
@NoArgsConstructor
class Movie {
    private String id;
    private String title;
}