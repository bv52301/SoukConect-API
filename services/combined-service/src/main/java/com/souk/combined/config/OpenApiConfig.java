package com.souk.combined.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.servers.Server;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI soukConnectOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("SoukConnect API")
                        .description("Unified API documentation for SoukConnect marketplace platform")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("SoukConnect Team")
                                .email("support@soukconnect.com")))
                .servers(List.of(
                        new Server().url("/api").description("API Server")
                ));
    }

    // Bundled services: product, vendor, cuisine, cart, customer, order

    @Bean
    public GroupedOpenApi allApis() {
        return GroupedOpenApi.builder()
                .group("1-all")
                .displayName("All APIs (Combined)")
                .pathsToMatch("/**")
                .pathsToExclude("/error")
                .build();
    }

    @Bean
    public GroupedOpenApi vendorApis() {
        return GroupedOpenApi.builder()
                .group("2-vendor")
                .displayName("Vendors")
                .pathsToMatch("/vendors/**")
                .build();
    }

    @Bean
    public GroupedOpenApi productApis() {
        return GroupedOpenApi.builder()
                .group("3-product")
                .displayName("Products")
                .pathsToMatch("/products/**", "/preview/**")
                .build();
    }

    @Bean
    public GroupedOpenApi cuisineApis() {
        return GroupedOpenApi.builder()
                .group("4-cuisine")
                .displayName("Cuisines")
                .pathsToMatch("/cuisines/**")
                .build();
    }

    @Bean
    public GroupedOpenApi customerApis() {
        return GroupedOpenApi.builder()
                .group("5-customer")
                .displayName("Customers")
                .pathsToMatch("/customers/**")
                .build();
    }

    @Bean
    public GroupedOpenApi cartApis() {
        return GroupedOpenApi.builder()
                .group("6-cart")
                .displayName("Cart")
                .pathsToMatch("/cart/**")
                .build();
    }

    @Bean
    public GroupedOpenApi orderApis() {
        return GroupedOpenApi.builder()
                .group("7-order")
                .displayName("Orders")
                .pathsToMatch("/orders/**")
                .build();
    }
}
