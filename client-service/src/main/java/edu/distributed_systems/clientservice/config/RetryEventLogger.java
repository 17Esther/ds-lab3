package edu.distributed_systems.clientservice.config;

import io.github.resilience4j.retry.RetryRegistry;
import io.github.resilience4j.retry.event.RetryEvent;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RetryEventLogger {

    private static final Logger log = LoggerFactory.getLogger(RetryEventLogger.class);
    private final RetryRegistry retryRegistry;

    public RetryEventLogger(RetryRegistry retryRegistry) {
        this.retryRegistry = retryRegistry;
    }

    @PostConstruct
    public void registerEventListeners() {
        retryRegistry.getAllRetries().forEach(retry -> {
            retry.getEventPublisher()
                .onRetry(event -> logRetryEvent(event, retry.getName()))
                .onSuccess(event -> logRetrySuccess(event, retry.getName()))
                .onError(event -> logRetryError(event, retry.getName()));
        });
    }

    private void logRetryEvent(RetryEvent event, String retryName) {
        log.warn("Retry '{}' - Attempt #{} (with exponential backoff + jitter). Error: {}",
                retryName,
                event.getNumberOfRetryAttempts(),
                event.getLastThrowable() != null ? event.getLastThrowable().getMessage() : "N/A");
    }

    private void logRetrySuccess(RetryEvent event, String retryName) {
        log.info("Retry '{}' - Success after {} attempts",
                retryName,
                event.getNumberOfRetryAttempts());
    }

    private void logRetryError(RetryEvent event, String retryName) {
        log.error("Retry '{}' - Failed after {} attempts. Final error: {}",
                retryName,
                event.getNumberOfRetryAttempts(),
                event.getLastThrowable() != null ? event.getLastThrowable().getMessage() : "N/A");
    }
}

