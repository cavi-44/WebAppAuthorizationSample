package org.example.backend.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final TokenBlacklistInterceptor blacklistInterceptor;

    public WebConfig(TokenBlacklistInterceptor blacklistInterceptor) {
        this.blacklistInterceptor = blacklistInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(blacklistInterceptor)
                .addPathPatterns("/api/**")
                .excludePathPatterns("/api/auth/login")  //exclusions
                .excludePathPatterns("/api/auth/register");
    }
}