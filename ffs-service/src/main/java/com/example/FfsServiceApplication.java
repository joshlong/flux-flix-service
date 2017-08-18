package com.example;

import lombok.*;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.HttpSecurity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.MapUserDetailsRepository;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsRepository;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.function.server.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuple2;

import java.time.Duration;
import java.util.*;
import java.util.stream.Stream;

import static org.springframework.web.reactive.function.server.RequestPredicates.*;

@SpringBootApplication
public class FfsServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(FfsServiceApplication.class, args);
    }
}

@Component
class UserHandler {
    private final UserDetailsRepository udr;

    UserHandler(UserDetailsRepository udr) {
        this.udr = udr;
    }

    Mono<ServerResponse> byUsername(ServerRequest request) {
        return ServerResponse.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(udr.findByUsername(request.pathVariable("username")), UserDetails.class);
    }

    Mono<ServerResponse> current(ServerRequest request) {
        return ServerResponse.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(request.principal()
                                .cast(Authentication.class)
                                .map(Authentication::getPrincipal)
                                .cast(UserDetails.class),
                        UserDetails.class
                );
    }
}

/*
@EnableWebFluxSecurity
class SecurityConfiguration {

    @Bean
    UserDetailsRepository userDetailsRepository() {
        return new MapUserDetailsRepository(user("rob").build(), user("josh").roles("USER","ADMIN").build());
    }

    private User.UserBuilder user(String username) {
        return User.withUsername(username).password("password").roles("USER");
    }

    @Bean
    SecurityWebFilterChain springSecurity(HttpSecurity http) {
        return http
                .authorizeExchange()
                    .pathMatchers("/users/me").authenticated()
                    .pathMatchers("/users/{username}").access((auth,context) ->
                        auth
                                .map( a-> a.getName().equals(context.getVariables().get("username")))
                                .map(AuthorizationDecision::new)
                    )
                    .anyExchange().hasRole("ADMIN")
                    .and()
                .build();
    }
}*/

@RestController
@RequestMapping("/movies")
class FluxFlixRestController {

    private final FluxFlixService fluxFlixService;

    FluxFlixRestController(FluxFlixService fluxFlixService) {
        this.fluxFlixService = fluxFlixService;
    }

    @GetMapping(value = "/{id}/events", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    Flux<MovieEvent> crossTheStreams(@PathVariable String id) {
        return fluxFlixService.events(id);
    }

    @GetMapping("/{id}")
    Mono<Movie> byId(@PathVariable String id) {
        return fluxFlixService.byId(id);
    }

    @GetMapping
    Flux<Movie> all() {
        return fluxFlixService.all();
    }

}

@Configuration
@EnableWebFluxSecurity
class SecurityConfiguration {

    @Bean
    UserDetailsRepository userDetailsRepository() {
        return new MapUserDetailsRepository(
                User.withUsername("jlong").roles("USER").password("password").build(),
                User.withUsername("rwinch").roles("ADMIN", "USER").password("password").build());
    }

    @Bean
    SecurityWebFilterChain securityWebFilterChain(HttpSecurity httpSecurity) {
        return httpSecurity
                .authorizeExchange()
                .anyExchange().hasRole("ADMIN").and()
                .build();

    }
}


@Component
class DataAppInitializr {

    private final MovieRepository movieRepository;

    @org.springframework.context.event.EventListener(ApplicationReadyEvent.class)
    public void run(ApplicationReadyEvent evt) {

        this.movieRepository
                .deleteAll()
                .thenMany(
                        Flux
                                .just("Foo", "Bar")
                                .flatMap(title -> this.movieRepository.save(new Movie(title))))
                .subscribe(null, null,
                        () -> this.movieRepository.findAll().subscribe(System.out::println));


    }

    DataAppInitializr(MovieRepository movieRepository) {
        this.movieRepository = movieRepository;
    }

}


@Configuration
class WebConfiguration {

    @Bean
    RouterFunction<?> routes(FluxFlixService ffs, UserHandler uh) {
        return RouterFunctions
                
                .route(GET("/movies"),
                        serverRequest -> ServerResponse.ok().body(ffs.all(), Movie.class))
                .andRoute(GET("/movies/{id}"),
                        serverRequest -> ServerResponse.ok().body(ffs.byId(serverRequest.pathVariable("id")), Movie.class))
                .andRoute(GET("/movies/{id}/events"), serverRequest ->
                        ServerResponse.ok()
                                .contentType(MediaType.TEXT_EVENT_STREAM)
                                .body(ffs.events(serverRequest.pathVariable("id")), MovieEvent.class))
                 .andRoute(GET("/users/me"), uh::current)
                 .andRoute(GET("/users/{username}"), uh::byUsername);
    }
}

@Service
class FluxFlixService {
    private final MovieRepository movieRepository;

    FluxFlixService(MovieRepository movieRepository) {
        this.movieRepository = movieRepository;
    }

    public Flux<MovieEvent> events(String movieId) {
        return Flux.<MovieEvent>generate(sink -> sink.next(new MovieEvent(movieId, new Date())))
                .delayElements(Duration.ofSeconds(1));
    }

    public Mono<Movie> byId(String id) {
        return this.movieRepository.findById(id);
    }

    public Flux<Movie> all() {
        return this.movieRepository.findAll();
    }
}

interface MovieRepository extends ReactiveMongoRepository<Movie, String> {
}

@Document
@Data
@NoArgsConstructor
@RequiredArgsConstructor
class Movie {
    private String id;
    @NonNull
    private String title;
}

@Data
@NoArgsConstructor
@AllArgsConstructor
class MovieEvent {
    private String movieId;
    private Date date;
}
