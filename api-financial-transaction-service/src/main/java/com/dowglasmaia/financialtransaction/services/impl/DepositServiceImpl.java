package com.dowglasmaia.financialtransaction.services.impl;

import com.dowglasmaia.financialtransaction.account.application.usecase.GetCurrentAgencyUseCase;
import com.dowglasmaia.financialtransaction.account.application.usecase.FindAccountByAgencyAndNumberUseCase;
import com.dowglasmaia.financialtransaction.account.application.usecase.CreditAccountUseCase;
import com.dowglasmaia.financialtransaction.account.domain.Account;
import com.dowglasmaia.financialtransaction.transaction.domain.TransactionType;
import com.dowglasmaia.financialtransaction.exeptions.ObjectNotFoundExeption;
import com.dowglasmaia.financialtransaction.services.DepositService;
import com.dowglasmaia.financialtransaction.services.TransactionService;
import com.dowglasmaia.provider.model.DepositRequestDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
public class DepositServiceImpl implements DepositService {

    private final GetCurrentAgencyUseCase getCurrentAgency;
    private final FindAccountByAgencyAndNumberUseCase findAccountByAgencyAndNumber;
    private final CreditAccountUseCase creditAccount;
    private final TransactionService transactionService;


    @Autowired
    public DepositServiceImpl(GetCurrentAgencyUseCase getCurrentAgency,
                              FindAccountByAgencyAndNumberUseCase findAccountByAgencyAndNumber,
                              CreditAccountUseCase creditAccount,
                              TransactionService transactionService
    ){
        this.getCurrentAgency = getCurrentAgency;
        this.findAccountByAgencyAndNumber = findAccountByAgencyAndNumber;
        this.creditAccount = creditAccount;
        this.transactionService = transactionService;
    }

    /** Credit + ledger row in ONE transaction: if the ledger cannot be written the credit is rolled back. */
    @Override
    @Transactional
    public void makeDeposit(DepositRequestDTO request){
        log.info("Start makeDeposit at agency and at acoount: {},{}", request.getAgency(), request.getAccount());

        String agencyId = getCurrentAgency.execute();
        log.info("Deposit requested at agency: {}", agencyId);

        Account accountEntity = findAccountByAgencyAndNumber.execute(agencyId, request.getAccount())
              .orElseThrow(() -> new ObjectNotFoundExeption("Account not found.", HttpStatus.FOUND));

        creditAccount.execute(accountEntity.getId(), request.getAmount());

        transactionService.save(TransactionType.DEPOSIT, accountEntity.getId(), request.getAmount(), agencyId);

        log.info("Deposit successfully");
    }

}
