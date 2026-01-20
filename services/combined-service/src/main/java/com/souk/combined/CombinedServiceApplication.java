package com.souk.combined;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = {
        "com.souk.combined",
        "com.souk.product",
        "com.souk.vendor",
        "com.souk.cuisine",
        "com.souk.common"
})
public class CombinedServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(CombinedServiceApplication.class, args);
    }
}
