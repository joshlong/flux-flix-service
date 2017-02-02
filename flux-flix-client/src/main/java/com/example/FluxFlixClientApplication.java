package com.example;

import lombok.Data;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriTemplate;

import java.net.URI;
import java.util.Date;

@SpringBootApplication
public class FluxFlixClientApplication {
    @Bean
    WebClient client() {
        return WebClient.create("/");
    }

    @Bean
    CommandLineRunner demo(WebClient client) {
        return args->client.get()
                .uri("http://localhost:8080/movies")
                .exchange()
                .flatMap(m->m.bodyToFlux(Movie.class))
                .filter(m->m.getTitle().equalsIgnoreCase("flux gordon"))
                .subscribe(m->client.get()
                        .uri(new UriTemplate("http://localhost:8080/movies/{id}/events").expand(m.getId()))
                        .exchange()
                        .subscribe(cr->cr.bodyToFlux(MovieEvent.class)
                                .subscribe(System.out::println)));
    }

    public static void main(String[] args) {
        SpringApplication.run(FluxFlixClientApplication.class, args);
    }
}

@Data
class MovieEvent {
    private Date when;
    private Movie movie;
    private String user;
}

@Data
class Movie {
    private String id, genre, title;
}