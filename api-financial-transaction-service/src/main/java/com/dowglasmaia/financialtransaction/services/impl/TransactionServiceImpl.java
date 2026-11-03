package com.dowglasmaia.financialtransaction.services.impl;

import com.dowglasmaia.financialtransaction.account.application.AccountService;
import com.dowglasmaia.financialtransaction.account.domain.Account;
import com.dowglasmaia.financialtransaction.entity.Transaction;
import com.dowglasmaia.financialtransaction.entity.TransactionStatusEnum;
import com.dowglasmaia.financialtransaction.entity.TransactionTypeEnum;
import com.dowglasmaia.financialtransaction.exeptions.ObjectNotFoundExeption;
import com.dowglasmaia.financialtransaction.repository.TransactionRepository;
import com.dowglasmaia.financialtransaction.services.TransactionService;
import com.dowglasmaia.financialtransaction.utils.DateConverter;
import com.dowglasmaia.provider.model.TransactionResponseDTO;
import com.dowglasmaia.provider.model.TransactionsResponseDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

import static com.dowglasmaia.financialtransaction.services.impl.mapper.TransactionMapper.toTransactionRequest;
import static com.dowglasmaia.financialtransaction.services.impl.mapper.TransactionMapper.toTransactionsResponse;


@Slf4j
@Service
public class TransactionServiceImpl implements TransactionService {

    private final TransactionRepository transactionRepository;
    private final AccountService accountService;


    @Autowired
    public TransactionServiceImpl(TransactionRepository transactionRepository,
                                  AccountService accountService
    ){
        this.transactionRepository = transactionRepository;
        this.accountService = accountService;
    }

    @Override
    @Transactional
    public void save(TransactionTypeEnum type, String accountId, BigDecimal amount, String agencyId){
        record(type, accountId, amount, agencyId, TransactionStatusEnum.COMPLETED);
    }

    @Override
    @Transactional
    public String record(TransactionTypeEnum type, String accountId, BigDecimal amount, String agencyId, TransactionStatusEnum status){
        log.info("Start save Transaction");
        var transactionEntity = Transaction.builder()
              .transactionType(type)
              .accountId(accountId)
              .amount(amount)
              .agencyId(agencyId)
              .status(status)
              .dateHour(OffsetDateTime.now())
              .build();

        return transactionRepository.save(transactionEntity).getId();
    }

    @Override
    @Transactional
    public void updateStatus(String id, TransactionStatusEnum status){
        transactionRepository.updateStatus(id, status);
    }

    @Override
    public TransactionsResponseDTO findByDateRange(String initialDate, String finalDate){
        log.info("Start findByDateRange Transactions");

        Account accountEntity = accountService.currentAccount();

        var startDate = DateConverter.convertStringToOffsetDateTime(initialDate);
        log.info("startDate: {}", startDate);

        var endDate = DateConverter.convertStringToOffsetDateTime(finalDate);
        log.info("endDate: {}", endDate);

        var transactionsEntity = transactionRepository.findByStatusAndAccountIdAndDateHourBetween(
              TransactionStatusEnum.COMPLETED, accountEntity.getId(), startDate, endDate);

        var transactionsResponse = toTransactionsResponse(transactionsEntity);

        transactionsResponse.setBalance(accountEntity.getBalance());

        return transactionsResponse;
    }

    @Override
    public TransactionResponseDTO findById(String id){
        Account account = accountService.currentAccount();

        var transaction = transactionRepository.findByIdAndAccountId(id, account.getId())
              .orElseThrow(() -> new ObjectNotFoundExeption("Transaction not found", HttpStatus.FOUND));

        return toTransactionRequest(transaction);

    }
}
