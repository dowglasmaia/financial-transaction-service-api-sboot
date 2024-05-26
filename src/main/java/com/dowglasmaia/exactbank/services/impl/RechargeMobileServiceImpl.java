package com.dowglasmaia.exactbank.services.impl;

import com.dowglasmaia.exactbank.entity.Account;
import com.dowglasmaia.exactbank.exeptions.ObjectNotFoundExeption;
import com.dowglasmaia.exactbank.exeptions.UnprocessableEntityExeption;
import com.dowglasmaia.exactbank.repository.AccountRepository;
import com.dowglasmaia.exactbank.services.RechargeMobileService;
import com.dowglasmaia.exactbank.services.TransactionService;
import com.dowglasmaia.exactbank.utils.PhoneValidator;
import com.dowglasmaia.provider.model.MobileRechargeRequestDTO;
import lombok.Getter;
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
    private final AccountRepository accountRepository;
    private final TransactionService transactionService;

    @Autowired
    public RechargeMobileServiceImpl(KafkaTemplate<String, MobileRechargeRequestDTO> mobileRechargeRequesKafkaTemplate,
                                     AccountRepository accountRepository,
                                     TransactionService transactionService
    ){
        this.mobileRechargeRequesKafkaTemplate = mobileRechargeRequesKafkaTemplate;
        this.accountRepository = accountRepository;
        this.transactionService = transactionService;
    }

    @Override
    public void makeRecharge(MobileRechargeRequestDTO request){
        validatePhoneNumber(request);

        var account = validateAndUpdateBalanceAccount(request.getAmount());

        mobileRechargeRequesKafkaTemplate.send("recharge-mobile-topic", request);

        transactionService.save(request, account.getNumber(), request.getAmount(), null);
    }

    private Account validateAndUpdateBalanceAccount(BigDecimal amoutReacharge){
        Account accountEntity = accountRepository.findByNumber(extractUserAccountFromJwt().getNumber())
              .orElseThrow(() -> new ObjectNotFoundExeption("Account not found.", HttpStatus.FOUND));

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

    // Simulated method to extract user account details from JWT
    private UserAccountDTO extractUserAccountFromJwt(){
        return new UserAccountDTO();
    }


    @Getter
    class UserAccountDTO {
        private String id = "750acb6e-79f8-45e2-b3f7-eb729142041d";
        private String userId = "user_id";
        private String agencyNumber = "1122";
        private String number = "2022";
    }

}
