package com.nexaerp.account;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AccountRepository extends JpaRepository<Account, Long> {
    boolean existsByCode(String code);

    Optional<Account> findByCode(String code);

    List<Account> findByType(AccountType type);

    List<Account> findByParentIsNull(); // Root accounts

    List<Account> findByIsActive(Boolean isActive);
}
