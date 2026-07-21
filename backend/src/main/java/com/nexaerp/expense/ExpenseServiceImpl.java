package com.nexaerp.expense;

import com.nexaerp.account.Account;
import com.nexaerp.account.AccountRepository;
import com.nexaerp.account.AccountType;
import com.nexaerp.accountingperiod.AccountingPeriodService;
import com.nexaerp.audit.AuditAction;
import com.nexaerp.audit.AuditLogService;
import com.nexaerp.banking.enums.TransactionSourceType;
import com.nexaerp.banking.enums.TransactionType;
import com.nexaerp.banking.services.BankTransactionService;
import com.nexaerp.budget.BudgetCheckService;
import com.nexaerp.budget.dto.BudgetWarningDto;
import com.nexaerp.common.exception.BusinessRuleException;
import com.nexaerp.common.exception.ResourceNotFoundException;
import com.nexaerp.expense.dto.ExpenseCancelRequestDto;
import com.nexaerp.expense.dto.ExpenseRequestDto;
import com.nexaerp.expense.dto.ExpenseResponseDto;
import com.nexaerp.journal.*;
import com.nexaerp.party.Party;
import com.nexaerp.party.PartyRepository;
import com.nexaerp.payment.PaymentAllocation;
import com.nexaerp.payment.PaymentAllocationRepository;
import com.nexaerp.payment.PaymentReferenceType;
import com.nexaerp.settings.SettingKey;
import com.nexaerp.settings.SystemSettingsService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ExpenseServiceImpl implements ExpenseService {

    private final ExpenseRepository expenseRepository;
    private final AccountRepository accountRepository;
    private final PartyRepository partyRepository;
    private final JournalEntryRepository journalEntryRepository;
    private final JournalLineRepository journalLineRepository;
    private final AccountingPeriodService accountingPeriodService;
    private final SystemSettingsService systemSettingsService;
    private final BankTransactionService bankTransactionService;
    private final PaymentAllocationRepository paymentAllocationRepository;
    private final AuditLogService auditLogService;
    private final BudgetCheckService budgetCheckService;

    @Override
    @Transactional
    public ExpenseResponseDto create(ExpenseRequestDto request) {

        Account expenseAccount = getAccount(request.getExpenseAccountId());
        if (expenseAccount.getType() != AccountType.EXPENSE) {
            throw new BusinessRuleException("Expense category account must be of type EXPENSE");
        }

        boolean paidImmediately = Boolean.TRUE.equals(request.getPaidImmediately());

        Party party = null;
        if (request.getPartyId() != null) {
            party = getParty(request.getPartyId());
        }

        Account paymentAccount = null;
        if (paidImmediately) {
            if (request.getPaymentAccountId() == null) {
                throw new BusinessRuleException("paymentAccountId is required when paidImmediately = true");
            }
            paymentAccount = getAccount(request.getPaymentAccountId());
            if (paymentAccount.getId().equals(expenseAccount.getId())) {
                throw new BusinessRuleException("Payment account cannot be the same as the expense account");
            }
        } else if (party == null) {
            throw new BusinessRuleException("partyId is required when paidImmediately = false (pay later)");
        }

        accountingPeriodService.validatePostingDate(request.getExpenseDate());

        Expense expense = Expense.builder()
                .expenseNumber(generateExpenseNumber())
                .expenseDate(request.getExpenseDate())
                .expenseAccount(expenseAccount)
                .paidImmediately(paidImmediately)
                .paymentAccount(paymentAccount)
                .party(party)
                .amount(request.getAmount())
                .paidAmount(paidImmediately ? request.getAmount() : BigDecimal.ZERO)
                .dueAmount(paidImmediately ? BigDecimal.ZERO : request.getAmount())
                .paymentStatus(paidImmediately ? ExpensePaymentStatus.PAID : ExpensePaymentStatus.UNPAID)
                .referenceNumber(request.getReferenceNumber())
                .attachmentUrl(request.getAttachmentUrl())
                .notes(request.getNotes())
                .status(ExpenseStatus.POSTED)
                .build();

        Expense saved = expenseRepository.save(expense);

        // Dr Expense Account, Cr (Cash/Bank if paid now, else Accounts Payable)
        Account creditAccount = paidImmediately
                ? paymentAccount
                : systemSettingsService.getAccount(SettingKey.DEFAULT_PAYABLE_ACCOUNT);

        JournalEntry entry = new JournalEntry();
        entry.setEntryNumber(generateJournalNumber());
        entry.setDate(request.getExpenseDate());
        entry.setDescription("Expense - " + saved.getExpenseNumber() + " - " + expenseAccount.getName());
        entry.setType(paidImmediately ? JournalEntryType.CASH : JournalEntryType.GENERAL);
        entry.setStatus(JournalStatus.POSTED);
        entry.setSourceType(JournalSourceType.EXPENSE_CLAIM);
        entry.setSourceId(saved.getId());
        entry.setTotalAmount(request.getAmount());
        entry.setReferenceNumber(saved.getExpenseNumber());
        JournalEntry savedEntry = journalEntryRepository.save(entry);

        addLine(savedEntry, expenseAccount, request.getAmount(), BigDecimal.ZERO,
                "Expense - " + saved.getExpenseNumber());
        addLine(savedEntry, creditAccount, BigDecimal.ZERO, request.getAmount(),
                "Expense - " + saved.getExpenseNumber());

        auditLogService.log(
                AuditAction.CREATED,
                "EXPENSE",
                saved.getId(),
                null,
                saved.getExpenseNumber() + " - " + saved.getAmount()
        );

        List<BudgetWarningDto> budgetWarnings = budgetCheckService
                .checkExpenseAccount(expenseAccount, request.getExpenseDate(), request.getAmount())
                .map(List::of)
                .orElse(Collections.emptyList());

        return toResponse(saved, budgetWarnings);
    }

    @Override
    public ExpenseResponseDto getById(Long id) {
        return toResponse(findOrThrow(id));
    }

    @Override
    public List<ExpenseResponseDto> getAll() {
        return expenseRepository.findAll().stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public ExpenseResponseDto cancel(Long id, ExpenseCancelRequestDto request) {
        Expense expense = findOrThrow(id);

        if (expense.getStatus() == ExpenseStatus.CANCELLED) {
            throw new BusinessRuleException("Expense " + expense.getExpenseNumber() + " is already cancelled");
        }

        // If money has already been paid against this expense via the Payment module
        // (the "pay later" case), it must be un-allocated / that Payment cancelled first.
        List<PaymentAllocation> existingAllocations =
                paymentAllocationRepository.findByReferenceTypeAndReferenceId(
                        PaymentReferenceType.EXPENSE, expense.getId());
        if (!existingAllocations.isEmpty()) {
            throw new BusinessRuleException(
                    "Cannot cancel — payment(s) already recorded against this expense. Cancel those payments first.");
        }

        journalEntryRepository.findBySourceTypeAndSourceId(JournalSourceType.EXPENSE_CLAIM, expense.getId())
                .filter(original -> original.getStatus() != JournalStatus.REVERSED)
                .ifPresent(original -> {
                    JournalEntry reversal = new JournalEntry();
                    reversal.setEntryNumber(generateJournalNumber());
                    reversal.setDate(LocalDate.now());
                    reversal.setDescription("Reversal - " + expense.getExpenseNumber());
                    reversal.setType(original.getType());
                    reversal.setStatus(JournalStatus.POSTED);
                    reversal.setSourceType(JournalSourceType.EXPENSE_CLAIM);
                    reversal.setSourceId(expense.getId());
                    reversal.setTotalAmount(original.getTotalAmount());
                    reversal.setReversedFromId(original.getId());
                    reversal.setReferenceNumber("REV-" + original.getEntryNumber());
                    JournalEntry savedReversal = journalEntryRepository.save(reversal);

                    List<JournalLine> originalLines = journalLineRepository.findByJournalEntryId(original.getId());
                    originalLines.forEach(line ->
                            addLine(savedReversal, line.getAccount(), line.getCredit(), line.getDebit(),
                                    "Reversal - " + expense.getExpenseNumber())); // debit/credit swapped

                    original.setStatus(JournalStatus.REVERSED);
                    journalEntryRepository.save(original);
                });

        expense.setStatus(ExpenseStatus.CANCELLED);
        expense.setCancelledAt(LocalDateTime.now());
        expense.setCancelReason(request.getReason());
        expense.setPaidAmount(BigDecimal.ZERO);
        expense.setDueAmount(BigDecimal.ZERO);
        expense.setPaymentStatus(ExpensePaymentStatus.UNPAID);

        auditLogService.log(
                AuditAction.CANCELLED,
                "EXPENSE",
                expense.getId(),
                ExpenseStatus.POSTED.name(),
                ExpenseStatus.CANCELLED.name()
        );

        return toResponse(expenseRepository.save(expense));
    }

    @Override
    @Transactional
    public ExpenseResponseDto attachReceipt(Long id, String attachmentUrl) {
        Expense expense = findOrThrow(id);
        expense.setAttachmentUrl(attachmentUrl);
        return toResponse(expenseRepository.save(expense));
    }

    // _______ Private helpers __________

    private void addLine(JournalEntry entry, Account account, BigDecimal debit, BigDecimal credit, String description) {
        JournalLine line = new JournalLine();
        line.setJournalEntry(entry);
        line.setAccount(account);
        line.setDebit(debit);
        line.setCredit(credit);
        line.setDescription(description);
        journalLineRepository.save(line);
        updateBalance(account, debit, credit);

        // Mirror into Banking module if this COA account is linked to a BankAccount
        if (debit.compareTo(BigDecimal.ZERO) > 0) {
            bankTransactionService.mirrorFromJournal(
                    account.getId(), entry.getDate(), TransactionType.CREDIT, debit,
                    description, entry.getEntryNumber(), null,
                    TransactionSourceType.EXPENSE, entry.getSourceId());
        } else if (credit.compareTo(BigDecimal.ZERO) > 0) {
            bankTransactionService.mirrorFromJournal(
                    account.getId(), entry.getDate(), TransactionType.DEBIT, credit,
                    description, entry.getEntryNumber(), null,
                    TransactionSourceType.EXPENSE, entry.getSourceId());
        }
    }

    private void updateBalance(Account account, BigDecimal debit, BigDecimal credit) {
        switch (account.getType()) {
            case ASSET:
            case EXPENSE:
                account.setCurrentBalance(account.getCurrentBalance().add(debit).subtract(credit));
                break;
            case LIABILITY:
            case EQUITY:
            case REVENUE:
                account.setCurrentBalance(account.getCurrentBalance().add(credit).subtract(debit));
                break;
        }
        accountRepository.save(account);
    }

    private String generateJournalNumber() {
        return journalEntryRepository.findTopByOrderByIdDesc()
                .map(last -> {
                    String lastNumber = last.getEntryNumber().replace("JE-", "");
                    int next = Integer.parseInt(lastNumber) + 1;
                    return String.format("JE-%04d", next);
                })
                .orElse("JE-0001");
    }

    private String generateExpenseNumber() {
        return expenseRepository.findTopByOrderByIdDesc()
                .map(last -> {
                    String lastNumber = last.getExpenseNumber().replace("EXP-", "");
                    int next = Integer.parseInt(lastNumber) + 1;
                    return String.format("EXP-%04d", next);
                })
                .orElse("EXP-0001");
    }

    private Account getAccount(Long id) {
        return accountRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Account not found: " + id));
    }

    private Party getParty(Long id) {
        return partyRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Party not found: " + id));
    }

    private Expense findOrThrow(Long id) {
        return expenseRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Expense not found"));
    }

    private ExpenseResponseDto toResponse(Expense e) {
        return ExpenseResponseDto.builder()
                .id(e.getId())
                .expenseNumber(e.getExpenseNumber())
                .expenseDate(e.getExpenseDate())
                .expenseAccountId(e.getExpenseAccount().getId())
                .expenseAccountName(e.getExpenseAccount().getName())
                .paidImmediately(e.getPaidImmediately())
                .paymentAccountId(e.getPaymentAccount() != null ? e.getPaymentAccount().getId() : null)
                .paymentAccountName(e.getPaymentAccount() != null ? e.getPaymentAccount().getName() : null)
                .partyId(e.getParty() != null ? e.getParty().getId() : null)
                .partyName(e.getParty() != null ? e.getParty().getName() : null)
                .amount(e.getAmount())
                .paidAmount(e.getPaidAmount())
                .dueAmount(e.getDueAmount())
                .paymentStatus(e.getPaymentStatus())
                .referenceNumber(e.getReferenceNumber())
                .attachmentUrl(e.getAttachmentUrl())
                .notes(e.getNotes())
                .status(e.getStatus())
                .cancelledAt(e.getCancelledAt())
                .cancelReason(e.getCancelReason())
                .createdAt(e.getCreatedAt())
                .build();
    }

    // overload
    private ExpenseResponseDto toResponse(Expense e, List<BudgetWarningDto> budgetWarnings) {
        return ExpenseResponseDto.builder()
                .id(e.getId())
                .expenseNumber(e.getExpenseNumber())
                .expenseDate(e.getExpenseDate())
                .expenseAccountId(e.getExpenseAccount().getId())
                .expenseAccountName(e.getExpenseAccount().getName())
                .paidImmediately(e.getPaidImmediately())
                .paymentAccountId(e.getPaymentAccount() != null ? e.getPaymentAccount().getId() : null)
                .paymentAccountName(e.getPaymentAccount() != null ? e.getPaymentAccount().getName() : null)
                .partyId(e.getParty() != null ? e.getParty().getId() : null)
                .partyName(e.getParty() != null ? e.getParty().getName() : null)
                .amount(e.getAmount())
                .paidAmount(e.getPaidAmount())
                .dueAmount(e.getDueAmount())
                .paymentStatus(e.getPaymentStatus())
                .referenceNumber(e.getReferenceNumber())
                .attachmentUrl(e.getAttachmentUrl())
                .notes(e.getNotes())
                .status(e.getStatus())
                .cancelledAt(e.getCancelledAt())
                .cancelReason(e.getCancelReason())
                .createdAt(e.getCreatedAt())
                .budgetWarnings(budgetWarnings)
                .build();
    }
}