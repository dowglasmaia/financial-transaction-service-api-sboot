package com.dowglasmaia.financialtransaction.services.client.account.impl;

import com.dowglasmaia.financialtransaction.entity.Account;
import com.dowglasmaia.financialtransaction.services.client.CurrentUser;
import com.dowglasmaia.financialtransaction.services.client.account.FindAccountByNumberAndUserIdService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/** The caller's account, resolved from the authenticated token (spec BE-04); the class name is kept (decision D-19). */
@Service
public class FindFindAccountByNumberAndUserIdByNumberAndUserIdServiceImpl implements FindAccountByNumberAndUserIdService {

    private final CurrentUser currentUser;

    @Autowired
    public FindFindAccountByNumberAndUserIdByNumberAndUserIdServiceImpl(CurrentUser currentUser){
        this.currentUser = currentUser;
    }

    @Override
    public Account findByNumberAndUserId(){
        return currentUser.account();
    }

}
