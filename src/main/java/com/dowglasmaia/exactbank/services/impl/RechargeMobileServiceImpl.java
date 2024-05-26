package com.dowglasmaia.exactbank.services.impl;

import com.dowglasmaia.exactbank.entity.Account;
import com.dowglasmaia.exactbank.exeptions.ObjectNotFoundExeption;
import com.dowglasmaia.exactbank.exeptions.UnprocessableEntityExeption;
import com.dowglasmaia.exactbank.repository.AccountRepository;
import com.dowglasmaia.exactbank.services.RechargeMobileService;
import com.dowglasmaia.exactbank.utils.BigDecimalConvert;
import com.dowglasmaia.exactbank.utils.PhoneValidator;
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
    private final AccountRepository accountRepository;

    @Autowired
    public RechargeMobileServiceImpl(KafkaTemplate<String, MobileRechargeRequestDTO> mobileRechargeRequesKafkaTemplate,
                                     AccountRepository accountRepository
    ){
        this.mobileRechargeRequesKafkaTemplate = mobileRechargeRequesKafkaTemplate;
        this.accountRepository = accountRepository;
    }

    @Override
    public void makeRecharge(MobileRechargeRequestDTO request){
        validatePhoneNumber(request);

        var amoutReacharge = BigDecimalConvert.toBigDecimal(request.getAmount());

        validateAndUpdateBalanceAccount(amoutReacharge);

        mobileRechargeRequesKafkaTemplate.send("recharge-mobile-topic", request);
    }

    private void validateAndUpdateBalanceAccount(BigDecimal amoutReacharge){
        Account accountEntity = accountRepository.findByNumber(extractAccountNumberFromJwt())
              .orElseThrow(() -> new ObjectNotFoundExeption("Account not found.", HttpStatus.FOUND));

        if (accountEntity.getBalance().compareTo(amoutReacharge) > 0) {
            var newBalance = accountEntity.getBalance().subtract(amoutReacharge);
            accountEntity.setBalance(newBalance);
            accountRepository.save(accountEntity);
        } else {
            log.error("Operation not performed, there is not enough balance in the account.");
            throw new UnprocessableEntityExeption("Operation not performed, there is not enough balance in the account.", HttpStatus.UNPROCESSABLE_ENTITY);
        }
    }

    private String extractAccountNumberFromJwt(){
        // Lógica para extrair o número da conta do JWT
        return "2022";
    }

    private void validatePhoneNumber(MobileRechargeRequestDTO request){
        String phoneNumber = String.valueOf(request.getAreaCode()) + request.getPhoneNumber();

        if (!PhoneValidator.isValidPhoneNumber(phoneNumber)) {
            throw new UnprocessableEntityExeption("Invalid cell phone numer", HttpStatus.UNPROCESSABLE_ENTITY);
        }
    }

}
