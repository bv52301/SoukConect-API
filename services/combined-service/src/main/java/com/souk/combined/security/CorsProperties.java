package com.souk.combined.security;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@ConfigurationProperties(prefix = "app.cors")
public class CorsProperties {

    private List<String> allowedOriginPatterns = List.of("*");
    private List<String> allowedMethods = List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS");
    private List<String> allowedHeaders = List.of("*");
    private List<String> exposedHeaders = List.of("Authorization");
    private boolean allowCredentials = true;
    private long maxAge = 3600L;

    public List<String> getAllowedOriginPatterns() { return allowedOriginPatterns; }
    public void setAllowedOriginPatterns(List<String> allowedOriginPatterns) { this.allowedOriginPatterns = allowedOriginPatterns; }

    public List<String> getAllowedMethods() { return allowedMethods; }
    public void setAllowedMethods(List<String> allowedMethods) { this.allowedMethods = allowedMethods; }

    public List<String> getAllowedHeaders() { return allowedHeaders; }
    public void setAllowedHeaders(List<String> allowedHeaders) { this.allowedHeaders = allowedHeaders; }

    public List<String> getExposedHeaders() { return exposedHeaders; }
    public void setExposedHeaders(List<String> exposedHeaders) { this.exposedHeaders = exposedHeaders; }

    public boolean isAllowCredentials() { return allowCredentials; }
    public void setAllowCredentials(boolean allowCredentials) { this.allowCredentials = allowCredentials; }

    public long getMaxAge() { return maxAge; }
    public void setMaxAge(long maxAge) { this.maxAge = maxAge; }
}
