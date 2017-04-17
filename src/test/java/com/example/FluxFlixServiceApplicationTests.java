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
	private FluxFlixService fluxFlixService;

    @Test
    public void findMovieEventsTake10() {
        Movie movie = fluxFlixService.findAllMovies().blockFirst();

        StepVerifier.withVirtualTime(() -> this.fluxFlixService.findMovieEvents(movie).take(10).collect(Collectors.toList()))
                .thenAwait(Duration.ofHours(10))
                .consumeNextWith(list -> Assert.assertTrue(list.size() == 10))
                .verifyComplete();
    }

}
