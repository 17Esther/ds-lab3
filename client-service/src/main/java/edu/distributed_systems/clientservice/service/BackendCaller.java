package edu.distributed_systems.clientservice.service;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.ObjectFactory;

import java.util.Timer;
import java.util.TimerTask;

@Service
public class BackendCaller {
    private static final Logger logger = LoggerFactory.getLogger(BackendCaller.class);

    private final RestTemplate restTemplate;
    private final String backendUrl = "http://backend-service:8081/api/data";

    private ObjectFactory<BackendCaller> selfFactory;

    @Autowired
    public BackendCaller(RestTemplate restTemplate, ObjectFactory<BackendCaller> selfFactory) {
        this.restTemplate = restTemplate;
        this.selfFactory = selfFactory;
    }

    @PostConstruct
    public void startCallingBackend() {
        Timer timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                try {
                    String response = selfFactory.getObject().callBackend(); // Call through the proxied instance
                    logger.info("Response: {}", response);
                } catch (Exception e) {
                    logger.error("Request failed: {}", e.getMessage());
                }
            }
        }, 0, 3000); // call every 3 seconds
    }

    @Retry(name = "backendRetry")
    @CircuitBreaker(name = "backendCB", fallbackMethod = "fallback")
    public String callBackend() {
        logger.info("Attempting to call backend service...");
        // RestTemplate will throw HttpServerErrorException for 5xx and HttpClientErrorException for 4xx
        // These exceptions will be caught by the retry mechanism
        ResponseEntity<String> response = restTemplate.getForEntity(backendUrl, String.class);
        logger.info("Backend call successful");
        return response.getBody();
    }

    public String fallback(Throwable t) {
        return "Fallback response due to error: " + t.getMessage();
    }
}
