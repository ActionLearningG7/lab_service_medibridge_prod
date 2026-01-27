package com.medibridge.lab_service_medibridge.service;

import com.medibridge.lab_service_medibridge.domain.AuditLog;
import com.medibridge.lab_service_medibridge.domain.enums.AuditAction;
import com.medibridge.lab_service_medibridge.repository.AuditLogRepository;
import com.medibridge.lab_service_medibridge.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuditService {

    private final AuditLogRepository auditLogRepository;

    @Async
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logAction(AuditAction action, String targetType, String targetId, String summary) {
        try {
            String userId = "SYSTEM";
            String userRole = "SYSTEM";
            try {
                userId = SecurityUtils.getCurrentUserId();
                // Extract role if possible, or leave as default
            } catch (Exception e) {
                // Ignore
            }

            AuditLog logEntry = AuditLog.builder()
                    .actorId(userId)
                    .actorRole(userRole)
                    .actionType(action)
                    .targetType(targetType)
                    .targetId(targetId)
                    .summary(summary)
                    .build();

            auditLogRepository.save(logEntry);
        } catch (Exception e) {
            log.error("Failed to audit log", e);
        }
    }
}
