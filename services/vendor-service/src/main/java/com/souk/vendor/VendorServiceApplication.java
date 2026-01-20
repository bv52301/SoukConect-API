package com.souk.vendor;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication(scanBasePackages = "com.souk")
public class VendorServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(VendorServiceApplication.class, args);
    }
}