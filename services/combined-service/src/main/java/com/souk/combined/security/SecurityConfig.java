package com.souk.combined.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;
    private final CorsProperties corsProperties;

    @org.springframework.beans.factory.annotation.Value("${app.security.enabled:true}")
    private boolean securityEnabled;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter,
            JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint,
            CorsProperties corsProperties) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        this.jwtAuthenticationEntryPoint = jwtAuthenticationEntryPoint;
        this.corsProperties = corsProperties;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(csrf -> csrf.disable());

        // If security is disabled, permit all requests
        if (!securityEnabled) {
            http.authorizeHttpRequests(auth -> auth.anyRequest().permitAll());
            return http.build();
        }

        http
                .exceptionHandling(ex -> ex.authenticationEntryPoint(jwtAuthenticationEntryPoint))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // =====================================================
                        // PUBLIC ENDPOINTS - No authentication required
                        // =====================================================

                        // Allow CORS preflight requests
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()

                        // Public Auth Endpoints
                        .requestMatchers("/v1/auth/**").permitAll()

                        // GET requests for browsing (read-only access)
                        .requestMatchers(HttpMethod.GET,
                                "/vendors/**",
                                "/products/**",
                                "/cuisines/categories", // Explicitly define before wildcard if order matters, though
                                                        // grouped it's fine
                                "/cuisines/**",
                                "/customers/**")
                        .permitAll()

                        // Swagger/OpenAPI documentation
                        .requestMatchers("/swagger-ui/**", "/v3/api-docs/**", "/swagger-ui.html").permitAll()

                        // Health check endpoint
                        .requestMatchers("/actuator/health").permitAll()

                        // Media/upload endpoints (read-only)
                        .requestMatchers(HttpMethod.GET, "/uploads/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/preview/**").permitAll()

                        // =====================================================
                        // PROTECTED ENDPOINTS - Authentication required
                        // =====================================================

                        // All write operations (POST, PUT, DELETE) require authentication
                        .requestMatchers(HttpMethod.POST, "/vendors/**").authenticated()
                        .requestMatchers(HttpMethod.PUT, "/vendors/**").authenticated()
                        .requestMatchers(HttpMethod.PATCH, "/vendors/**").authenticated()
                        .requestMatchers(HttpMethod.DELETE, "/vendors/**").authenticated()

                        .requestMatchers(HttpMethod.POST, "/products/**").authenticated()
                        .requestMatchers(HttpMethod.PUT, "/products/**").authenticated()
                        .requestMatchers(HttpMethod.PATCH, "/products/**").authenticated()
                        .requestMatchers(HttpMethod.DELETE, "/products/**").authenticated()

                        .requestMatchers(HttpMethod.POST, "/cuisines/**").authenticated()
                        .requestMatchers(HttpMethod.PUT, "/cuisines/**").authenticated()
                        .requestMatchers(HttpMethod.PATCH, "/cuisines/**").authenticated()
                        .requestMatchers(HttpMethod.DELETE, "/cuisines/**").authenticated()

                        .requestMatchers(HttpMethod.POST, "/customers/**").authenticated()
                        .requestMatchers(HttpMethod.PUT, "/customers/**").authenticated()
                        .requestMatchers(HttpMethod.PATCH, "/customers/**").authenticated()
                        .requestMatchers(HttpMethod.DELETE, "/customers/**").authenticated()

                        // Upload operations require authentication
                        .requestMatchers(HttpMethod.POST, "/uploads/**").authenticated()
                        .requestMatchers(HttpMethod.POST, "/preview/**").authenticated()

                        // =====================================================
                        // DEFAULT - All other endpoints require authentication
                        // =====================================================
                        .anyRequest().authenticated())
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        java.util.List<String> patterns = corsProperties.getAllowedOriginPatterns();
        boolean hasWildcard = patterns.stream().anyMatch(p -> "*".equals(p));

        if (hasWildcard) {
            // Use setAllowedOrigins with "*" and disable credentials (CORS spec
            // requirement)
            configuration.setAllowedOrigins(java.util.List.of("*"));
            configuration.setAllowCredentials(false);
        } else {
            // Use pattern matching for specific origins
            configuration.setAllowedOriginPatterns(patterns);
            configuration.setAllowCredentials(corsProperties.isAllowCredentials());
        }

        configuration.setAllowedMethods(corsProperties.getAllowedMethods());
        configuration.setAllowedHeaders(corsProperties.getAllowedHeaders());
        configuration.setExposedHeaders(corsProperties.getExposedHeaders());
        configuration.setMaxAge(corsProperties.getMaxAge());

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    @Bean
    public CorsFilter corsFilter() {
        return new CorsFilter(corsConfigurationSource());
    }
}
