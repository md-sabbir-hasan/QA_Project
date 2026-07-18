package com.nexaerp.invoice;

import com.nexaerp.common.BaseEntity;
import com.nexaerp.party.Party;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "invoices")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Invoice extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String invoiceNumber;

    @Column(nullable = false)
    private LocalDate invoiceDate;

    private LocalDate dueDate;

    @ManyToOne
    @JoinColumn(name = "party_id", nullable = false)
    private Party party;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private InvoiceStatus status = InvoiceStatus.DRAFT;

    @Column(nullable = false, length = 3)
    @Builder.Default
    private String currencyCode = "BDT";

    @Column(nullable = false, precision = 19, scale = 8)
    @Builder.Default
    private BigDecimal exchangeRate = BigDecimal.ONE;

    @Column(nullable = false, precision = 19, scale = 2)
    @Builder.Default
    private BigDecimal baseGrandTotal = BigDecimal.ZERO;

    @Column(nullable = false, precision = 19, scale = 2)
    @Builder.Default
    private BigDecimal basePaidAmount = BigDecimal.ZERO;

    @Column(nullable = false, precision = 19, scale = 2)
    @Builder.Default
    private BigDecimal baseDueAmount = BigDecimal.ZERO;

    @Builder.Default
    private Integer paymentTerms = 30;

    private String reference;
    private String notes;

    @Enumerated(EnumType.STRING)
    private CancelledReason cancelledReason;

    private String attachmentUrl;

    @Builder.Default
    private Boolean pdfGenerated = false;

    @Builder.Default
    private Integer printCount = 0;

    private Long companyId;
    private Long branchId;

    @Column(precision = 19, scale = 2)
    @Builder.Default
    private BigDecimal subTotal = BigDecimal.ZERO;

    @Column(precision = 19, scale = 2)
    @Builder.Default
    private BigDecimal discountAmount = BigDecimal.ZERO;

    @Column(precision = 19, scale = 2)
    @Builder.Default
    private BigDecimal vatAmount = BigDecimal.ZERO;

    @Column(precision = 19, scale = 2)
    @Builder.Default
    private BigDecimal grandTotal = BigDecimal.ZERO;

    @Column(precision = 19, scale = 2)
    @Builder.Default
    private BigDecimal paidAmount = BigDecimal.ZERO;

    @Column(precision = 19, scale = 2)
    @Builder.Default
    private BigDecimal dueAmount = BigDecimal.ZERO;

    private LocalDateTime postedAt;
    private Long postedBy;

    @OneToMany(
            mappedBy = "invoice",
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    private List<InvoiceItem> items;

    @PrePersist
    public void prePersistInvoice() {
        normalizeCurrencyFields();
    }

    @PreUpdate
    public void preUpdateInvoice() {
        normalizeCurrencyFields();
    }

    private void normalizeCurrencyFields() {

        if (currencyCode == null || currencyCode.isBlank()) {
            currencyCode = "BDT";
        } else {
            currencyCode = currencyCode.trim().toUpperCase();
        }

        if (exchangeRate == null) {
            exchangeRate = BigDecimal.ONE;
        }

        if (baseGrandTotal == null) {
            baseGrandTotal = BigDecimal.ZERO;
        }

        if (basePaidAmount == null) {
            basePaidAmount = BigDecimal.ZERO;
        }

        if (baseDueAmount == null) {
            baseDueAmount = BigDecimal.ZERO;
        }

        if (subTotal == null) {
            subTotal = BigDecimal.ZERO;
        }

        if (discountAmount == null) {
            discountAmount = BigDecimal.ZERO;
        }

        if (vatAmount == null) {
            vatAmount = BigDecimal.ZERO;
        }

        if (grandTotal == null) {
            grandTotal = BigDecimal.ZERO;
        }

        if (paidAmount == null) {
            paidAmount = BigDecimal.ZERO;
        }

        if (dueAmount == null) {
            dueAmount = BigDecimal.ZERO;
        }

        if (status == null) {
            status = InvoiceStatus.DRAFT;
        }

        if (paymentTerms == null) {
            paymentTerms = 30;
        }

        if (pdfGenerated == null) {
            pdfGenerated = false;
        }

        if (printCount == null) {
            printCount = 0;
        }
    }
}