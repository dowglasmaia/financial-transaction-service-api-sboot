package com.dowglasmaia.exactbank.services.client.account.impl;

import com.dowglasmaia.exactbank.config.db_massa_test.DBService;
import com.dowglasmaia.exactbank.entity.Account;
import com.dowglasmaia.exactbank.exeptions.ObjectNotFoundExeption;
import com.dowglasmaia.exactbank.repository.AccountRepository;
import com.dowglasmaia.exactbank.services.client.account.FindAccountByNumberAndUserIdService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class FindFindAccountByNumberAndUserIdByNumberAndUserIdServiceImpl implements FindAccountByNumberAndUserIdService {

    private final AccountRepository accountRepository;

    @Autowired
    private DBService dbService;

    @Autowired
    public FindFindAccountByNumberAndUserIdByNumberAndUserIdServiceImpl(AccountRepository accountRepository){
        this.accountRepository = accountRepository;
    }

    @Override
    public Account findByNumberAndUserId(){
        String accountNumber = dbService.extractUserAccountFromJwt().getNumber();
        String userId = dbService.extractUserAccountFromJwt().getUserId();

        log.info("ACCOUNT NUMBER: {}", accountNumber);
        log.info("USER_ID: {}", userId);

        var account = accountRepository.findByNumberAndUser(accountNumber, userId);

        log.info("ACCOUNT: {}", account);

        return account.orElseThrow(() -> new ObjectNotFoundExeption("Account not found.", HttpStatus.FOUND));
    }

}
