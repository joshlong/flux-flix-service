package com.example;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;
import reactor.test.StepVerifier;

import java.time.Duration;
import java.util.stream.Collectors;

@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
public class FluxFlixServiceApplicationTests {

	@Autowired
	private MovieRepository movieRepository;

	@Autowired
	private FluxFlixService service;

	@Test
	public void getMovieEvents() throws Exception {
		Movie movie = this.movieRepository.findAll().blockFirst();
		StepVerifier.withVirtualTime(() -> service.getMovieEvents(movie).take(10).collect(Collectors.toList()))
				.thenAwait(Duration.ofMinutes(10))
				.consumeNextWith(movieEvents -> Assert.assertTrue(movieEvents.size() == 10))
				.verifyComplete();
	}
}
