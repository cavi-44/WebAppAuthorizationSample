package org.example.backend.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Value("${security.jwt.secret}")
    private String secretKey;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .cors(Customizer.withDefaults())
                .csrf(csrf -> csrf.disable())
                .headers(headers -> headers.frameOptions(frame -> frame.disable()))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers("/api/auth/**").permitAll()
                        .requestMatchers("/api/teams").permitAll()
                        .requestMatchers("/api/roles").permitAll()
                        .requestMatchers("/h2-console/**").permitAll()
                        .requestMatchers(
                                "/swagger-ui",
                                "/swagger-ui/**",
                                "/v3/api-docs",
                                "/v3/api-docs/**",
                                "/swagger-ui.html",
                                "/swagger-resources",
                                "/swagger-resources/**",
                                "/webjars/**"
                        ).permitAll()
                        .anyRequest().authenticated()
                )
                .addFilterBefore(new JwtAuthenticationFilter(secretKey), UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        //TODO config - swoje ip wpisac na liste
        configuration.setAllowedOrigins(List.of("http://localhost:5173", "http://192.168.1.64:5173", "http://192.168.1.21:5173"));
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
