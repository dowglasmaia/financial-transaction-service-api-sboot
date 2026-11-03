package com.dowglasmaia.financialtransaction.services.impl;

import com.dowglasmaia.financialtransaction.account.application.usecase.GetCurrentAccountUseCase;
import com.dowglasmaia.financialtransaction.account.domain.Account;
import com.dowglasmaia.financialtransaction.entity.Transaction;
import com.dowglasmaia.financialtransaction.transaction.domain.TransactionStatus;
import com.dowglasmaia.financialtransaction.transaction.domain.TransactionType;
import com.dowglasmaia.financialtransaction.exeptions.ObjectNotFoundExeption;
import com.dowglasmaia.financialtransaction.repository.TransactionRepository;
import com.dowglasmaia.financialtransaction.services.TransactionService;
import com.dowglasmaia.financialtransaction.transaction.domain.DateConverter;
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
    private final GetCurrentAccountUseCase getCurrentAccount;


    @Autowired
    public TransactionServiceImpl(TransactionRepository transactionRepository,
                                  GetCurrentAccountUseCase getCurrentAccount
    ){
        this.transactionRepository = transactionRepository;
        this.getCurrentAccount = getCurrentAccount;
    }

    @Override
    @Transactional
    public void save(TransactionType type, String accountId, BigDecimal amount, String agencyId){
        record(type, accountId, amount, agencyId, TransactionStatus.COMPLETED);
    }

    @Override
    @Transactional
    public String record(TransactionType type, String accountId, BigDecimal amount, String agencyId, TransactionStatus status){
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
    public void updateStatus(String id, TransactionStatus status){
        transactionRepository.updateStatus(id, status);
    }

    @Override
    public TransactionsResponseDTO findByDateRange(String initialDate, String finalDate){
        log.info("Start findByDateRange Transactions");

        Account accountEntity = getCurrentAccount.execute();

        var startDate = DateConverter.convertStringToOffsetDateTime(initialDate);
        log.info("startDate: {}", startDate);

        var endDate = DateConverter.convertStringToOffsetDateTime(finalDate);
        log.info("endDate: {}", endDate);

        var transactionsEntity = transactionRepository.findByStatusAndAccountIdAndDateHourBetween(
              TransactionStatus.COMPLETED, accountEntity.getId(), startDate, endDate);

        var transactionsResponse = toTransactionsResponse(transactionsEntity);

        transactionsResponse.setBalance(accountEntity.getBalance());

        return transactionsResponse;
    }

    @Override
    public TransactionResponseDTO findById(String id){
        Account account = getCurrentAccount.execute();

        var transaction = transactionRepository.findByIdAndAccountId(id, account.getId())
              .orElseThrow(() -> new ObjectNotFoundExeption("Transaction not found", HttpStatus.FOUND));

        return toTransactionRequest(transaction);

    }
}
