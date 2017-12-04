package com.example.mc

import org.reactivestreams.Publisher
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.builder.SpringApplicationBuilder
import org.springframework.cloud.gateway.filter.factory.RequestRateLimiterGatewayFilterFactory
import org.springframework.cloud.gateway.filter.ratelimit.RedisRateLimiter
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder
import org.springframework.cloud.gateway.route.builder.filters
import org.springframework.cloud.gateway.route.builder.routes
import org.springframework.context.support.beans
import org.springframework.security.config.web.server.ServerHttpSecurity
import org.springframework.security.core.userdetails.MapReactiveUserDetailsService
import org.springframework.security.core.userdetails.User
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.bodyToFlux
import org.springframework.web.reactive.function.server.ServerResponse
import org.springframework.web.reactive.function.server.body
import org.springframework.web.reactive.function.server.router
import java.util.*

val myBeans = beans {

    bean {
        WebClient.create("http://localhost:8080/movies")
    }

    bean {
        router {
            val client = ref<WebClient>()
            GET("/titles") {
                val titles: Publisher<String> = client.get().retrieve().bodyToFlux<Movie>().map { it.title }
                ServerResponse.ok().body(titles)
            }
        }
    }

    bean {
        val user = User
                .withDefaultPasswordEncoder()
                .username("user")
                .password("pw")
                .roles("USER")
                .build()
        MapReactiveUserDetailsService(user)
    }
    bean {
        ref<ServerHttpSecurity>()
                .authorizeExchange()
                .pathMatchers("/rl").hasRole("USER")
                .anyExchange().permitAll()
                .and()
                .httpBasic()
                .and()
                .build()
    }
    bean {

        ref<RouteLocatorBuilder>().routes {
            route {
                path("/proxy")
                uri("http://localhost:8080/movies")
            }
            route {
                val rl = ref<RequestRateLimiterGatewayFilterFactory>()
                val gatewayFilter = rl.apply(RedisRateLimiter.args(5, 10))
                path("/rl")
                filters {
                    filter(gatewayFilter)
                }
                uri("http://localhost:8080/movies")
            }
        }

    }

}

class Movie(val id: String? = null, val title: String? = null)
class MovieEvent(val movieId: String? = null, val dataViewed: Date? = null)


@SpringBootApplication
class McApplication

fun main(args: Array<String>) {
    SpringApplicationBuilder()
            .initializers(myBeans)
            .sources(McApplication::class.java)
            .run(*args)
}
