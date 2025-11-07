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

        if (chance < 30) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Simulated backend failure");
        } else if (chance < 60) {
            Thread.sleep(2000); // Simulate delay
        }

        return ResponseEntity.ok("Backend response OK");
    }
}
