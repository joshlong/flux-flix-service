package com.example;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.reactive.server.ExchangeMutatorWebFilter;
import org.springframework.test.web.reactive.server.WebTestClient;

import static org.springframework.security.test.web.reactive.server.SecurityExchangeMutators.withUser;
import static org.springframework.web.reactive.function.client.ExchangeFilterFunctions.basicAuthentication;

@RunWith(SpringRunner.class)
@SpringBootTest
public class FfsServiceApplicationTests {
	@Autowired
	ApplicationContext context;

	ExchangeMutatorWebFilter mutator = new ExchangeMutatorWebFilter();

	WebTestClient client;

	@Before
	public void setup() {
		client = WebTestClient
				.bindToApplicationContext(context)
				.webFilter(mutator)
				.configureClient()
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
				.filter(basicAuthentication("rob","password"))
				.get()
				.uri("/movies/")
				.exchange()
				.expectStatus().isEqualTo(HttpStatus.FORBIDDEN);
	}

	@Test
	public void getMoviesWhenIsAdminThenIsOk() {
		client
				.filter(basicAuthentication("josh","password"))
				.get()
				.uri("/movies/")
				.exchange()
				.expectStatus().isOk();
	}

	@Test
	public void getMoviesWhenMockNotAdminThenIsForbidden() {
		client
				.filter(mutator.perClient(withUser()))
				.get()
				.uri("/movies/")
				.exchange()
				.expectStatus().isEqualTo(HttpStatus.FORBIDDEN);
	}

	@Test
	public void getMoviesWhenMockIsAdminThenIsOk() {
		client
				.filter(mutator.perClient(withUser().roles("ADMIN")))
				.get()
				.uri("/movies/")
				.exchange()
				.expectStatus().isOk();
	}

	@Test
	public void getUsersRobWhenIsRobThenIsOk() {
		client
				.filter(basicAuthentication("rob","password"))
				.get()
				.uri("/users/rob")
				.exchange()
				.expectStatus().isOk();
	}

	@Test
	public void getUsersRobWhenIsJoshThenIsForbidden() {
		client
				.filter(basicAuthentication("josh","password"))
				.get()
				.uri("/users/rob")
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
				.filter(basicAuthentication("rob","password"))
				.get()
				.uri("/users/me")
				.exchange()
				.expectStatus().isOk();
	}

	@Test
	public void getUsersMeWhenIsJoshThenIsOk() {
		client
				.filter(basicAuthentication("josh","password"))
				.get()
				.uri("/users/me")
				.exchange()
				.expectStatus().isOk();
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
}
