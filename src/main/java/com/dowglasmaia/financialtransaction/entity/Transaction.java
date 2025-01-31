package com.dowglasmaia.financialtransaction.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "transaction")
public class Transaction {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    private OffsetDateTime dateHour;

    private BigDecimal amount;

    private String accountId;

    private String agencyId;

    @Enumerated(EnumType.STRING)
    private TransactionTypeEnum transactionType;

}

