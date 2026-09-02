package me.project.apigateway.config;

import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RouteConfig {

    @Bean
    public RouteLocator customRouteLocator(RouteLocatorBuilder builder) {
        return builder.routes()
                // Route pour Auth Service
                .route("auth-service", r -> r
                        .path("/api/auth/**")
                        .filters(f -> f.stripPrefix(0))
                        .uri("lb://AUTH-SERVICE"))  // ← MAJUSCULES

                // Route pour User Service
                .route("user-service", r -> r
                        .path("/api/users/**")
                        .filters(f -> f.stripPrefix(0))
                        .uri("lb://USER-SERVICE"))  // ← MAJUSCULES

                // Route pour Ticket Service
                .route("ticket-service", r -> r
                        .path("/api/tickets/**")
                        .filters(f -> f.stripPrefix(0))
                        .uri("lb://TICKET-SERVICE"))  // ← MAJUSCULES

                // Route pour Notification Service
                .route("notification-service", r -> r
                        .path("/api/notifications/**")
                        .filters(f -> f.stripPrefix(0))
                        .uri("lb://NOTIFICATION-SERVICE"))  // ← MAJUSCULES

                .build();
    }
}