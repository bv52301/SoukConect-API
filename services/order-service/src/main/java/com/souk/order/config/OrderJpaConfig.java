package com.souk.order.config;

import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@Configuration
@Profile("!combined")
@EnableJpaRepositories(basePackages = "com.souk.common.adapters.jpa.repository")
@EntityScan(basePackages = "com.souk.common.domain")
public class OrderJpaConfig {
}
