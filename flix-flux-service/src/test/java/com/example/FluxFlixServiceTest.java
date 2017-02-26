package com.example;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;
import reactor.test.StepVerifier;

import java.time.Duration;
import java.util.stream.Collectors;

@SpringBootTest
@RunWith(SpringRunner.class)
public class FluxFlixServiceTest {

	private Movie movie;

	@Autowired
	private FluxFlixService ffs;

	@Before
	public void setUp() throws Throwable {
		this.movie = this.ffs.all().blockFirst();
	}

	@Test
	public void name() throws Exception {

		StepVerifier.withVirtualTime(() ->
				this.ffs.crossTheStreams(this.movie)
						.take(10)
						.collect(Collectors.toList()))
				.thenAwait(Duration.ofHours(10))
				.consumeNextWith(batch -> Assert.assertEquals(10, batch.size()))
				.verifyComplete();

	}
}