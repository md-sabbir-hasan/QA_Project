package com.nexaerp.banking.services;

import com.nexaerp.banking.dto.BankTransactionRequestDto;
import com.nexaerp.banking.dto.BankTransactionResponseDto;

import java.time.LocalDate;
import java.util.List;

public interface BankTransactionService {
    BankTransactionResponseDto create(BankTransactionRequestDto request);
    BankTransactionResponseDto getById(Long id);
    List<BankTransactionResponseDto> getAll();
    List<BankTransactionResponseDto> getByAccount(Long bankAccountId);
    List<BankTransactionResponseDto> getByAccountAndDateRange(
            Long bankAccountId, LocalDate from, LocalDate to);
    BankTransactionResponseDto reconcile(Long id);
}
