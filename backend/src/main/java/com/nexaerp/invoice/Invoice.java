package com.nexaerp.invoice;


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

public class Invoice {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String invoiceNumber; // INV-2025-000001

    @Column(nullable = false)
    private LocalDate invoiceDate;

    private LocalDate dueDate;

    @ManyToOne
    @JoinColumn(name = "party_id" , nullable = false)
    private Party party;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private InvoiceStatus status = InvoiceStatus.DRAFT;

    private String currencyCode = "BDT";
    private BigDecimal exchangeRate = BigDecimal.ONE;
    private Integer paymentTerms = 30;
    private String reference;
    private String notes;

    @Enumerated(EnumType.STRING)
    private CancelledReason cancelledReason;

    private String attachmentUrl;
    private Boolean pdfGenerated = false;
    private Integer printCount = 0;

    // Future
    private Long companyId;
    private Long branchId;

    // Calculated & Stored
    @Column(precision = 19, scale = 2)
    private BigDecimal subTotal = BigDecimal.ZERO;

    @Column(precision = 19, scale = 2)
    private BigDecimal discountAmount = BigDecimal.ZERO;

    @Column(precision = 19, scale = 2)
    private BigDecimal vatAmount = BigDecimal.ZERO;

    @Column(precision = 19, scale = 2)
    private BigDecimal grandTotal = BigDecimal.ZERO;

    @Column(precision = 19, scale = 2)
    private BigDecimal paidAmount = BigDecimal.ZERO;

    @Column(precision = 19, scale = 2)
    private BigDecimal dueAmount = BigDecimal.ZERO;

    // Audit
    private LocalDateTime postedAt;
    private Long postedBy;
    private Long createdBy;
    private LocalDateTime createdAt;
    private Long updatedBy;
    private LocalDateTime updatedAt;



    @OneToMany(mappedBy = "invoice", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<InvoiceItem> items;

    @PrePersist
    public void prePersist() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
