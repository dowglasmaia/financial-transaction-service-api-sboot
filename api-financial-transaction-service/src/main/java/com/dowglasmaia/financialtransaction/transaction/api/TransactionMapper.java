package com.dowglasmaia.financialtransaction.transaction.api;

import com.dowglasmaia.financialtransaction.transaction.domain.DailyTotal;
import com.dowglasmaia.financialtransaction.transaction.domain.Statement;
import com.dowglasmaia.financialtransaction.transaction.domain.Transaction;
import com.dowglasmaia.financialtransaction.transaction.domain.TransactionsSummary;
import com.dowglasmaia.provider.model.DailyTotalDTO;
import com.dowglasmaia.provider.model.TransactionResponseDTO;
import com.dowglasmaia.provider.model.TransactionsResponseDTO;
import com.dowglasmaia.provider.model.TransactionsSummaryResponseDTO;
import java.util.List;
import java.util.UUID;

/** Copies domain values to the generated DTOs; no rule lives here (totals and paging come from the ledger). */
public class TransactionMapper {

    public static TransactionsResponseDTO toTransactionsResponse(Statement statement) {
        TransactionsResponseDTO response = new TransactionsResponseDTO();
        response.setTransaction(statement.transactions().stream().map(TransactionMapper::toTransactionRequest).toList());
        response.setSumIncoming(statement.sumIncoming());
        response.setSumOutgoing(statement.sumOutgoing());
        response.setBalance(statement.balance());
        response.setPage(statement.page());
        response.setSize(statement.size());
        response.setTotalElements(statement.totalElements());
        return response;
    }

    public static TransactionsSummaryResponseDTO toSummaryResponse(TransactionsSummary summary) {
        TransactionsSummaryResponseDTO response = new TransactionsSummaryResponseDTO();
        response.setSumIncoming(summary.sumIncoming());
        response.setSumOutgoing(summary.sumOutgoing());
        response.setBalance(summary.balance());
        response.setSeries(summary.series().stream().map(TransactionMapper::toDay).toList());
        return response;
    }

    private static DailyTotalDTO toDay(DailyTotal day) {
        DailyTotalDTO dto = new DailyTotalDTO();
        dto.setDate(day.date());
        dto.setIncoming(day.incoming());
        dto.setOutgoing(day.outgoing());
        return dto;
    }

    private static UUID idOf(Transaction transaction) {
        return transaction.getId() == null ? null : UUID.fromString(transaction.getId());
    }

    public static TransactionResponseDTO toTransactionRequest(Transaction transaction) {
        TransactionResponseDTO response = new TransactionResponseDTO();
        response.setId(idOf(transaction));
        response.setAmount(transaction.getAmount());
        response.setTransactionType(TransactionResponseDTO.TransactionTypeEnum.fromValue(transaction.getTransactionType().name()));
        response.setDateHour(transaction.getDateHour());
        return response;
    }

}
