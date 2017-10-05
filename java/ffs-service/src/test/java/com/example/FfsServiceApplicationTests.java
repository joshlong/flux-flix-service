package com.example;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.reactive.function.client.ExchangeFilterFunctions;

import java.util.Map;
import java.util.function.Consumer;

import static org.springframework.security.test.web.reactive.server.SecurityMockServerConfigurers.mockUser;
import static org.springframework.security.test.web.reactive.server.SecurityMockServerConfigurers.springSecurity;
import static org.springframework.web.reactive.function.client.ExchangeFilterFunctions.basicAuthentication;

@RunWith(SpringRunner.class)
@SpringBootTest
public class FfsServiceApplicationTests {

    @Autowired
    private ApplicationContext context;

    private WebTestClient client;

    @Before
    public void setup() {
        client = WebTestClient
                .bindToApplicationContext(context)
                .apply(springSecurity())
                .configureClient()
                .filter(basicAuthentication())
                .baseUrl("http://localhost:8080/")
                .build();
    }

    @Test
    public void getMoviesWhenNotAuthenticatedThenIsUnauthorized() {
        client
                .get()
                .uri("/movies/")
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    public void getMoviesWhenNotAdminThenIsForbidden() {
        client
                .get()
                .uri("/movies/")
                .attributes(joshsCredentials())
                .exchange()
                .expectStatus().isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    public void getMoviesWhenIsAdminThenIsOk() {
        client
                .get()
                .uri("/movies/")
                .attributes(robsCredentials())
                .exchange()
                .expectStatus().isOk();
    }

    @Test
    public void getMoviesWhenMockNotAdminThenIsForbidden() {
        client
                .mutateWith(mockUser())
                .get()
                .uri("/movies/")
                .exchange()
                .expectStatus().isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    public void getMoviesWhenMockIsAdminThenIsOk() {
        client
                .mutateWith(mockUser().roles("ADMIN"))
                .get()
                .uri("/movies/")
                .exchange()
                .expectStatus().isOk();
    }

    @Test
    public void getUsersRobWhenIsRobThenIsOk() {
        client
                .get()
                .uri("/users/rob")
                .attributes(robsCredentials())
                .exchange()
                .expectStatus().isOk();
    }

    @Test
    public void getUsersRobWhenIsJoshThenIsForbidden() {
        client
                .get()
                .uri("/users/rob")
                .attributes(joshsCredentials())
                .exchange()
                .expectStatus().isEqualTo(HttpStatus.FORBIDDEN)
                .expectBody().isEmpty();
    }

    @Test
    public void getUsersRobWhenNotAuthenticatedThenIsUnauthorized() {
        client
                .get()
                .uri("/users/rob")
                .exchange()
                .expectStatus().isUnauthorized()
                .expectBody().isEmpty();
    }

    @Test
    public void getUsersMeWhenIsRobThenIsOk() {
        client
                .get()
                .uri("/users/me")
                .attributes(robsCredentials())
                .exchange()
                .expectStatus().isOk();
    }

    @Test
    public void getUsersMeWhenIsJoshThenIsForbidden() {
        client
                .get()
                .uri("/users/me")
                .attributes(joshsCredentials())
                .exchange()
                .expectStatus().isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    public void getUsersMeWhenNotAuthenticatedThenIsUnauthorized() {
        client
                .get()
                .uri("/users/me")
                .exchange()
                .expectStatus().isUnauthorized()
                .expectBody().isEmpty();
    }

    private Consumer<Map<String, Object>> robsCredentials() {
        return ExchangeFilterFunctions.Credentials.basicAuthenticationCredentials("rwinch", "password");
    }

    private Consumer<Map<String, Object>> joshsCredentials() {
        return ExchangeFilterFunctions.Credentials.basicAuthenticationCredentials("jlong", "password");
    }
}
