package com.dowglasmaia.financialtransaction.controllers;

import com.dowglasmaia.financialtransaction.account.infra.persistence.AccountEntity;
import com.dowglasmaia.financialtransaction.services.client.CurrentUser;
import com.dowglasmaia.provider.api.AccountsApi;
import com.dowglasmaia.provider.model.AccountResponseDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * The signed-in customer's account. It takes no id: the account is resolved from the token subject by
 * {@link CurrentUser}, so no other customer's data can be requested (an unlinked subject is already 403 in the filter chain).
 */
@RestController
@RequestMapping(value = "/api/v1")
public class AccountController implements AccountsApi {

    private final CurrentUser currentUser;

    @Autowired
    public AccountController(CurrentUser currentUser){
        this.currentUser = currentUser;
    }

    @Override
    public ResponseEntity<AccountResponseDTO> getMyAccount(){
        AccountEntity account = currentUser.account();

        AccountResponseDTO body = new AccountResponseDTO()
              .agency(account.getAgency().getNumber())
              .number(account.getNumber())
              .balance(account.getBalance());

        return ResponseEntity.ok(body);
    }
}
