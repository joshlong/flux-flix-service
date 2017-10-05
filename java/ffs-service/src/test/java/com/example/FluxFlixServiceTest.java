package com.example;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;
import reactor.test.StepVerifier;

import java.time.Duration;

@RunWith(SpringRunner.class)
@SpringBootTest
public class FluxFlixServiceTest {
    @Autowired
    FluxFlixService fluxFlixService;

    @Test
    public void eventsTake10() throws Exception {
        Movie movie = this.fluxFlixService.all().blockFirst();

        StepVerifier.withVirtualTime(
                () -> this.fluxFlixService.events(movie.getId())
                        .take(10)
                        .collectList())
                .thenAwait(Duration.ofHours(1))
                .consumeNextWith(list -> Assert.assertTrue(list.size() == 10))
                .verifyComplete();
    }
}