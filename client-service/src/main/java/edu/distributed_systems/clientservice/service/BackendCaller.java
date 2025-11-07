package edu.distributed_systems.clientservice.service;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.ResponseEntity;

import java.util.Timer;
import java.util.TimerTask;

@Service
public class BackendCaller {
    private static final Logger logger = LoggerFactory.getLogger(BackendCaller.class);

    private final RestTemplate restTemplate = new RestTemplate();
    private final String backendUrl = "http://backend-service:8081/api/data";

    @PostConstruct
    public void startCallingBackend() {
        Timer timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                try {
                    ResponseEntity<String> response = restTemplate.getForEntity(backendUrl, String.class);
                    logger.info("Response: {}", response.getBody());
                } catch (Exception e) {
                    logger.error("Request failed: {}", e.getMessage());
                }
            }
        }, 0, 3000); // call every 3 seconds
    }

    @CircuitBreaker(name = "backendCB", fallbackMethod = "fallback")
    public String callBackend() {
        return restTemplate.getForObject("http://backend-service:8081/api/data", String.class);
    }

    public String fallback(Throwable t) {
        return "Fallback response due to error: " + t.getMessage();
    }
}
