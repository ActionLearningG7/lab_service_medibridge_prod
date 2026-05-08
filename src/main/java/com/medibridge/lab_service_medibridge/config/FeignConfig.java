package com.medibridge.lab_service_medibridge.config;

import feign.RequestInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Configuration
public class FeignConfig {

    @Bean
    public RequestInterceptor requestInterceptor() {
        return requestTemplate -> {
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder
                    .getRequestAttributes();

            // 1. Try to get headers from current servlet request (if any)
            if (attributes != null) {
                var request = attributes.getRequest();

                // Propagate Authorization header
                String authHeader = request.getHeader("Authorization");
                if (authHeader != null) {
                    requestTemplate.header("Authorization", authHeader);
                }

                // Propagate Gateway headers for payment-service authentication
                String userId = request.getHeader("X-User-Id");
                if (userId != null) {
                    requestTemplate.header("X-User-Id", userId);
                }

                String userRole = request.getHeader("X-User-Role");
                if (userRole != null) {
                    requestTemplate.header("X-User-Role", userRole);
                }
            }

            // 2. Fallback/Ensure: If headers are still missing, try to fill from
            // SecurityContext
            // This is essential for inter-service calls that might not have come through
            // the gateway
            var authentication = org.springframework.security.core.context.SecurityContextHolder.getContext()
                    .getAuthentication();
            if (authentication != null && authentication.isAuthenticated()) {
                // Set X-User-Id if not already set
                if (requestTemplate.headers().get("X-User-Id") == null) {
                    requestTemplate.header("X-User-Id", authentication.getName());
                }

                // Set X-User-Role if not already set
                if (requestTemplate.headers().get("X-User-Role") == null) {
                    authentication.getAuthorities().stream()
                            .findFirst()
                            .map(ga -> {
                                String role = ga.getAuthority();
                                return role.startsWith("ROLE_") ? role.substring(5) : role;
                            })
                            .ifPresent(role -> requestTemplate.header("X-User-Role", role));
                }
            }
        };
    }
}
