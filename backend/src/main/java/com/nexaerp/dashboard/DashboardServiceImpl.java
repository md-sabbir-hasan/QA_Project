package com.nexaerp.dashboard;

import com.nexaerp.account.AccountRepository;
import com.nexaerp.audit.AuditLog;
import com.nexaerp.audit.AuditLogRepository;
import com.nexaerp.dashboard.dto.*;
import com.nexaerp.journal.JournalEntryRepository;
import com.nexaerp.journal.JournalStatus;
import com.nexaerp.permission.PermissionRepository;
import com.nexaerp.role.RoleRepository;
import com.nexaerp.user.UserRepository;
import com.nexaerp.user.UserStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import com.nexaerp.dashboard.dto.HealthSummaryDto;
import java.time.ZoneId;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DashboardServiceImpl implements DashboardService{
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;
    private final AccountRepository accountRepository;
    private final JournalEntryRepository journalEntryRepository;
    private final AuditLogRepository auditLogRepository;

    @Value("${app.version:1.0.0}")
    private String applicationVersion;

    @Value("${spring.profiles.active:default}")
    private String environment;

    @Override
    public DashboardSummaryDto getSummary() {
        List<RecentActivityDto> recentActivities = auditLogRepository
                .findTop10ByOrderByCreatedAtDesc()
                .stream()
                .map(this::toActivity)
                .toList();


        return DashboardSummaryDto.builder()
                .users(buildUserSummary())
                .security(buildSecuritySummary())
                .finance(buildFinanceSummary())
                .system(buildSystemSummary())
                .recentActivities(recentActivities)
                .health(buildHealthSummary())
                .build();
    }

    private UserSummaryDto buildUserSummary() {
        return UserSummaryDto.builder()
                .total(userRepository.count())
                .active(userRepository.countByStatus(UserStatus.ACTIVE))
                .pending(userRepository.countByStatus(UserStatus.PENDING))
                .inactive(userRepository.countByStatus(UserStatus.INACTIVE))
                .locked(userRepository.countByStatus(UserStatus.LOCKED))
                .build();
    }

    private SecuritySummaryDto buildSecuritySummary() {
        return SecuritySummaryDto.builder()
                .totalRoles(roleRepository.count())
                .totalPermissions(permissionRepository.count())
                .build();
    }

    private FinanceSummaryDto buildFinanceSummary() {
        return FinanceSummaryDto.builder()
                .totalAccounts(accountRepository.count())
                .totalJournalEntries(journalEntryRepository.count())
                .postedJournalEntries(journalEntryRepository.countByStatus(JournalStatus.POSTED))
                .draftJournalEntries(journalEntryRepository.countByStatus(JournalStatus.DRAFT))
                .reversedJournalEntries(journalEntryRepository.countByStatus(JournalStatus.REVERSED))
                .build();
    }

    private SystemSummaryDto buildSystemSummary() {
        return SystemSummaryDto.builder()
                .applicationVersion(applicationVersion)
                .serverTime(LocalDateTime.now())
                .serverTimezone(ZoneId.systemDefault().toString())
                .environment(environment)
                .javaVersion(System.getProperty("java.version"))
                .build();
    }

    private RecentActivityDto toActivity(AuditLog audit) {
        return RecentActivityDto.builder()
                .action(audit.getAction().name())
                .entityName(audit.getEntityName())
                .entityId(audit.getEntityId())
                .userName(audit.getUserName())
                .createdAt(audit.getCreatedAt())
                .description(
                        audit.getAction() + " " + audit.getEntityName()
                )
                .build();
    }

    private HealthSummaryDto buildHealthSummary() {
        return HealthSummaryDto.builder()
                .application("UP")
                .database("UP")
                .mail("UP")
                .build();
    }
}
