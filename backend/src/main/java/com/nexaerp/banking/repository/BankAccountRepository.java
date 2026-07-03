package com.nexaerp.banking.repository;


import com.nexaerp.banking.entity.BankAccount;
import com.nexaerp.banking.enums.BankAccountType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BankAccountRepository extends JpaRepository<BankAccount, Long> {
    List<BankAccount> findByIsActive(Boolean isActive);
    List<BankAccount> findByAccountType(BankAccountType accountType);
    Optional<BankAccount> findTopByOrderByIdDesc();
}
