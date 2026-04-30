package com.finbrain;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class FinBrainApplication {
    public static void main(String[] args) {
        SpringApplication.run(FinBrainApplication.class, args);
    }
}
