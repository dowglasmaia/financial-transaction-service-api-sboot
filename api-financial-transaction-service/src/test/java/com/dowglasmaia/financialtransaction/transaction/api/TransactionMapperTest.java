package com.dowglasmaia.financialtransaction.transaction.api;

import static org.assertj.core.api.Assertions.assertThat;

import com.dowglasmaia.financialtransaction.transaction.domain.DailyTotal;
import com.dowglasmaia.financialtransaction.transaction.domain.Statement;
import com.dowglasmaia.financialtransaction.transaction.domain.StatementTotals;
import com.dowglasmaia.financialtransaction.transaction.domain.Transaction;
import com.dowglasmaia.financialtransaction.transaction.domain.TransactionStatus;
import com.dowglasmaia.financialtransaction.transaction.domain.TransactionType;
import com.dowglasmaia.financialtransaction.transaction.domain.TransactionsSummary;
import com.dowglasmaia.provider.model.TransactionResponseDTO;
import com.dowglasmaia.provider.model.TransactionsResponseDTO;
import com.dowglasmaia.provider.model.TransactionsSummaryResponseDTO;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/** The mapper only copies: totals and paging come from the backend (the old in-mapper sums moved to the ledger queries). */
class TransactionMapperTest {

    private static Transaction row(String id, TransactionType type, String amount) {
        return Transaction.restore(id, type, "acc-1", new BigDecimal(amount), "agency-1", TransactionStatus.COMPLETED, OffsetDateTime.parse("2026-03-11T12:00:00Z"));
    }

    @Test
    void copiesTheRowsTheTotalsTheBalanceAndThePaging() {
        String id = UUID.randomUUID().toString();
        Statement statement = Statement.of(List.of(row(id, TransactionType.PIX, "10.00")), new BigDecimal("4990.00"),
              new StatementTotals(new BigDecimal("100.00"), new BigDecimal("35.00"), 7), 1, 5);

        TransactionsResponseDTO response = TransactionMapper.toTransactionsResponse(statement);

        assertThat(response.getTransaction()).hasSize(1);
        TransactionResponseDTO first = response.getTransaction().get(0);
        assertThat(first.getId()).isEqualTo(UUID.fromString(id));
        assertThat(first.getTransactionType()).isEqualTo(TransactionResponseDTO.TransactionTypeEnum.PIX);
        assertThat(first.getAmount()).isEqualByComparingTo("10.00");
        assertThat(response.getSumIncoming()).isEqualByComparingTo("100.00");
        assertThat(response.getSumOutgoing()).isEqualByComparingTo("35.00");
        assertThat(response.getBalance()).isEqualByComparingTo("4990.00");
        assertThat(response.getPage()).isEqualTo(1);
        assertThat(response.getSize()).isEqualTo(5);
        assertThat(response.getTotalElements()).isEqualTo(7L);
    }

    @Test
    void anEmptyStatementGivesAnEmptyListAndZeroTotals() {
        Statement statement = Statement.of(List.of(), BigDecimal.ZERO, new StatementTotals(BigDecimal.ZERO, BigDecimal.ZERO, 0), 0, 20);

        TransactionsResponseDTO response = TransactionMapper.toTransactionsResponse(statement);

        assertThat(response.getTransaction()).isEmpty();
        assertThat(response.getTotalElements()).isZero();
    }

    @Test
    void mapsTheSummaryWithItsDailySeries() {
        TransactionsSummary summary = new TransactionsSummary(new BigDecimal("100.00"), new BigDecimal("35.00"), new BigDecimal("4900.00"),
              List.of(new DailyTotal(LocalDate.of(2026, 3, 11), new BigDecimal("100.00"), new BigDecimal("35.00"))));

        TransactionsSummaryResponseDTO response = TransactionMapper.toSummaryResponse(summary);

        assertThat(response.getSeries()).hasSize(1);
        assertThat(response.getSeries().get(0).getDate()).isEqualTo(LocalDate.of(2026, 3, 11));
        assertThat(response.getSeries().get(0).getIncoming()).isEqualByComparingTo("100.00");
        assertThat(response.getSumOutgoing()).isEqualByComparingTo("35.00");
        assertThat(response.getBalance()).isEqualByComparingTo("4900.00");
    }
}
