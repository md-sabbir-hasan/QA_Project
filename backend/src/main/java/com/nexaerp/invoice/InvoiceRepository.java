package com.nexaerp.invoice;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface InvoiceRepository extends JpaRepository<Invoice, Long> {
    Optional<Invoice> findTopByOrderByIdDesc();
    List<Invoice> findByPartyId(Long partyId);
    List<Invoice> findByStatus(InvoiceStatus status);
    boolean existsByInvoiceNumber(String invoiceNumber);
    // Used for auto (FIFO) payment allocation
// Returns invoices with remaining due amount, oldest due date first
    List<Invoice> findByPartyIdAndDueAmountGreaterThanAndStatusNotOrderByDueDateAsc(
            Long partyId, BigDecimal dueAmount, InvoiceStatus excludeStatus);
}
