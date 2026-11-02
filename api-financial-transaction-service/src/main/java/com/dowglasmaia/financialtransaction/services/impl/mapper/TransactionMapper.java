package com.dowglasmaia.financialtransaction.services.impl.mapper;

import com.dowglasmaia.financialtransaction.entity.Transaction;
import com.dowglasmaia.financialtransaction.entity.TransactionTypeEnum;
import com.dowglasmaia.provider.model.TransactionResponseDTO;
import com.dowglasmaia.provider.model.TransactionsResponseDTO;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

public class TransactionMapper {

    public static TransactionsResponseDTO toTransactionsResponse(List<Transaction> transactions){
        TransactionsResponseDTO transactionsResponse = new TransactionsResponseDTO();

        List<TransactionResponseDTO> newTransactions = transactions.stream()
              .map(transactionMap -> {
                  TransactionResponseDTO transaction = new TransactionResponseDTO();
                  transaction.setTransactionType(transactionMap.getTransactionType().name());
                  transaction.setAmount(transactionMap.getAmount());
                  transaction.setDateHour(transactionMap.getDateHour());
                  return transaction;
              })
              .collect(Collectors.toList());

        // sumIncoming e sumOutgoing
        BigDecimal sumIncoming = transactions.stream()
              .filter(transaction -> transaction.getTransactionType() == TransactionTypeEnum.DEPOSIT)
              .map(Transaction::getAmount)
              .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal sumOutgoing = transactions.stream()
              .filter(transaction -> transaction.getTransactionType() == TransactionTypeEnum.PIX ||
                    transaction.getTransactionType() == TransactionTypeEnum.PHONE_RECHARGE ||
                    transaction.getTransactionType() == TransactionTypeEnum.WITHDRAW)
              .map(Transaction::getAmount)
              .reduce(BigDecimal.ZERO, BigDecimal::add);

        transactionsResponse.setTransaction(newTransactions);
        transactionsResponse.setSumIncoming(sumIncoming);
        transactionsResponse.setSumOutgoing(sumOutgoing);

        return transactionsResponse;
    }

    public static TransactionResponseDTO toTransactionRequest(Transaction transaction){
        TransactionResponseDTO response = new TransactionResponseDTO();
        response.setAmount(transaction.getAmount());
        response.setTransactionType(transaction.getTransactionType().name());
        response.setDateHour(transaction.getDateHour());
        return response;
    }

}
