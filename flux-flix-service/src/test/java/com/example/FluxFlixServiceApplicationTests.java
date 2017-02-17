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
@SpringBootTest
public class FluxFlixServiceApplicationTests {

	@Autowired
	private MovieRepository movieRepository;

	@Autowired
	private FluxFlixService fluxFlixService;

	@Test
	public void testStreamingStreamsFluxItAll() throws Throwable {

		Movie movie = this.movieRepository.findAll().blockFirst();

		StepVerifier.withVirtualTime(() -> fluxFlixService.streamStreams(movie).take(10).collect(Collectors.toList()))
				.thenAwait(Duration.ofMinutes(10))
				.consumeNextWith(batchOfEvents -> Assert.assertEquals(batchOfEvents.size(), 10))
				.verifyComplete();

	}

}
