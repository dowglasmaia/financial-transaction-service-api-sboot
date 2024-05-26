package com.dowglasmaia.exactbank.services.client.account.impl;

import com.dowglasmaia.exactbank.entity.Account;
import com.dowglasmaia.exactbank.exeptions.ObjectNotFoundExeption;
import com.dowglasmaia.exactbank.repository.AccountRepository;
import com.dowglasmaia.exactbank.services.client.account.FindAccountByNumberAndUserIdService;
import com.dowglasmaia.exactbank.services.client.account.model.UserAccountDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class FindFindAccountByNumberAndUserIdByNumberAndUserIdServiceImpl implements FindAccountByNumberAndUserIdService {

    private final AccountRepository accountRepository;

    @Autowired
    public FindFindAccountByNumberAndUserIdByNumberAndUserIdServiceImpl(AccountRepository accountRepository){
        this.accountRepository = accountRepository;
    }

    @Override
    public Account findByNumberAndUserId(){
        String accountNumber = extractUserAccountFromJwt().getNumber();
        String userId = extractUserAccountFromJwt().getUserId();

        var account = accountRepository.findByNumberAndUser(accountNumber, userId);

        return account
              .orElseThrow(() -> new ObjectNotFoundExeption("Account not found.", HttpStatus.FOUND));
    }


    private UserAccountDTO extractUserAccountFromJwt(){
        return new UserAccountDTO();
    }

}
