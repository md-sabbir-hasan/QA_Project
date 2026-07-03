package com.nexaerp.audit;

import com.nexaerp.user.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AuditLogServiceImpl implements AuditLogService{

    private final AuditLogRepository auditLogRepository;
    private final UserRepository userRepository;


    @Override
    public void log(AuditAction action, String entityName, Long entityId, String oldValue, String newValue) {

        // Get current user from SecurityContext
        Long userId = null;
        String userName = "SYSTEM";

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() &&
                !auth.getPrincipal().equals("anonymousUser")) {

            String email = auth.getName();

            var userOpt = userRepository.findByEmail(email);
            if (userOpt.isPresent()) {
                userId = userOpt.get().getId();
                userName = userOpt.get().getName();
            }
        }

        // Get IP address
        String ipAddress = getIpAddress();

        AuditLog log = AuditLog.builder()
                .userId(userId)
                .userName(userName)
                .entityName(entityName)
                .entityId(entityId)
                .action(action)
                .oldValue(oldValue)
                .newValue(newValue)
                .ipAddress(ipAddress)
                .build();

        auditLogRepository.save(log);
    }

    @Override
    public List<AuditLog> getEntityHistory(String entityName, Long entityId) {
        return auditLogRepository
                .findByEntityNameAndEntityIdOrderByCreatedAtDesc(entityName, entityId);
    }

    @Override
    public List<AuditLog> getUserActivity(Long userId) {
        return auditLogRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    @Override
    public List<AuditLog> getEntityLogs(String entityName) {
        return auditLogRepository.findByEntityNameOrderByCreatedAtDesc(entityName);
    }



    // ===================private helper==============


    private String getIpAddress() {
        try {
            ServletRequestAttributes attributes =
                    (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes != null) {
                HttpServletRequest request = attributes.getRequest();
                String ip = request.getHeader("X-Forwarded-For");
                if (ip == null || ip.isEmpty()) {
                    ip = request.getRemoteAddr();
                }
                return ip;
            }
        } catch (Exception e) {
            // ignore
        }
        return "unknown";
    }
}
