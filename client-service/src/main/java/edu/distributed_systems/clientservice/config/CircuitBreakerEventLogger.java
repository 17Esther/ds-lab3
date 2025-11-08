package edu.distributed_systems.clientservice.config;

import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.circuitbreaker.event.CircuitBreakerOnStateTransitionEvent;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CircuitBreakerEventLogger {

    private static final Logger log = LoggerFactory.getLogger(CircuitBreakerEventLogger.class);
    private final CircuitBreakerRegistry circuitBreakerRegistry;

    public CircuitBreakerEventLogger(CircuitBreakerRegistry circuitBreakerRegistry) {
        this.circuitBreakerRegistry = circuitBreakerRegistry;
    }

    @PostConstruct
    public void registerEventListeners() {
        circuitBreakerRegistry.getAllCircuitBreakers().forEach(cb -> {
            cb.getEventPublisher()
              .onStateTransition(event -> logStateTransition(event, cb.getName()));
        });
    }

    private void logStateTransition(CircuitBreakerOnStateTransitionEvent event, String cbName) {
        log.warn("CircuitBreaker '{}' changed state: {} -> {}",
                cbName,
                event.getStateTransition().getFromState(),
                event.getStateTransition().getToState());
    }
}
