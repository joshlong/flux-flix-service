package com.example;

import lombok.Data;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpMethod;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriTemplate;

import java.net.URI;
import java.util.Date;

@SpringBootApplication
public class FluxFlixClientApplication {

    @Bean
    WebClient webClient() {
        return WebClient.create(new ReactorClientHttpConnector());
    }

    @Bean
    CommandLineRunner demo(WebClient client) {
        return args ->
                client.exchange(ClientRequest.method(HttpMethod.GET, new URI("http://localhost:8080/movies")).build())
                        .flatMap(m -> m.bodyToFlux(Movie.class))
                        .filter(m -> m.getTitle().equalsIgnoreCase("flux gordon"))
                        .subscribe(m -> client.exchange(ClientRequest.method(HttpMethod.GET,
                                new UriTemplate("http://localhost:8080/movies/{id}/events")
                                        .expand(m.getId())).build())
                                .subscribe(cr -> cr.bodyToFlux(MovieEvent.class)
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