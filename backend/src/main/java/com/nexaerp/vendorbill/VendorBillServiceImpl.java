package com.nexaerp.vendorbill;

import com.nexaerp.account.Account;
import com.nexaerp.account.AccountRepository;
import com.nexaerp.common.exception.BusinessRuleException;
import com.nexaerp.common.exception.ResourceNotFoundException;
import com.nexaerp.journal.*;
import com.nexaerp.party.Party;
import com.nexaerp.party.PartyRepository;
import com.nexaerp.settings.SettingKey;
import com.nexaerp.settings.SystemSettingsService;
import com.nexaerp.vendorbill.dto.VendorBillItemRequestDto;
import com.nexaerp.vendorbill.dto.VendorBillItemResponseDto;
import com.nexaerp.vendorbill.dto.VendorBillRequestDto;
import com.nexaerp.vendorbill.dto.VendorBillResponseDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Year;
import java.util.List;
import java.util.stream.Collectors;


@Service
@RequiredArgsConstructor
public class VendorBillServiceImpl implements VendorBillService {

    private final VendorBillRepository vendorBillRepository;
    private final VendorBillItemRepository vendorBillItemRepository;
    private final PartyRepository partyRepository;
    private final AccountRepository accountRepository;
    private final JournalEntryRepository journalEntryRepository;
    private final JournalLineRepository journalLineRepository;
    private final SystemSettingsService systemSettingsService;


    @Override
    @Transactional
    public VendorBillResponseDto create(VendorBillRequestDto request) {
        // Find the vendor party
        Party party = partyRepository.findById(request.getPartyId())
                .orElseThrow(() -> new ResourceNotFoundException("Party not found"));

        // Build the vendor bill header
        VendorBill bill = new VendorBill();
        bill.setBillNumber(generateBillNumber());
        bill.setBillDate(request.getBillDate());

        // If posting Date not provided, use billDate
        bill.setPostingDate(request.getPostingDate() != null
                ? request.getPostingDate()
                : request.getBillDate());

        bill.setVendorBillRef(request.getVendorBillRef());
        bill.setParty(party);
        bill.setBillType(request.getBillType());
        bill.setStatus(VendorBillStatus.DRAFT);
        bill.setCurrencyCode(request.getCurrencyCode() != null
                ? request.getCurrencyCode() : "BDT");
        bill.setExchangeRate(BigDecimal.ONE);
        bill.setPaymentTerms(request.getPaymentTerms() != null
                ? request.getPaymentTerms() : party.getPaymentTerms());
        bill.setReferenceType(request.getReferenceType() != null
                ? request.getReferenceType() : VendorBillReferenceType.MANUAL);
        bill.setReferenceId(request.getReferenceId());
        bill.setNotes(request.getNotes());

        // Calculate due date from bill date + payment terms
        bill.setDueDate(bill.getBillDate().plusDays(bill.getPaymentTerms()));

        VendorBill saved = vendorBillRepository.save(bill);

        // Build and save all line items
        List<VendorBillItem> items = request.getItems().stream()
                .map(itemDto -> buildItem(itemDto, saved))
                .collect(Collectors.toList());

        vendorBillItemRepository.saveAll(items);

        // Calculate and store totals in header
        calculateAndSaveTotals(saved, items);

        return toResponse(saved);
    }

    @Override
    @Transactional
    public VendorBillResponseDto update(Long id, VendorBillRequestDto request) {
        VendorBill bill = vendorBillRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Vendor bill not found"));

        // Only DRAFT bills can be updated
        if (!bill.getStatus().equals(VendorBillStatus.DRAFT)) {
            throw new BusinessRuleException("Only DRAFT bills can be updated");
        }

        Party party = partyRepository.findById(request.getPartyId())
                .orElseThrow(() -> new ResourceNotFoundException("Party not found"));

        // Update header fields
        bill.setParty(party);
        bill.setBillDate(request.getBillDate());
        bill.setPostingDate(request.getPostingDate() != null
                ? request.getPostingDate() : request.getBillDate());
        bill.setVendorBillRef(request.getVendorBillRef());
        bill.setBillType(request.getBillType());
        bill.setPaymentTerms(request.getPaymentTerms() != null
                ? request.getPaymentTerms() : 30);
        bill.setDueDate(bill.getBillDate().plusDays(bill.getPaymentTerms()));
        bill.setReferenceType(request.getReferenceType() != null
                ? request.getReferenceType() : VendorBillReferenceType.MANUAL);
        bill.setReferenceId(request.getReferenceId());
        bill.setNotes(request.getNotes());

        // Delete old items and save new ones
        vendorBillItemRepository.deleteAll(bill.getItems());

        List<VendorBillItem> items = request.getItems().stream()
                .map(itemDto -> buildItem(itemDto, bill))
                .collect(Collectors.toList());

        vendorBillItemRepository.saveAll(items);
        calculateAndSaveTotals(bill, items);

        return toResponse(vendorBillRepository.save(bill));
    }

    @Override
    public VendorBillResponseDto getById(Long id) {
        VendorBill bill = vendorBillRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Vendor bill not found"));
        return toResponse(bill);
    }

    @Override
    public List<VendorBillResponseDto> getAll() {
        return vendorBillRepository.findAll()
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<VendorBillResponseDto> getByParty(Long partyId) {
        return vendorBillRepository.findByPartyId(partyId)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<VendorBillResponseDto> getByStatus(VendorBillStatus status) {
        return vendorBillRepository.findByStatus(status)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<VendorBillResponseDto> getByBillType(VendorBillType billType) {
        return vendorBillRepository.findByBillType(billType)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public VendorBillResponseDto approve(Long id) {
        VendorBill bill = vendorBillRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Vendor bill not found"));

        // Only DRAFT bills can be approved
        if (!bill.getStatus().equals(VendorBillStatus.DRAFT)) {
            throw new BusinessRuleException("Only DRAFT bills can be approved");
        }

        bill.setStatus(VendorBillStatus.APPROVED);
        bill.setApprovedAt(LocalDateTime.now());

        return toResponse(vendorBillRepository.save(bill));
    }

    @Override
    @Transactional
    public VendorBillResponseDto post(Long id) {
        VendorBill bill = vendorBillRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Vendor bill not found"));

        if (bill.getStatus() == VendorBillStatus.POSTED) {
            throw new BusinessRuleException("Vendor bill is already posted");
        }

        if (bill.getStatus() == VendorBillStatus.CANCELLED) {
            throw new BusinessRuleException("Cannot post a cancelled vendor bill");
        }

        // Only APPROVED bills can be posted
        if (!bill.getStatus().equals(VendorBillStatus.APPROVED)) {
            throw new BusinessRuleException("Only APPROVED bills can be posted");
        }

        List<VendorBillItem> items = vendorBillItemRepository.findByVendorBillId(bill.getId());
        if (items == null || items.isEmpty()) {
            throw new BusinessRuleException("Cannot post a vendor bill with zero items");
        }

        if (journalEntryRepository.findBySourceTypeAndSourceId(JournalSourceType.VENDOR_BILL, bill.getId()).isPresent()) {
            throw new BusinessRuleException("Journal entry already exists for this vendor bill");
        }

        // Create automatic journal entry
        createJournalEntry(bill);

        bill.setStatus(VendorBillStatus.POSTED);
        bill.setPostedAt(LocalDateTime.now());

        return toResponse(vendorBillRepository.save(bill));
    }

    @Override
    @Transactional
    public VendorBillResponseDto cancel(Long id, VendorBillCancelledReason reason) {
        VendorBill bill = vendorBillRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Vendor bill not found"));

        if (bill.getStatus() == VendorBillStatus.CANCELLED) {
            throw new BusinessRuleException("Vendor bill is already cancelled");
        }

        // Paid bills cannot be cancelled
        if (bill.getStatus().equals(VendorBillStatus.PAID)) {
            throw new BusinessRuleException("Paid bills cannot be cancelled");
        }

        // Reason is required
        if (reason == null) {
            throw new BusinessRuleException("Cancelled reason is required");
        }

        // If already posted, reverse the journal entry
        if (bill.getStatus().equals(VendorBillStatus.POSTED) ||
                bill.getStatus().equals(VendorBillStatus.PARTIAL)) {
            reverseJournalEntry(bill);
        }

        bill.setStatus(VendorBillStatus.CANCELLED);
        bill.setCancelledReason(reason);
        bill.setDueAmount(BigDecimal.ZERO);

        return toResponse(vendorBillRepository.save(bill));
    }


    // ---Privet Helpers---
    private VendorBillItem buildItem(VendorBillItemRequestDto dto, VendorBill bill) {

        // Find the expense account for this item
        Account expenseAccount = accountRepository.findById(dto.getExpenseAccountId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Expense account not found: " + dto.getExpenseAccountId()));

        // Calculate subTotal = quantity x unitPrice
        BigDecimal subTotal = dto.getQuantity()
                .multiply(dto.getUnitPrice())
                .setScale(2, RoundingMode.HALF_UP);

        // Calculate discount amount from percentage
        BigDecimal discountAmount = subTotal
                .multiply(dto.getDiscountPercent())
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);

        // Amount after discount
        BigDecimal afterDiscount = subTotal.subtract(discountAmount);

        // Calculate VAT (Input VAT )
        BigDecimal vatAmount = afterDiscount
                .multiply(dto.getVatRate())
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);

        // Calculate TDS (Tax Deducted at Source - we pay to government)
        BigDecimal tdsAmount = afterDiscount
                .multiply(dto.getTdsRate())
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);

        // lineTotal = afterDiscount + VAT (TDS is deducted at payment time)
        BigDecimal lineTotal = afterDiscount.add(vatAmount);

        return VendorBillItem.builder()
                .vendorBill(bill)
                .productId(dto.getProductId())
                .expenseAccount(expenseAccount)
                .costCenterId(dto.getCostCenterId())
                .description(dto.getDescription())
                .quantity(dto.getQuantity())
                .unitPrice(dto.getUnitPrice())
                .discountPercent(dto.getDiscountPercent())
                .discountAmount(discountAmount)
                .vatRate(dto.getVatRate())
                .vatAmount(vatAmount)
                .tdsRate(dto.getTdsRate())
                .tdsAmount(tdsAmount)
                .subTotal(subTotal)
                .lineTotal(lineTotal)
                .build();
    }

    private void calculateAndSaveTotals(VendorBill bill, List<VendorBillItem> items) {

        // Sum all item subtotals
        BigDecimal subTotal = items.stream()
                .map(VendorBillItem::getSubTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Sum all discounts
        BigDecimal discountAmount = items.stream()
                .map(VendorBillItem::getDiscountAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Sum all Input VAT
        BigDecimal vatAmount = items.stream()
                .map(VendorBillItem::getVatAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Sum all TDS amounts
        BigDecimal tdsAmount = items.stream()
                .map(VendorBillItem::getTdsAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // grandTotal = subTotal - discount + VAT
        BigDecimal grandTotal = subTotal.subtract(discountAmount).add(vatAmount);

        // netPayable = grandTotal - TDS (vendor receives less because we deduct TDS)
        BigDecimal netPayable = grandTotal.subtract(tdsAmount);

        bill.setSubTotal(subTotal);
        bill.setDiscountAmount(discountAmount);
        bill.setVatAmount(vatAmount);
        bill.setTdsAmount(tdsAmount);
        bill.setGrandTotal(grandTotal);
        bill.setNetPayable(netPayable);
        bill.setPaidAmount(BigDecimal.ZERO);
        bill.setDueAmount(netPayable);

        vendorBillRepository.save(bill);
    }

    private void createJournalEntry(VendorBill bill) {

        // Get Accounts Payable account
        Account payable = systemSettingsService.getAccount(
                SettingKey.DEFAULT_PAYABLE_ACCOUNT);

        // Get TDS Payable account
        Account tdsPayable = systemSettingsService.getAccount(
                SettingKey.DEFAULT_TDS_PAYABLE);

        // Create the journal entry header
        JournalEntry entry = new JournalEntry();
        entry.setEntryNumber(generateJournalNumber());
        entry.setDate(bill.getPostingDate());
        entry.setDescription("Vendor Bill - " + bill.getBillNumber());
        entry.setType(JournalEntryType.PURCHASE);
        entry.setStatus(JournalStatus.POSTED);
        entry.setSourceType(JournalSourceType.VENDOR_BILL);
        entry.setSourceId(bill.getId());
        entry.setTotalAmount(bill.getGrandTotal());
        entry.setReferenceNumber(bill.getBillNumber());

        JournalEntry saved = journalEntryRepository.save(entry);

        // Get all items to create expense lines per item
        List<VendorBillItem> items = vendorBillItemRepository
                .findByVendorBillId(bill.getId());

        // Create one debit line per item for the expense account
        for (VendorBillItem item : items) {

            // Debit — Expense Account (cost of this item)
            BigDecimal expenseAmount = item.getSubTotal()
                    .subtract(item.getDiscountAmount());

            JournalLine expenseLine = new JournalLine();
            expenseLine.setJournalEntry(saved);
            expenseLine.setAccount(item.getExpenseAccount());
            expenseLine.setDebit(expenseAmount);
            expenseLine.setCredit(BigDecimal.ZERO);
            expenseLine.setDescription(item.getDescription());
            journalLineRepository.save(expenseLine);

            // Update expense account balance
            updateBalance(item.getExpenseAccount(), expenseAmount, BigDecimal.ZERO);

            // Debit — Input VAT (if VAT exists)
            if (item.getVatAmount().compareTo(BigDecimal.ZERO) > 0) {
                Account inputVat = accountRepository.findByCode("1130")
                        .orElseThrow(() -> new ResourceNotFoundException(
                                "Input VAT account not found"));

                JournalLine vatLine = new JournalLine();
                vatLine.setJournalEntry(saved);
                vatLine.setAccount(inputVat);
                vatLine.setDebit(item.getVatAmount());
                vatLine.setCredit(BigDecimal.ZERO);
                vatLine.setDescription("Input VAT - " + item.getDescription());
                journalLineRepository.save(vatLine);

                updateBalance(inputVat, item.getVatAmount(), BigDecimal.ZERO);
            }
        }

        // Credit — Accounts Payable (net amount vendor will receive)
        JournalLine payableLine = new JournalLine();
        payableLine.setJournalEntry(saved);
        payableLine.setAccount(payable);
        payableLine.setDebit(BigDecimal.ZERO);
        payableLine.setCredit(bill.getNetPayable());
        payableLine.setDescription("Accounts Payable - " + bill.getBillNumber());
        journalLineRepository.save(payableLine);
        updateBalance(payable, BigDecimal.ZERO, bill.getNetPayable());

        // Credit — TDS Payable (if TDS exists)
        if (bill.getTdsAmount().compareTo(BigDecimal.ZERO) > 0) {
            JournalLine tdsLine = new JournalLine();
            tdsLine.setJournalEntry(saved);
            tdsLine.setAccount(tdsPayable);
            tdsLine.setDebit(BigDecimal.ZERO);
            tdsLine.setCredit(bill.getTdsAmount());
            tdsLine.setDescription("TDS Payable - " + bill.getBillNumber());
            journalLineRepository.save(tdsLine);
            updateBalance(tdsPayable, BigDecimal.ZERO, bill.getTdsAmount());
        }
    }

    private void reverseJournalEntry(VendorBill bill) {

        // Find the original journal entry for this vendor bill
        journalEntryRepository
                .findBySourceTypeAndSourceId(JournalSourceType.VENDOR_BILL, bill.getId())
                .ifPresent(original -> {
                    if (original.getStatus() == JournalStatus.REVERSED) {
                        throw new BusinessRuleException("Journal entry is already reversed");
                    }

                    // Create a reversal entry
                    JournalEntry reversal = new JournalEntry();
                    reversal.setEntryNumber(generateJournalNumber());
                    reversal.setDate(LocalDate.now());
                    reversal.setDescription("Reversal - " + bill.getBillNumber());
                    reversal.setType(JournalEntryType.PURCHASE);
                    reversal.setStatus(JournalStatus.POSTED);
                    reversal.setSourceType(JournalSourceType.VENDOR_BILL);
                    reversal.setSourceId(bill.getId());
                    reversal.setTotalAmount(original.getTotalAmount());
                    reversal.setReversedFromId(original.getId());
                    reversal.setReferenceNumber("REV-" + original.getReferenceNumber());

                    JournalEntry savedReversal = journalEntryRepository.save(reversal);

                    // Reverse all lines (swap debit and credit)
                    List<JournalLine> originalLines =
                            journalLineRepository.findByJournalEntryId(original.getId());

                    originalLines.forEach(line -> {
                        JournalLine reversalLine = new JournalLine();
                        reversalLine.setJournalEntry(savedReversal);
                        reversalLine.setAccount(line.getAccount());
                        reversalLine.setDebit(line.getCredit());   // swap
                        reversalLine.setCredit(line.getDebit());   // swap
                        reversalLine.setDescription("Reversal: " + line.getDescription());
                        journalLineRepository.save(reversalLine);

                        // Update account balance with reversed amounts
                        updateBalance(line.getAccount(), line.getCredit(), line.getDebit());
                    });

                    // Mark original entry as reversed
                    original.setStatus(JournalStatus.REVERSED);
                    journalEntryRepository.save(original);
                });
    }

    private void updateBalance(Account account, BigDecimal debit, BigDecimal credit) {
        switch (account.getType()) {
            case ASSET:
            case EXPENSE:
                // Debit increases, Credit decreases
                account.setCurrentBalance(
                        account.getCurrentBalance().add(debit).subtract(credit));
                break;
            case LIABILITY:
            case EQUITY:
            case REVENUE:
                // Credit increases, Debit decreases
                account.setCurrentBalance(
                        account.getCurrentBalance().add(credit).subtract(debit));
                break;
        }
        accountRepository.save(account);
    }

    private String generateBillNumber() {
        int year = Year.now().getValue();
        return vendorBillRepository.findTopByOrderByIdDesc()
                .map(last -> {
                    String[] parts = last.getBillNumber().split("-");
                    int next = Integer.parseInt(parts[2]) + 1;
                    return String.format("BILL-%d-%06d", year, next);
                })
                .orElse(String.format("BILL-%d-%06d", year, 1));
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


    // ---Mappers----

    private VendorBillResponseDto toResponse(VendorBill bill) {
        List<VendorBillItem> items =
                vendorBillItemRepository.findByVendorBillId(bill.getId());

        return VendorBillResponseDto.builder()
                .id(bill.getId())
                .billNumber(bill.getBillNumber())
                .billDate(bill.getBillDate())
                .postingDate(bill.getPostingDate())
                .dueDate(bill.getDueDate())
                .vendorBillRef(bill.getVendorBillRef())
                .partyId(bill.getParty().getId())
                .partyName(bill.getParty().getName())
                .billType(bill.getBillType())
                .status(bill.getStatus())
                .currencyCode(bill.getCurrencyCode())
                .exchangeRate(bill.getExchangeRate())
                .paymentTerms(bill.getPaymentTerms())
                .referenceType(bill.getReferenceType())
                .referenceId(bill.getReferenceId())
                .notes(bill.getNotes())
                .cancelledReason(bill.getCancelledReason())
                .subTotal(bill.getSubTotal())
                .discountAmount(bill.getDiscountAmount())
                .vatAmount(bill.getVatAmount())
                .tdsAmount(bill.getTdsAmount())
                .grandTotal(bill.getGrandTotal())
                .netPayable(bill.getNetPayable())
                .paidAmount(bill.getPaidAmount())
                .dueAmount(bill.getDueAmount())
                .approvedAt(bill.getApprovedAt())
                .postedAt(bill.getPostedAt())
                .createdAt(bill.getCreatedAt())
                .updatedAt(bill.getUpdatedAt())
                .items(items.stream()
                        .map(this::toItemResponse)
                        .collect(Collectors.toList()))
                .build();
    }

    private VendorBillItemResponseDto toItemResponse(VendorBillItem item) {
        return VendorBillItemResponseDto.builder()
                .id(item.getId())
                .productId(item.getProductId())
                .expenseAccountId(item.getExpenseAccount().getId())
                .expenseAccountName(item.getExpenseAccount().getName())
                .expenseAccountCode(item.getExpenseAccount().getCode())
                .costCenterId(item.getCostCenterId())
                .description(item.getDescription())
                .quantity(item.getQuantity())
                .unitPrice(item.getUnitPrice())
                .discountPercent(item.getDiscountPercent())
                .discountAmount(item.getDiscountAmount())
                .vatRate(item.getVatRate())
                .vatAmount(item.getVatAmount())
                .tdsRate(item.getTdsRate())
                .tdsAmount(item.getTdsAmount())
                .subTotal(item.getSubTotal())
                .lineTotal(item.getLineTotal())
                .build();
    }
}
