package com.dowglasmaia.financialtransaction.services.client.account.impl;

import com.dowglasmaia.financialtransaction.entity.Account;
import com.dowglasmaia.financialtransaction.exeptions.ObjectNotFoundExeption;
import com.dowglasmaia.financialtransaction.exeptions.UnprocessableEntityExeption;
import com.dowglasmaia.financialtransaction.repository.AccountRepository;
import com.dowglasmaia.financialtransaction.services.client.account.AddAmountService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Slf4j
@Service
public class AddAmountServiceImpl implements AddAmountService {

    static final String INSUFFICIENT_BALANCE = "Operation not performed, there is not enough balance in the account.";

    private final AccountRepository accountRepository;

    @Autowired
    public AddAmountServiceImpl(AccountRepository accountRepository){
        this.accountRepository = accountRepository;
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRED)
    public void debit(String accountNumber, BigDecimal amount){
        log.info("Start debit at account:{}", accountNumber);

        Account account = accountRepository.findByNumber(accountNumber)
              .orElseThrow(() -> new ObjectNotFoundExeption("Account not found.", HttpStatus.FOUND));

        debitById(account.getId(), amount);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRED)
    public void debitById(String accountId, BigDecimal amount){
        // One atomic statement: the balance check and the update cannot be separated by another request.
        if (accountRepository.debit(accountId, amount) == 0) {
            log.error("Operation not performed, there is not enough balance in the account.");
            throw new UnprocessableEntityExeption(INSUFFICIENT_BALANCE, HttpStatus.UNPROCESSABLE_ENTITY);
        }
    }
}
