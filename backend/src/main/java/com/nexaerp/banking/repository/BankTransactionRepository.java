package com.nexaerp.banking.repository;


import com.nexaerp.banking.entity.BankTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface BankTransactionRepository extends JpaRepository<BankTransaction, Long> {

    List<BankTransaction> findByBankAccountId(Long bankAccountId);
    List<BankTransaction> findByBankAccountIdAndTransactionDateBetween(
            Long bankAccountId, LocalDate from, LocalDate to);
    List<BankTransaction> findByReconciled(Boolean reconciled);
    Optional<BankTransaction> findTopByOrderByIdDesc();
}
