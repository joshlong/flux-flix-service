package com.example.fluxflixservice;

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
    private MovieService service;

    @Test
    public void getEventsTakeSadness() {
        Movie movie = service.all().blockFirst();

        StepVerifier.withVirtualTime(() -> service.events(movie.getId()).take(10).collect(Collectors.toList()))
                .thenAwait(Duration.ofHours(1000))
                .consumeNextWith(list -> Assert.assertTrue(list.size() == 10))
                .verifyComplete();
    }

    @Test
    public void getEventsTake10Works() {
        Movie movie = service.all().blockFirst();


        StepVerifier.withVirtualTime(() -> service.eventsByMovie(movie).take(10).collect(Collectors.toList()))
                .thenAwait(Duration.ofHours(1000))
                .consumeNextWith(list -> Assert.assertTrue(list.size() == 10))
                .verifyComplete();
    }

}
