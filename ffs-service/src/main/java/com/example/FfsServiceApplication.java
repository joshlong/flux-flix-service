package com.example;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.java.Log;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.ReactiveAuthenticationManager;
import org.springframework.security.authentication.UserDetailsRepository;
import org.springframework.security.authentication.UserDetailsRepositoryAuthenticationManager;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.config.web.server.HttpSecurity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.web.server.authorization.AuthorizationContext;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import org.springframework.web.server.WebFilter;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.Arrays.asList;
import static org.springframework.web.reactive.function.server.RequestPredicates.GET;
import static org.springframework.web.reactive.function.server.RouterFunctions.route;
import static org.springframework.web.reactive.function.server.ServerResponse.ok;

@SpringBootApplication
public class FfsServiceApplication {

    @Bean
    RouterFunction<?> routerFunction(MovieHandler rh) {
        return route(GET("/movies"), rh::all)
                .andRoute(GET("/movies/{id}"), rh::byId)
                .andRoute(GET("/movies/{id}/events"), rh::events);
    }

    public static void main(String[] args) {
        SpringApplication.run(FfsServiceApplication.class, args);
    }
}


@Configuration
class SecurityConfiguration {

    public static final String AUTHORITY_ADMIN = "admin";
    public static final String AUTHORITY_USER = "stream";

    private Map<String, List<String>> users = new ConcurrentHashMap<String, List<String>>() {
        {
            put("sdeleuze", asList(AUTHORITY_ADMIN, AUTHORITY_USER));
            put("apoutsma", asList(AUTHORITY_ADMIN, AUTHORITY_USER));
            put("rwinch", asList(AUTHORITY_USER));
            put("mkheck", asList(AUTHORITY_ADMIN, AUTHORITY_USER));
            put("jlong", asList(AUTHORITY_USER));
        }
    };

    @Bean
    UserDetailsRepository userDetailsRepository() {
        return username -> Mono.justOrEmpty(users.get(username))
                .map(ignore -> {
                    List<String> authorities = users.get(username);
                    List<SimpleGrantedAuthority> grantedAuthorities = authorities
                            .stream()
                            .map(SimpleGrantedAuthority::new)
                            .collect(Collectors.toList());
                    return new User(username, "password", grantedAuthorities);
                });

    }

    @Bean
    ReactiveAuthenticationManager reactiveAuthenticationManager() {
        return new UserDetailsRepositoryAuthenticationManager(userDetailsRepository());
    }

    @Bean
    WebFilter reactive(ReactiveAuthenticationManager manager) throws Exception {
        HttpSecurity http = HttpSecurity.http();
        http.authenticationManager(manager).httpBasic();
        http.authorizeExchange().antMatchers("/**").access(this::authorize);
        return http.build();
    }

    private Mono<AuthorizationDecision> authorize(Mono<Authentication> authentication, AuthorizationContext ctx) {
        return authentication
                .map(auth ->
                        auth.getAuthorities()
                                .stream()
                                .anyMatch(ga -> ga.getAuthority().equalsIgnoreCase(AUTHORITY_USER)))
                .map(AuthorizationDecision::new);
    }
}

@Component
class MovieHandler {

    private final FluxFlixService ffs;

    MovieHandler(FluxFlixService ffs) {
        this.ffs = ffs;
    }

    Mono<ServerResponse> all(ServerRequest request) {
        return ok().body(ffs.all(), Movie.class);
    }

    Mono<ServerResponse> byId(ServerRequest request) {
        return ok().body(ffs.byId(request.pathVariable("id")), Movie.class);
    }

    Mono<ServerResponse> events(ServerRequest request) {
        return ok()
                .contentType(MediaType.TEXT_EVENT_STREAM)
                .body(ffs.streamStreams(request.pathVariable("id")), MovieEvent.class);
    }
}

@Data
@AllArgsConstructor
@NoArgsConstructor
class MovieEvent {
    private Movie movie;
    private Date when;
}

/*

@RestController
@RequestMapping("/movies")
class FluxFlixRestController {

    private final FluxFlixService fluxFlixService;

    FluxFlixRestController(FluxFlixService fluxFlixService) {
        this.fluxFlixService = fluxFlixService;
    }

    @GetMapping(value = "/{id}/events", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    Flux<MovieEvent> crossTheStreams(@PathVariable String id) {
        return fluxFlixService.streamStreams(id);
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
*/

@Service
class FluxFlixService {

    private final MovieRepository movieRepository;

    FluxFlixService(MovieRepository movieRepository) {
        this.movieRepository = movieRepository;
    }

    public Flux<MovieEvent> streamStreams(String movieId) {
        return byId(movieId).flatMapMany(movie -> {
            Flux<MovieEvent> eventFlux = Flux.fromStream(Stream.generate(() -> new MovieEvent(movie, new Date())));
            return eventFlux.delayElements(Duration.ofSeconds(1));
        });
    }

    public Flux<Movie> all() {
        return movieRepository.findAll();
    }

    public Mono<Movie> byId(String id) {
        return movieRepository.findById(id);
    }
}

@Log
@Component
class MovieDataCLR implements CommandLineRunner {

    private final MovieRepository movieRepository;

    MovieDataCLR(MovieRepository movieRepository) {
        this.movieRepository = movieRepository;
    }

    @Override
    public void run(String... strings) throws Exception {
        this.movieRepository
                .deleteAll()
                .subscribe(null, null, () ->
                        Stream.of("Flux Gordon", "Enter the Mono<Void>", "Back to the Future", "AEon Flux")
                                .map(title -> new Movie(UUID.randomUUID().toString(), title))
                                .forEach(movie -> movieRepository.save(movie)
                                        .subscribe(m -> log.info(m.toString()))));
    }
}

interface MovieRepository extends ReactiveMongoRepository<Movie, String> {
}

@AllArgsConstructor
@Data
@Document
class Movie {
    @Id
    private String id;
    private String title;
}