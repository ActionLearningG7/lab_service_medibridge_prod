package com.medibridge.lab_service_medibridge.config.security;

import com.medibridge.lab_service_medibridge.domain.LabOrder;
import com.medibridge.lab_service_medibridge.repository.LabOrderRepository;
import com.medibridge.lab_service_medibridge.util.JwtService;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;

import java.security.Principal;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
@RequiredArgsConstructor
@Slf4j
public class WebSocketAuthInterceptor implements ChannelInterceptor {

    private final JwtService jwtService;
    // resolving circular dependency if any by Lazy, though unlikely here
    private final LabOrderRepository labOrderRepository;

    private static final Pattern TRACKING_TOPIC_PATTERN = Pattern.compile(".*/topic/lab/orders/([^/]+)/tracking.*");

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

        if (accessor != null && StompCommand.CONNECT.equals(accessor.getCommand())) {
            String authToken = accessor.getFirstNativeHeader("Authorization");
            if (authToken == null) {
                // Fallback to X-Authorization if standard header missing/filtered
                authToken = accessor.getFirstNativeHeader("X-Authorization");
            }

            if (authToken != null && authToken.startsWith("Bearer ")) {
                try {
                    Claims claims = jwtService.validateToken(authToken);
                    String userId = claims.getSubject();
                    String role = claims.get("role", String.class);

                    if (role != null && !role.startsWith("ROLE_")) {
                        role = "ROLE_" + role;
                    }

                    List<SimpleGrantedAuthority> authorities = role != null
                            ? Collections.singletonList(new SimpleGrantedAuthority(role))
                            : Collections.emptyList();

                    UsernamePasswordAuthenticationToken user = new UsernamePasswordAuthenticationToken(userId, null,
                            authorities);

                    accessor.setUser(user);
                    log.info("WS CONNECT: Authenticated user {}", userId);

                } catch (Exception e) {
                    log.error("WS CONNECT: Authentication failed: {}", e.getMessage());
                    // We don't necessarily throw here, STOMP will fail if user is null later?
                    // Or we throw to reject connection immediately.
                    throw new IllegalArgumentException("Invalid Auth Token");
                }
            }
        } else if (accessor != null && StompCommand.SUBSCRIBE.equals(accessor.getCommand())) {
            Principal user = accessor.getUser();
            if (user == null) {
                log.error("WS SUBSCRIBE: Unauthenticated subscription attempt");
                throw new IllegalArgumentException("User not authenticated");
            }

            String destination = accessor.getDestination();
            if (destination != null) {
                Matcher matcher = TRACKING_TOPIC_PATTERN.matcher(destination);
                if (matcher.matches()) {
                    String orderIdStr = matcher.group(1);
                    validateSubscriptionAccess(user, orderIdStr);
                    log.info("WS SUBSCRIBE: Authorized access to {} for user {}", destination, user.getName());
                }
            }
        }

        return message;
    }

    private void validateSubscriptionAccess(Principal user, String orderIdStr) {
        if (user instanceof UsernamePasswordAuthenticationToken auth) {
            boolean isAdmin = auth.getAuthorities().stream()
                    .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
            // Depending on architecture, PHLEBOTOMIST might need access to specific orders
            // or all
            boolean isPhleb = auth.getAuthorities().stream()
                    .anyMatch(a -> a.getAuthority().equals("ROLE_PHLEBOTOMIST"));

            if (isAdmin || isPhleb)
                return; // Allow admin/phleb to subscribe

            try {
                UUID orderId = UUID.fromString(orderIdStr);
                LabOrder order = labOrderRepository.findById(orderId).orElse(null);

                if (order == null) {
                    // Avoid leaking existence? Or just deny.
                    throw new IllegalArgumentException("Resource not found");
                }

                String userId = user.getName();
                boolean isPatient = userId.equals(order.getPatientId());
                boolean isDoctor = userId.equals(order.getDoctorId());

                if (!isPatient && !isDoctor) {
                    throw new SecurityException("Subscription denied: Not your order");
                }

            } catch (IllegalArgumentException e) {
                // Invalid UUID
                throw new IllegalArgumentException("Invalid Order ID");
            }
        }
    }
}
