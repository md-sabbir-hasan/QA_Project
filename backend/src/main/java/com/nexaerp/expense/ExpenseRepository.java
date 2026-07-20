package com.nexaerp.expense;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ExpenseRepository extends JpaRepository<Expense, Long> {
    Optional<Expense> findTopByOrderByIdDesc(); // for EXP-0001 number generation
}