package com.souk.auth;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication(scanBasePackages = {
    "com.souk.auth",
    "com.souk.common.adapters"
})
@EntityScan(basePackages = "com.souk.common.domain")
@EnableJpaRepositories(basePackages = "com.souk.common.adapters.jpa.repository")
@EnableConfigurationProperties
@EnableAsync
public class AuthServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(AuthServiceApplication.class, args);
    }
}
