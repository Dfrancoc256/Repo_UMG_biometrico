package com.umg.biometrico;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class BiometricoApplication {

    public static void main(String[] args) {
        SpringApplication.run(BiometricoApplication.class, args);
    }
}