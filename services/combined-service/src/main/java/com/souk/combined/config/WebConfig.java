package com.souk.combined.config;

import com.souk.combined.security.CorsProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final CorsProperties corsProperties;

    public WebConfig(CorsProperties corsProperties) {
        this.corsProperties = corsProperties;
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        var patterns = corsProperties.getAllowedOriginPatterns();
        boolean hasWildcard = patterns.stream().anyMatch(p -> "*".equals(p));

        var mapping = registry.addMapping("/**");

        if (hasWildcard) {
            mapping.allowedOrigins("*");
            mapping.allowCredentials(false);
        } else {
            mapping.allowedOriginPatterns(patterns.toArray(new String[0]));
            mapping.allowCredentials(corsProperties.isAllowCredentials());
        }

        mapping.allowedMethods(corsProperties.getAllowedMethods().toArray(new String[0]));
        mapping.allowedHeaders(corsProperties.getAllowedHeaders().toArray(new String[0]));
        mapping.exposedHeaders(corsProperties.getExposedHeaders().toArray(new String[0]));
        mapping.maxAge(corsProperties.getMaxAge());
    }
}
