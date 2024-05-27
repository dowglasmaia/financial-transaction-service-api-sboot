package com.dowglasmaia.exactbank.services.impl;

import com.dowglasmaia.exactbank.entity.Account;
import com.dowglasmaia.exactbank.entity.Transaction;
import com.dowglasmaia.exactbank.entity.TransactionTypeEnum;
import com.dowglasmaia.exactbank.exeptions.ObjectNotFoundExeption;
import com.dowglasmaia.exactbank.exeptions.UnprocessableEntityExeption;
import com.dowglasmaia.exactbank.repository.TransactionRepository;
import com.dowglasmaia.exactbank.services.TransactionService;
import com.dowglasmaia.exactbank.services.client.account.FindAccountByNumberAndUserIdService;
import com.dowglasmaia.exactbank.services.model.TransactionRequestDTO;
import com.dowglasmaia.exactbank.utils.DateConverter;
import com.dowglasmaia.provider.model.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

import static com.dowglasmaia.exactbank.services.impl.mapper.TransactionMapper.toTransactionRequest;
import static com.dowglasmaia.exactbank.services.impl.mapper.TransactionMapper.toTransactionsResponse;


@Slf4j
@Service
public class TransactionServiceImpl implements TransactionService {

    private final TransactionRepository transactionRepository;
    private final FindAccountByNumberAndUserIdService findAccountByNumberAndUserIdService;


    @Autowired
    public TransactionServiceImpl(TransactionRepository transactionRepository,
                                  FindAccountByNumberAndUserIdService findAccountByNumberAndUserIdService
    ){
        this.transactionRepository = transactionRepository;
        this.findAccountByNumberAndUserIdService = findAccountByNumberAndUserIdService;
    }

    @Override
    public void save(Object transaction, String accountId, BigDecimal amount, String agencyId){
        log.info("Start save Transaction");
        var transactionEntity = Transaction.builder()
              .transactionType(getTransactionType(transaction))
              .accountId(accountId)
              .amount(amount)
              .agencyId(agencyId)
              .dateHour(OffsetDateTime.now())
              .build();

        transactionRepository.save(transactionEntity);
    }

    @Override
    public TransactionsResponseDTO findByDateRange(String initialDate, String finalDate){
        log.info("Start findByDateRange Transactions");

        Account accountEntity = findAccountByNumberAndUserIdService.findByNumberAndUserId();

        var startDate = DateConverter.convertStringToOffsetDateTime(initialDate);
        log.info("startDate: {}", startDate);

        var endDate = DateConverter.convertStringToOffsetDateTime(finalDate);
        log.info("endDate: {}", endDate);

        var transactionsEntity = transactionRepository.findByDateHourBetween(startDate, endDate);

        var transactionsResponse = toTransactionsResponse(transactionsEntity);

        transactionsResponse.setBalance(accountEntity.getBalance());

        return transactionsResponse;
    }

    @Override
    public TransactionResponseDTO findById(String id){
        var transaction = transactionRepository.findById(id)
              .orElseThrow(() -> new ObjectNotFoundExeption("Transaction not found", HttpStatus.FOUND));

        return toTransactionRequest(transaction);

    }


    private TransactionTypeEnum getTransactionType(Object object){
        if (object instanceof WithdrawRequestDTO)
            return TransactionTypeEnum.WITHDRAW;

        if (object instanceof DepositRequestDTO)
            return TransactionTypeEnum.DEPOSIT;

        if (object instanceof PixRequestDTO)
            return TransactionTypeEnum.PIX;

        if (object instanceof MobileRechargeRequestDTO)
            return TransactionTypeEnum.PHONE_RECHARGE;

        throw new UnprocessableEntityExeption("Type of transaction is not valid", HttpStatus.UNPROCESSABLE_ENTITY);
    }
}
