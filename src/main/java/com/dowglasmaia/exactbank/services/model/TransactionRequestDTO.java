package com.dowglasmaia.exactbank.services.model;

import com.dowglasmaia.exactbank.entity.TransactionTypeEnum;
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
public class TransactionRequestDTO {

    private OffsetDateTime dateHour;
    private BigDecimal amount;
    private String accountId;
    private TransactionTypeEnum transactionType;

}
