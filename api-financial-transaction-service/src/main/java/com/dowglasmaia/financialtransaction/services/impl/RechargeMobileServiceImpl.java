package com.dowglasmaia.financialtransaction.services.impl;

import com.dowglasmaia.financialtransaction.entity.Account;
import com.dowglasmaia.financialtransaction.exeptions.UnprocessableEntityExeption;
import com.dowglasmaia.financialtransaction.repository.AccountRepository;
import com.dowglasmaia.financialtransaction.services.RechargeMobileService;
import com.dowglasmaia.financialtransaction.services.TransactionService;
import com.dowglasmaia.financialtransaction.services.client.account.FindAccountByNumberAndUserIdService;
import com.dowglasmaia.financialtransaction.utils.PhoneValidator;
import com.dowglasmaia.provider.model.MobileRechargeRequestDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Slf4j
@Service
public class RechargeMobileServiceImpl implements RechargeMobileService {

    private final KafkaTemplate<String, MobileRechargeRequestDTO> mobileRechargeRequesKafkaTemplate;
    private final FindAccountByNumberAndUserIdService findAccountByNumberAndUserIdService;
    private final TransactionService transactionService;
    private final AccountRepository accountRepository;

    @Autowired
    public RechargeMobileServiceImpl(KafkaTemplate<String, MobileRechargeRequestDTO> mobileRechargeRequesKafkaTemplate,
                                     FindAccountByNumberAndUserIdService findAccountByNumberAndUserIdService,
                                     TransactionService transactionService,
                                     AccountRepository accountRepository
    ){
        this.mobileRechargeRequesKafkaTemplate = mobileRechargeRequesKafkaTemplate;
        this.findAccountByNumberAndUserIdService = findAccountByNumberAndUserIdService;
        this.transactionService = transactionService;
        this.accountRepository = accountRepository;
    }

    @Override
    public void makeRecharge(MobileRechargeRequestDTO request){
        validatePhoneNumber(request);

        var account = validateAndUpdateBalanceAccount(request.getAmount());

        mobileRechargeRequesKafkaTemplate.send("recharge-mobile-topic", request);

        transactionService.save(request, account.getNumber(), request.getAmount(), null);
    }

    private Account validateAndUpdateBalanceAccount(BigDecimal amoutReacharge){
        Account accountEntity = findAccountByNumberAndUserIdService.findByNumberAndUserId();

        if (accountEntity.getBalance().compareTo(amoutReacharge) > 0) {
            var newBalance = accountEntity.getBalance().subtract(amoutReacharge);
            accountEntity.setBalance(newBalance);
            return accountRepository.save(accountEntity);

        } else {
            log.error("Operation not performed, there is not enough balance in the account.");
            throw new UnprocessableEntityExeption("Operation not performed, there is not enough balance in the account.", HttpStatus.UNPROCESSABLE_ENTITY);
        }
    }


    private void validatePhoneNumber(MobileRechargeRequestDTO request){
        String phoneNumber = String.valueOf(request.getAreaCode()) + request.getPhoneNumber();

        if (!PhoneValidator.isValidPhoneNumber(phoneNumber)) {
            throw new UnprocessableEntityExeption("Invalid cell phone numer", HttpStatus.UNPROCESSABLE_ENTITY);
        }
    }


}
