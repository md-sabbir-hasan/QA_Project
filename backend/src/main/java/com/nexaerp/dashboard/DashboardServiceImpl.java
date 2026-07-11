package com.nexaerp.dashboard;

import com.nexaerp.account.AccountRepository;
import com.nexaerp.audit.AuditLog;
import com.nexaerp.audit.AuditLogRepository;
import com.nexaerp.banking.repository.BankAccountRepository;
import com.nexaerp.dashboard.dto.*;
import com.nexaerp.invoice.InvoiceRepository;
import com.nexaerp.journal.JournalEntryRepository;
import com.nexaerp.journal.JournalStatus;
import com.nexaerp.permission.PermissionRepository;
import com.nexaerp.role.RoleRepository;
import com.nexaerp.user.UserRepository;
import com.nexaerp.user.UserStatus;
import com.nexaerp.vendorbill.VendorBillRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import com.nexaerp.dashboard.dto.HealthSummaryDto;
import java.time.ZoneId;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class DashboardServiceImpl implements DashboardService{
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;
    private final AccountRepository accountRepository;
    private final JournalEntryRepository journalEntryRepository;
    private final AuditLogRepository auditLogRepository;
    private final InvoiceRepository invoiceRepository;
    private final VendorBillRepository vendorBillRepository;
    private final BankAccountRepository bankAccountRepository;

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
                .business(buildBusinessSummary())
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

    private BusinessSummaryDto buildBusinessSummary() {
        LocalDate today = LocalDate.now();

        return BusinessSummaryDto.builder()
                .cashPosition(bankAccountRepository.sumActiveBalances())
                .accountsReceivable(invoiceRepository.sumOutstandingReceivable())
                .overdueInvoiceCount(invoiceRepository.countOverdue(today))
                .overdueInvoiceAmount(invoiceRepository.sumOverdueAmount(today))
                .accountsPayable(vendorBillRepository.sumOutstandingPayable())
                .overdueBillCount(vendorBillRepository.countOverdue(today))
                .overdueBillAmount(vendorBillRepository.sumOverdueAmount(today))
                .revenueTrend(buildMonthlyTrend(true))
                .expenseTrend(buildMonthlyTrend(false))
                .build();
    }

    // Builds a 6-month trend (oldest -> newest, including current month)
    private List<MonthlyTrendDto> buildMonthlyTrend(boolean revenue) {
        List<MonthlyTrendDto> trend = new ArrayList<>();
        DateTimeFormatter labelFormat = DateTimeFormatter.ofPattern("MMM yyyy", Locale.ENGLISH);

        YearMonth current = YearMonth.now();

        for (int i = 5; i >= 0; i--) {
            YearMonth month = current.minusMonths(i);
            LocalDate from = month.atDay(1);
            LocalDate to = month.atEndOfMonth();

            BigDecimal amount = revenue
                    ? invoiceRepository.sumGrandTotalBetween(from, to)
                    : vendorBillRepository.sumGrandTotalBetween(from, to);

            trend.add(MonthlyTrendDto.builder()
                    .month(month.format(labelFormat))
                    .amount(amount)
                    .build());
        }

        return trend;
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