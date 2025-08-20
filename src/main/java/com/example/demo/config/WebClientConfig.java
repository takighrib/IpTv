package com.example.demo.config;

import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

/**
 * Configuration WebClient optimisée pour les requêtes Xtream
 */
@Configuration
public class WebClientConfig {

    /**
     * WebClient configuré avec timeouts optimisés
     */
    @Bean
    public WebClient webClient() {
        HttpClient httpClient = HttpClient.create()
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 30_000)
                .responseTimeout(Duration.ofSeconds(60))
                .doOnConnected(conn ->
                        conn.addHandlerLast(new ReadTimeoutHandler(60, TimeUnit.SECONDS))
                                .addHandlerLast(new WriteTimeoutHandler(30, TimeUnit.SECONDS)));

        return WebClient.builder()
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .exchangeStrategies(ExchangeStrategies.builder()
                        .codecs(configurer -> configurer
                                .defaultCodecs()
                                .maxInMemorySize(50 * 1024 * 1024)) // 50MB
                        .build())
                .build();
    }

    /**
     * WebClient spécialement configuré pour les TRÈS gros fichiers M3U (jusqu'à 200MB)
     */
    @Bean("m3uWebClient")
    public WebClient m3uWebClient() {
        HttpClient httpClient = HttpClient.create()
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 60_000) // 1 minute
                .responseTimeout(Duration.ofMinutes(10)) // 10 minutes pour très gros fichiers
                .doOnConnected(conn ->
                        conn.addHandlerLast(new ReadTimeoutHandler(600, TimeUnit.SECONDS)) // 10 min
                                .addHandlerLast(new WriteTimeoutHandler(120, TimeUnit.SECONDS))); // 2 min

        return WebClient.builder()
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .exchangeStrategies(ExchangeStrategies.builder()
                        .codecs(configurer -> configurer
                                .defaultCodecs()
                                .maxInMemorySize(200 * 1024 * 1024)) // 200MB pour très gros M3U
                        .build())
                .build();
    }

    /**
     * WebClient pour les requêtes EPG (plus tolérant aux timeouts)
     */
    @Bean("epgWebClient")
    public WebClient epgWebClient() {
        HttpClient httpClient = HttpClient.create()
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 20_000)
                .responseTimeout(Duration.ofSeconds(45))
                .doOnConnected(conn ->
                        conn.addHandlerLast(new ReadTimeoutHandler(45, TimeUnit.SECONDS))
                                .addHandlerLast(new WriteTimeoutHandler(20, TimeUnit.SECONDS)));

        return WebClient.builder()
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .exchangeStrategies(ExchangeStrategies.builder()
                        .codecs(configurer -> configurer
                                .defaultCodecs()
                                .maxInMemorySize(10 * 1024 * 1024)) // 10MB suffisant pour EPG
                        .build())
                .build();
    }

    /**
     * WebClient ultra-light pour les APIs Xtream classiques
     */
    @Bean("xtreamWebClient")
    public WebClient xtreamWebClient() {
        HttpClient httpClient = HttpClient.create()
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 15_000)
                .responseTimeout(Duration.ofSeconds(30))
                .doOnConnected(conn ->
                        conn.addHandlerLast(new ReadTimeoutHandler(30, TimeUnit.SECONDS))
                                .addHandlerLast(new WriteTimeoutHandler(15, TimeUnit.SECONDS)));

        return WebClient.builder()
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .exchangeStrategies(ExchangeStrategies.builder()
                        .codecs(configurer -> configurer
                                .defaultCodecs()
                                .maxInMemorySize(5 * 1024 * 1024)) // 5MB pour APIs normales
                        .build())
                .build();
    }
}