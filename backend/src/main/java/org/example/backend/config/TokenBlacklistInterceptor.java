package org.example.backend.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class TokenBlacklistInterceptor implements HandlerInterceptor {

    private final TokenBlacklistService blacklistService;

    public TokenBlacklistInterceptor(TokenBlacklistService blacklistService) {
        this.blacklistService = blacklistService;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // optional: can go OPTIONS (CORS preflight)
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }

        String authHeader = request.getHeader("Authorization");

        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7).trim();


            if (blacklistService.isBlacklisted(token)) {
                response.setStatus(HttpStatus.UNAUTHORIZED.value());
                response.getWriter().write("Token has been revoked. Please log in again.");
                return false;
            }
        }

        return true;
    }
}