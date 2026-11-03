package com.dowglasmaia.financialtransaction.account.infra.web;

import com.dowglasmaia.financialtransaction.account.application.AccountService;
import com.dowglasmaia.financialtransaction.account.domain.Account;
import com.dowglasmaia.provider.api.AccountsApi;
import com.dowglasmaia.provider.model.AccountResponseDTO;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * The signed-in customer's account. It takes no id: the account is resolved from the token subject by
 * {@link AccountService}, so no other customer's data can be requested (an unlinked subject is already 403 in the filter chain).
 */
@RestController
@RequestMapping(value = "/api/v1")
public class AccountController implements AccountsApi {

    private final AccountService accountService;

    public AccountController(AccountService accountService){
        this.accountService = accountService;
    }

    @Override
    public ResponseEntity<AccountResponseDTO> getMyAccount(){
        Account account = accountService.currentAccount();

        AccountResponseDTO body = new AccountResponseDTO()
              .agency(account.getAgency().getNumber())
              .number(account.getNumber())
              .balance(account.getBalance());

        return ResponseEntity.ok(body);
    }
}
