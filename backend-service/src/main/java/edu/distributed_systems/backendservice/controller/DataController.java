package edu.distributed_systems.backendservice.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Random;

@RestController
@RequestMapping("/api")
public class DataController {

    private final Random random = new Random();

    @GetMapping("/data")
    public ResponseEntity<String> getData() throws InterruptedException {
        int chance = random.nextInt(100);

        // 20% chance: HTTP 429 Too Many Requests (transient failure - should retry)
        if (chance < 20) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .body("Rate limit exceeded - transient failure");
        }
        // 15% chance: HTTP 500 Internal Server Error (transient failure - should retry)
        else if (chance < 35) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Simulated transient backend failure");
        }
        // 25% chance: Delay (simulate slow response)
        else if (chance < 60) {
            Thread.sleep(2000); // Simulate delay
        }

        return ResponseEntity.ok("Backend response OK");
    }
}
