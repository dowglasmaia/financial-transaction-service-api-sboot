package com.dowglasmaia.exactbank.services.client.account.impl;

import com.dowglasmaia.exactbank.entity.Account;
import com.dowglasmaia.exactbank.exeptions.ObjectNotFoundExeption;
import com.dowglasmaia.exactbank.exeptions.UnprocessableEntityExeption;
import com.dowglasmaia.exactbank.repository.AccountRepository;
import com.dowglasmaia.exactbank.services.client.account.AddAmountService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Slf4j
@Service
public class AddAmountServiceImpl implements AddAmountService {

    private final AccountRepository accountRepository;

    @Autowired
    public AddAmountServiceImpl(AccountRepository accountRepository){
        this.accountRepository = accountRepository;
    }

    @Override
    public void addAmountToAccount(String accountNumber, BigDecimal amount){
        log.info("Start AddAmountToAccount at account:{}", accountNumber);

        Account accountEntity = accountRepository.findByNumber(accountNumber)
              .orElseThrow(() -> new ObjectNotFoundExeption("Account not found.", HttpStatus.FOUND));


        if (accountEntity.getBalance().compareTo(amount) > 0) {
            var originalBalance = accountEntity.getBalance();
            var newBalance = originalBalance.subtract(amount);

            accountEntity.setBalance(newBalance);
            accountRepository.save(accountEntity);
        } else {
            log.error("Operation not performed, there is not enough balance in the account.");
            throw new UnprocessableEntityExeption("Operation not performed, there is not enough balance in the account.", HttpStatus.UNPROCESSABLE_ENTITY);
        }
    }
}
