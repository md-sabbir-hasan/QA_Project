package com.nexaerp.banking.serviceimpl;


import com.nexaerp.account.Account;
import com.nexaerp.account.AccountRepository;
import com.nexaerp.banking.dto.BankTransactionRequestDto;
import com.nexaerp.banking.dto.BankTransactionResponseDto;
import com.nexaerp.banking.entity.BankAccount;
import com.nexaerp.banking.entity.BankTransaction;
import com.nexaerp.banking.enums.TransactionSourceType;
import com.nexaerp.banking.enums.TransactionType;
import com.nexaerp.banking.repository.BankAccountRepository;
import com.nexaerp.banking.repository.BankTransactionRepository;
import com.nexaerp.banking.services.BankTransactionService;
import com.nexaerp.common.exception.BusinessRuleException;
import com.nexaerp.common.exception.ResourceNotFoundException;
import com.nexaerp.journal.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Year;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BankTransactionServiceImpl implements BankTransactionService {

    private final BankTransactionRepository bankTransactionRepository;
    private final BankAccountRepository bankAccountRepository;
    private final AccountRepository accountRepository;
    private final JournalEntryRepository journalEntryRepository;
    private final JournalLineRepository journalLineRepository;



    @Override
    public BankTransactionResponseDto create(BankTransactionRequestDto request) {
        BankAccount bankAccount = bankAccountRepository.findById(request.getBankAccountId())
                .orElseThrow(() -> new ResourceNotFoundException("Bank account not found"));

        Account contraAccount = accountRepository.findById(request.getContraAccountId())
                .orElseThrow(() -> new ResourceNotFoundException("Contra account not found"));

        BankTransaction transaction = BankTransaction.builder()
                .transactionNumber(generateTransactionNumber())
                .bankAccount(bankAccount)
                .transactionDate(request.getTransactionDate())
                .transactionType(request.getTransactionType())
                .amount(request.getAmount())
                .description(request.getDescription())
                .referenceNumber(request.getReferenceNumber())
                .contraAccountId(request.getContraAccountId())
                .reconciled(false)
                .sourceType(TransactionSourceType.MANUAL)
                .build();

        BankTransaction saved = bankTransactionRepository.save(transaction);

        // Update bank account balance
        if (request.getTransactionType() == TransactionType.CREDIT) {
            bankAccount.setCurrentBalance(
                    bankAccount.getCurrentBalance().add(request.getAmount()));
        } else {
            bankAccount.setCurrentBalance(
                    bankAccount.getCurrentBalance().subtract(request.getAmount()));
        }
        bankAccountRepository.save(bankAccount);

        // Create Journal Entry
        createJournalEntry(saved, bankAccount, contraAccount);

        return toResponse(saved);
    }

    @Override
    public BankTransactionResponseDto getById(Long id) {
        BankTransaction transaction = bankTransactionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction not found"));
        return toResponse(transaction);
    }

    @Override
    public List<BankTransactionResponseDto> getAll() {
        return bankTransactionRepository.findAll()
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<BankTransactionResponseDto> getByAccount(Long bankAccountId) {
        return bankTransactionRepository.findByBankAccountId(bankAccountId)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<BankTransactionResponseDto> getByAccountAndDateRange(Long bankAccountId, LocalDate from, LocalDate to) {
        return bankTransactionRepository
                .findByBankAccountIdAndTransactionDateBetween(bankAccountId, from, to)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    public BankTransactionResponseDto reconcile(Long id) {
        BankTransaction transaction = bankTransactionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction not found"));

        if (transaction.getReconciled()) {
            throw new BusinessRuleException("Transaction already reconciled");
        }

        transaction.setReconciled(true);
        transaction.setReconciledAt(LocalDateTime.now());

        return toResponse(bankTransactionRepository.save(transaction));
    }


    // _______Private helper__________


    private void createJournalEntry(BankTransaction transaction,
                                    BankAccount bankAccount,
                                    Account contraAccount) {

        // Get COA account linked to this bank account
        Account bankCoaAccount = accountRepository.findById(bankAccount.getCoaAccountId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "COA account not found for bank account"));

        JournalEntry entry = new JournalEntry();
        entry.setEntryNumber(generateJournalNumber());
        entry.setDate(transaction.getTransactionDate());
        entry.setDescription(transaction.getDescription());
        entry.setType(JournalEntryType.BANK);
        entry.setStatus(JournalStatus.POSTED);
        entry.setSourceType(JournalSourceType.BANK_TRANSACTION);
        entry.setSourceId(transaction.getId());
        entry.setReferenceNumber(transaction.getTransactionNumber());
        entry.setTotalAmount(transaction.getAmount());

        JournalEntry saved = journalEntryRepository.save(entry);

        JournalLine line1 = new JournalLine();
        JournalLine line2 = new JournalLine();

        if (transaction.getTransactionType() == TransactionType.CREDIT) {
            // Money coming in -> Dr Bank COA, Cr Contra Account
            line1.setJournalEntry(saved);
            line1.setAccount(bankCoaAccount);
            line1.setDebit(transaction.getAmount());
            line1.setCredit(BigDecimal.ZERO);
            line1.setDescription(transaction.getDescription());

            line2.setJournalEntry(saved);
            line2.setAccount(contraAccount);
            line2.setDebit(BigDecimal.ZERO);
            line2.setCredit(transaction.getAmount());
            line2.setDescription(transaction.getDescription());

            // Update balances
            updateBalance(bankCoaAccount, transaction.getAmount(), BigDecimal.ZERO);
            updateBalance(contraAccount, BigDecimal.ZERO, transaction.getAmount());

        } else {
            // Money going out > Dr Contra Account, Cr Bank COA
            line1.setJournalEntry(saved);
            line1.setAccount(contraAccount);
            line1.setDebit(transaction.getAmount());
            line1.setCredit(BigDecimal.ZERO);
            line1.setDescription(transaction.getDescription());

            line2.setJournalEntry(saved);
            line2.setAccount(bankCoaAccount);
            line2.setDebit(BigDecimal.ZERO);
            line2.setCredit(transaction.getAmount());
            line2.setDescription(transaction.getDescription());

            // Update balances
            updateBalance(contraAccount, transaction.getAmount(), BigDecimal.ZERO);
            updateBalance(bankCoaAccount, BigDecimal.ZERO, transaction.getAmount());
        }

        journalLineRepository.save(line1);
        journalLineRepository.save(line2);
    }

    private void updateBalance(Account account, BigDecimal debit, BigDecimal credit) {
        switch (account.getType()) {
            case ASSET:
            case EXPENSE:
                account.setCurrentBalance(
                        account.getCurrentBalance().add(debit).subtract(credit));
                break;
            case LIABILITY:
            case EQUITY:
            case REVENUE:
                account.setCurrentBalance(
                        account.getCurrentBalance().add(credit).subtract(debit));
                break;
        }
        accountRepository.save(account);
    }

    private String generateTransactionNumber() {
        int year = Year.now().getValue();
        return bankTransactionRepository.findTopByOrderByIdDesc()
                .map(last -> {
                    String[] parts = last.getTransactionNumber().split("-");
                    int next = Integer.parseInt(parts[2]) + 1;
                    return String.format("TXN-%d-%06d", year, next);
                })
                .orElse(String.format("TXN-%d-%06d", year, 1));
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

    private BankTransactionResponseDto toResponse(BankTransaction transaction) {

        String contraAccountName = null;
        if (transaction.getContraAccountId() != null) {
            contraAccountName = accountRepository.findById(transaction.getContraAccountId())
                    .map(a -> a.getName())
                    .orElse(null);
        }

        return BankTransactionResponseDto.builder()
                .id(transaction.getId())
                .transactionNumber(transaction.getTransactionNumber())
                .bankAccountId(transaction.getBankAccount().getId())
                .bankAccountName(transaction.getBankAccount().getAccountName())
                .transactionDate(transaction.getTransactionDate())
                .transactionType(transaction.getTransactionType())
                .amount(transaction.getAmount())
                .description(transaction.getDescription())
                .referenceNumber(transaction.getReferenceNumber())
                .contraAccountId(transaction.getContraAccountId())
                .contraAccountName(contraAccountName)
                .reconciled(transaction.getReconciled())
                .reconciledAt(transaction.getReconciledAt())
                .sourceType(transaction.getSourceType())
                .createdAt(transaction.getCreatedAt())
                .build();
    }
}
