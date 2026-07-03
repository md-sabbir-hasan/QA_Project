package com.nexaerp.vendorbill;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface VendorBillRepository extends JpaRepository<VendorBill, Long> {
    Optional<VendorBill> findTopByOrderByIdDesc();
    List<VendorBill> findByPartyId(Long partyId);
    List<VendorBill> findByStatus(VendorBillStatus status);
    List<VendorBill> findByBillType(VendorBillType billType);
    // Used for auto (FIFO) payment allocation
// Returns bills with remaining due amount, oldest due date first
    List<VendorBill> findByPartyIdAndDueAmountGreaterThanAndStatusNotOrderByDueDateAsc(
            Long partyId, BigDecimal dueAmount, VendorBillStatus excludeStatus);
}
