package com.souk.payment.gateway.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

/**
 * Configuration for payment gateway beans.
 */
@Configuration
@EnableConfigurationProperties(GatewayProperties.class)
public class GatewayConfig {

    @Bean
    public RestTemplate gatewayRestTemplate() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(10000); // 10 seconds
        factory.setReadTimeout(30000);    // 30 seconds
        return new RestTemplate(factory);
    }

    @Bean
    public ObjectMapper gatewayObjectMapper() {
        return new ObjectMapper();
    }
}
