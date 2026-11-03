package com.dowglasmaia.financialtransaction.services.impl.mapper;

import com.dowglasmaia.financialtransaction.transaction.infrastructure.persistence.TransactionEntity;
import com.dowglasmaia.financialtransaction.transaction.domain.TransactionType;
import com.dowglasmaia.provider.model.TransactionsResponseDTO;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/** Pins the income/outgoing classification (BR-52) at unit level (spec BE-01). */
class TransactionMapperTest {

    private static TransactionEntity tx(TransactionType type, String amount) {
        return TransactionEntity.builder().transactionType(type).amount(new BigDecimal(amount)).dateHour(OffsetDateTime.now()).build();
    }

    @Test
    void depositIsIncomingAndTheOtherThreeAreOutgoing() {
        TransactionsResponseDTO response = TransactionMapper.toTransactionsResponse(List.of(
              tx(TransactionType.DEPOSIT, "100.00"),
              tx(TransactionType.PIX, "10.00"),
              tx(TransactionType.WITHDRAW, "20.00"),
              tx(TransactionType.PHONE_RECHARGE, "5.00")));

        assertThat(response.getSumIncoming()).isEqualByComparingTo("100");
        assertThat(response.getSumOutgoing()).isEqualByComparingTo("35");
        assertThat(response.getTransaction()).hasSize(4);
    }

    @Test
    void emptyListGivesZeroSums() {
        TransactionsResponseDTO response = TransactionMapper.toTransactionsResponse(List.of());
        assertThat(response.getSumIncoming()).isEqualByComparingTo("0");
        assertThat(response.getSumOutgoing()).isEqualByComparingTo("0");
    }
}
