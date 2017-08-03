package com.example.fluxflixservice;

import lombok.*;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.http.MediaType;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.authorization.ReactiveAuthorizationManager;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.HttpSecurity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.MapUserDetailsRepository;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsRepository;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.authorization.AuthorizationContext;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.server.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuple2;

import java.time.Duration;
import java.util.Date;

import static org.springframework.web.reactive.function.server.RequestPredicates.*;
import static org.springframework.web.reactive.function.server.RouterFunctions.*;

@SpringBootApplication
public class FluxFlixServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(FluxFlixServiceApplication.class, args);
    }
}

@Configuration
@EnableWebFluxSecurity
class SecurityConfiguration {

    private UserDetails user(String userName) {
        return User.withUsername(userName).authorities("USER").password("password")
                .build();
    }

    private UserDetails admin(String userName) {
        return User.withUsername(userName).authorities("USER", "ADMIN").password("password")
                .build();
    }

    @Bean
    UserDetailsRepository userDetailsRepository() {
        return new MapUserDetailsRepository(
                user("jlong"), user("fm"),
                admin("rwinch"), admin("mkheck")
        );
    }

    @Bean
    SecurityWebFilterChain securityFilterChain(HttpSecurity httpSecurity) {
        return httpSecurity.authorizeExchange()
                .anyExchange()
                .access((authentication, context) -> authentication
                        .map(auth -> new AuthorizationDecision(!auth.getName().equalsIgnoreCase("jlong"))))
                .and()
                .build();
    }

}

@Configuration
class WebConfiguration {

    @Bean
    RouterFunction<?> routes(MovieHandler movieHandler) {
        return route(GET("/movies"), movieHandler::all)
                .andRoute(GET("/movies/{id}"), movieHandler::byId)
                .andRoute(GET("/movies/{id}/events"), movieHandler::events);
    }
}

@Component
class MovieHandler {
    private final MovieService service;

    MovieHandler(MovieService service) {
        this.service = service;
    }

    public Mono<ServerResponse> all(ServerRequest request) {
        return ServerResponse.ok()
                .body(service.all(), Movie.class);
    }

    public Mono<ServerResponse> byId(ServerRequest request) {
        return ServerResponse.ok()
                .body(service.byId(request.pathVariable("id")), Movie.class);
    }

    public Mono<ServerResponse> events(ServerRequest request) {
        return ServerResponse.ok()
                .contentType(MediaType.TEXT_EVENT_STREAM)
                .body(service.events(request.pathVariable("id")), MovieEvent.class);
    }
}

@Component
class MovieInitializer implements ApplicationRunner {

    private final MovieRepository mr;

    MovieInitializer(MovieRepository mr) {
        this.mr = mr;
    }

    @Override
    public void run(ApplicationArguments applicationArguments) throws Exception {

        mr.deleteAll().
                thenMany(Flux.just("Silence of the Lambdas", "Back to the Future", "AEon Flux",
                        "Enter the Mono<Void>", "The Fluxxinator", "Meet the Fluxxers",
                        "12 Monos", "Y Tu Mono Tambien", "Somethings got to Flux",
                        "Fluxxing Tiger, Hidden Lambda",
                        "Lambda Lambda Lambda",
                        "One Flux over the Cuckoo's Nest")
                        .map(Movie::new)
                        .flatMap(mr::save))
                .subscribe(null, null,
                        () -> mr.findAll().subscribe(System.out::println));

    }
}


//@RestController
//@RequestMapping("/movies")
//class MovieController {
//    private final MovieService movieService;
//
//    MovieController(MovieService movieService) {
//        this.movieService = movieService;
//    }
//
//    @GetMapping
//    public Flux<Movie> all() {
//        return movieService.all();
//    }
//
//    @GetMapping("/{id}")
//    public Mono<Movie> byId(@PathVariable String id) {
//        return movieService.byId(id);
//    }
//
//    @GetMapping(value = "/{id}/events", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
//    public Flux<MovieEvent> crossTheStreams(@PathVariable String id) {
//        return movieService.byId(id)
//                .flatMapMany(movieService::eventsByMovie);
//    }
//}

@Service
class MovieService {

    public MovieService(MovieRepository mr) {
        this.mr = mr;
    }

    private final MovieRepository mr;

    Flux<Movie> all() {
        return mr.findAll();
    }

    Mono<Movie> byId(String id) {
        return mr.findById(id);
    }

    // this tests correctly
    Flux<MovieEvent> eventsByMovie(Movie movie) {
        return Flux.<MovieEvent>generate(sink -> sink.next(new MovieEvent(new Date(), movie)))
                .delayElements(Duration.ofSeconds(1));
    }

    Flux<MovieEvent> events(String id) {
        return Mono.just(new Movie("hi")).flatMapMany(movie -> {
            Flux<MovieEvent> movieEvents = Flux.generate(
                    sink -> sink.next(new MovieEvent(new Date(), movie)));
            Flux<Long> interval = Flux.interval(Duration.ofSeconds(1));
            Flux<Tuple2<MovieEvent, Long>> tuple2Flux = Flux.zip(movieEvents, interval);
            return tuple2Flux.map(Tuple2::getT1);
        });
    }


}

@Data
@AllArgsConstructor
@NoArgsConstructor
class MovieEvent {

    private Date when;
    private Movie movie;
}

interface MovieRepository extends ReactiveMongoRepository<Movie, String> {
}

@NoArgsConstructor
@Document
@RequiredArgsConstructor
@Data
class Movie {

    @Id
    private String id;

    @NonNull
    private String title;
}