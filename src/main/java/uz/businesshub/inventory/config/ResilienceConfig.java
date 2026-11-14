package uz.businesshub.inventory.config;

import feign.FeignException;
import feign.RetryableException;
import io.github.resilience4j.bulkhead.Bulkhead;
import io.github.resilience4j.bulkhead.BulkheadConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.core.IntervalFunction;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Configuration
public class ResilienceConfig {

    private static final Logger log = LoggerFactory.getLogger(ResilienceConfig.class);

    /** Faqat infratuzilma xatolari: ulanish yoki timeout xatosi, 5xx. 404 va 400 bunga KIRMAYDI. */
    public static boolean isInfraFailure(Throwable e) {
        return e instanceof RetryableException
                || (e instanceof FeignException fe && fe.status() >= 500);
    }

    @Bean
    public CircuitBreaker productServiceCircuitBreaker() {
        CircuitBreakerConfig config = CircuitBreakerConfig.custom()
                .slidingWindowType(CircuitBreakerConfig.SlidingWindowType.COUNT_BASED)
                .slidingWindowSize(10)
                .minimumNumberOfCalls(5)
                .failureRateThreshold(50)
                .waitDurationInOpenState(Duration.ofSeconds(10))
                .permittedNumberOfCallsInHalfOpenState(2)
                .recordException(ResilienceConfig::isInfraFailure)
                .build();

        CircuitBreaker cb = CircuitBreaker.of("product-service", config);
        cb.getEventPublisher().onStateTransition(e ->
                log.warn("CircuitBreaker '{}': {}", e.getCircuitBreakerName(), e.getStateTransition()));
        return cb;
    }

    @Bean
    public Retry productServiceRetry() {
        RetryConfig config = RetryConfig.custom()
                .maxAttempts(3)
                .intervalFunction(IntervalFunction.ofExponentialRandomBackoff(200, 2.0))
                .retryOnException(ResilienceConfig::isInfraFailure)
                .build();

        Retry retry = Retry.of("product-service", config);
        retry.getEventPublisher().onRetry(e ->
                log.info("Retry '{}' attempt #{}: {}", e.getName(), e.getNumberOfRetryAttempts(),
                        e.getLastThrowable().toString()));
        return retry;
    }

    @Bean
    public Bulkhead productServiceBulkhead() {
        BulkheadConfig config = BulkheadConfig.custom()
                .maxConcurrentCalls(20)
                .maxWaitDuration(Duration.ZERO)
                .build();
        return Bulkhead.of("product-service", config);
    }
}