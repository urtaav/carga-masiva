package com.tech.apicargamasiva.config;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import io.github.resilience4j.retry.RetryRegistry;
import io.github.resilience4j.bulkhead.Bulkhead;
import io.github.resilience4j.bulkhead.BulkheadConfig;
import io.github.resilience4j.bulkhead.BulkheadRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.dao.DataAccessException;

import java.sql.SQLException;
import java.time.Duration;
import java.util.concurrent.TimeoutException;

@Configuration
@Slf4j
public class ResilienceConfig {

    /**
     * Configuración de Retry para importación
     */
    @Bean
    public RetryConfig importacionRetryConfig() {
        return RetryConfig.custom()
                .maxAttempts(3)
                .waitDuration(Duration.ofSeconds(3))
                .enableExponentialBackoff()
                .exponentialBackoffMultiplier(2)
                .retryExceptions(
                        SQLException.class,
                        DataAccessException.class,
                        TimeoutException.class
                )
                .ignoreExceptions(
                        IllegalArgumentException.class,
                        jakarta.validation.ValidationException.class
                )
                .failAfterMaxAttempts(true)
                .build();
    }

    @Bean
    public RetryRegistry retryRegistry(RetryConfig importacionRetryConfig) {
        RetryRegistry registry = RetryRegistry.of(importacionRetryConfig);

        // Crear retry instances
        Retry importacionRetry = registry.retry("importacionRetry", importacionRetryConfig);

        // Event listeners
        importacionRetry.getEventPublisher()
                .onRetry(event -> log.warn("Reintento {} de importación: {}",
                        event.getNumberOfRetryAttempts(),
                        event.getLastThrowable().getMessage()))
                .onSuccess(event -> log.info("Importación exitosa después de {} reintentos",
                        event.getNumberOfRetryAttempts()))
                .onError(event -> log.error("Importación falló después de {} reintentos",
                        event.getNumberOfRetryAttempts(),
                        event.getLastThrowable()));

        log.info("✅ RetryRegistry configurado con 'importacionRetry'");

        return registry;
    }

    /**
     * Configuración de Circuit Breaker
     */
    @Bean
    public CircuitBreakerConfig circuitBreakerConfig() {
        return CircuitBreakerConfig.custom()
                .failureRateThreshold(50) // 50% de fallos abre el circuito
                .slowCallRateThreshold(80) // 80% de llamadas lentas abre el circuito
                .slowCallDurationThreshold(Duration.ofSeconds(60))
                .permittedNumberOfCallsInHalfOpenState(10)
                .slidingWindowSize(100)
                .minimumNumberOfCalls(10)
                .waitDurationInOpenState(Duration.ofMinutes(1))
                .automaticTransitionFromOpenToHalfOpenEnabled(true)
                .recordExceptions(
                        SQLException.class,
                        DataAccessException.class,
                        TimeoutException.class
                )
                .ignoreExceptions(
                        IllegalArgumentException.class,
                        jakarta.validation.ValidationException.class
                )
                .build();
    }

    @Bean
    public CircuitBreakerRegistry circuitBreakerRegistry(CircuitBreakerConfig circuitBreakerConfig) {
        CircuitBreakerRegistry registry = CircuitBreakerRegistry.of(circuitBreakerConfig);

        CircuitBreaker importacionCB = registry.circuitBreaker("importacionCB", circuitBreakerConfig);

        // Event listeners
        importacionCB.getEventPublisher()
                .onStateTransition(event ->
                        log.warn("Circuit Breaker cambió de estado: {} -> {}",
                                event.getStateTransition().getFromState(),
                                event.getStateTransition().getToState()))
                .onFailureRateExceeded(event ->
                        log.error("Circuit Breaker: Tasa de fallos excedida: {}%",
                                event.getFailureRate()))
                .onCallNotPermitted(event ->
                        log.warn("Circuit Breaker: Llamada no permitida - circuito ABIERTO"));

        log.info("✅ CircuitBreakerRegistry configurado con 'importacionCB'");

        return registry;
    }

    /**
     * Configuración de Bulkhead (límite de concurrencia)
     */
    @Bean
    public BulkheadConfig bulkheadConfig() {
        return BulkheadConfig.custom()
                .maxConcurrentCalls(10)
                .maxWaitDuration(Duration.ofMillis(500))
                .build();
    }

    @Bean
    public BulkheadRegistry bulkheadRegistry(BulkheadConfig bulkheadConfig) {
        BulkheadRegistry registry = BulkheadRegistry.of(bulkheadConfig);

        Bulkhead importacionBulkhead = registry.bulkhead("importacionBulkhead", bulkheadConfig);

        importacionBulkhead.getEventPublisher()
                .onCallPermitted(event ->
                        log.debug("Bulkhead: Llamada permitida"))
                .onCallRejected(event ->
                        log.warn("Bulkhead: Llamada rechazada - límite alcanzado"))
                .onCallFinished(event ->
                        log.debug("Bulkhead: Llamada finalizada"));

        log.info("✅ BulkheadRegistry configurado con 'importacionBulkhead'");

        return registry;
    }
}
