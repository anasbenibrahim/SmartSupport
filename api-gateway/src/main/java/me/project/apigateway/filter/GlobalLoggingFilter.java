package me.project.apigateway.filter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Component
public class GlobalLoggingFilter implements GlobalFilter, Ordered {

    private static final Logger log =
            LoggerFactory.getLogger(GlobalLoggingFilter.class);

    @Override
    public Mono<Void> filter(
            ServerWebExchange exchange,
            GatewayFilterChain chain) {

        ServerHttpRequest request = exchange.getRequest();

        long startTime = System.currentTimeMillis();

        log.info(
                "Incoming Request: {} {}",
                request.getMethod(),
                request.getURI()
        );

        log.info(
                "Query Params: {}",
                request.getQueryParams()
        );

        request.getHeaders().forEach((key, values) -> {

            // Ne jamais afficher le JWT dans les logs
            if (!key.equalsIgnoreCase("Authorization")) {
                log.info("Header: {} = {}", key, values);
            }
        });

        return chain.filter(exchange)
                .then(Mono.fromRunnable(() -> {

                    ServerHttpResponse response =
                            exchange.getResponse();

                    long duration =
                            System.currentTimeMillis() - startTime;

                    log.info(
                            "Response: {} | Duration: {} ms",
                            response.getStatusCode(),
                            duration
                    );
                }));
    }

    @Override
    public int getOrder() {
        return -1;
    }
}