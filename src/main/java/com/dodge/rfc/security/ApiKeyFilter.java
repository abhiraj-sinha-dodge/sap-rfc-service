package com.dodge.rfc.security;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@Order(1)
public class ApiKeyFilter implements Filter {

    @Value("${security.api-key:}")
    private String apiKey;

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        String path = httpRequest.getRequestURI();

        // Skip auth for health and actuator endpoints
        if (path.equals("/rfc/health") ||
            path.startsWith("/actuator")) {
            chain.doFilter(request, response);
            return;
        }

        // Skip auth if no API key is configured
        if (apiKey == null || apiKey.isEmpty()) {
            chain.doFilter(request, response);
            return;
        }

        // Check API key
        String providedKey = httpRequest.getHeader("x-api-key");
        if (providedKey == null) {
            providedKey = httpRequest.getHeader("X-API-Key");
        }

        if (!apiKey.equals(providedKey)) {
            httpResponse.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            httpResponse.setContentType("application/json");
            httpResponse.getWriter().write("{\"error\":\"Unauthorized\",\"message\":\"Invalid or missing API key\"}");
            return;
        }

        chain.doFilter(request, response);
    }
}
