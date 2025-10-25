package com.souk.combined;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;



@SpringBootApplication(scanBasePackages = {
        "com.souk.product",
        "com.souk.vendor",
        "com.souk.common"
})
@EnableJpaRepositories(basePackages = "com.souk.common.adapters.jpa.repository")
@EntityScan(basePackages = "com.souk.common.domain")
public class CombinedServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(CombinedServiceApplication.class, args);
    }
}