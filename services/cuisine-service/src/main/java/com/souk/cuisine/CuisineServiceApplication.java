package com.souk.cuisine;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication(scanBasePackages = {"com.souk.cuisine", "com.souk.common"})
@EnableJpaRepositories(basePackages = "com.souk.common.adapters.jpa.repository")
@EntityScan(basePackages = "com.souk.common.domain")
public class CuisineServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(CuisineServiceApplication.class, args);
    }
}

