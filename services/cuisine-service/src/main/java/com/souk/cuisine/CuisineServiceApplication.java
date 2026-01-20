package com.souk.cuisine;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = { "com.souk.cuisine", "com.souk.common" })
public class CuisineServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(CuisineServiceApplication.class, args);
    }
}
