package com.dowglasmaia.financialtransaction.services.impl;

import com.dowglasmaia.financialtransaction.entity.Account;
import com.dowglasmaia.financialtransaction.exeptions.ObjectNotFoundExeption;
import com.dowglasmaia.financialtransaction.exeptions.UnprocessableEntityExeption;
import com.dowglasmaia.financialtransaction.repository.AccountRepository;
import com.dowglasmaia.financialtransaction.services.DepositService;
import com.dowglasmaia.financialtransaction.services.TransactionService;
import com.dowglasmaia.financialtransaction.services.client.agency.AgencyService;
import com.dowglasmaia.provider.model.DepositRequestDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class DepositServiceImpl implements DepositService {

    private final AgencyService agencyService;
    private final AccountRepository accountRepository;
    private final TransactionService transactionService;


    @Autowired
    public DepositServiceImpl(AgencyService agencyService,
                              AccountRepository accountRepository,
                              TransactionService transactionService
    ){
        this.agencyService = agencyService;
        this.accountRepository = accountRepository;
        this.transactionService = transactionService;
    }

    @Override
    public void makeDeposit(DepositRequestDTO request){
        log.info("Start makeDeposit at agency and at acoount: {},{}", request.getAgency(), request.getAccount());

        String agencyId = agencyService.getCurrentAgency();
        log.info("Deposit requested at agency: {}", agencyId);

        Account accountEntity = accountRepository.findByAgencyAndNumber(agencyId, request.getAccount())
              .orElseThrow(() -> new ObjectNotFoundExeption("Account not found.", HttpStatus.FOUND));

        var newBalance = accountEntity.getBalance().add(request.getAmount());

        accountEntity.setBalance(newBalance);

        try {
            accountRepository.save(accountEntity);

            transactionService.save(request, accountEntity.getId(), request.getAmount(), agencyId);

            log.info("Deposit successfully");
        } catch (Exception e) {
            log.error("Fail deposit at account {}: {}", request.getAccount(), e.getMessage());
            throw new UnprocessableEntityExeption("Fail deposito at account " + request.getAccount(), HttpStatus.UNPROCESSABLE_ENTITY);
        }
    }

}
