package com.dowglasmaia.exactbank.services.impl;

import com.dowglasmaia.exactbank.entity.Account;
import com.dowglasmaia.exactbank.exeptions.ObjectNotFoundExeption;
import com.dowglasmaia.exactbank.exeptions.UnprocessableEntityExeption;
import com.dowglasmaia.exactbank.integrations.MaiaBankClient;
import com.dowglasmaia.exactbank.repository.AccountRepository;
import com.dowglasmaia.exactbank.services.PixService;
import com.dowglasmaia.exactbank.utils.BigDecimalConvert;
import com.dowglasmaia.exactbank.utils.CPFValidator;
import com.dowglasmaia.exactbank.utils.EmailValidator;
import com.dowglasmaia.exactbank.utils.PhoneValidator;
import com.dowglasmaia.provider.model.PixRequestDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
@Slf4j
public class PixServiceImpl implements PixService {

    private final MaiaBankClient maiaBankClient;
    private final AccountRepository accountRepository;

    @Autowired
    public PixServiceImpl(MaiaBankClient maiaBankClient, AccountRepository accountRepository){
        this.maiaBankClient = maiaBankClient;
        this.accountRepository = accountRepository;
    }

    @Override
    public void makeTransfer(PixRequestDTO request){
        log.info("Start Send Pix");

        if (isValidKeyAndExists(request)) {
            String accountNumber = extractAccountNumberFromJwt();

            BigDecimal amountTransferred = BigDecimalConvert.toBigDecimal(request.getTransactionAmount());

            makeTransfer(accountNumber, amountTransferred);
            log.info("Pix sent successfully");
        } else {
            String errorMessage = "Pix key does not exist or is invalid for the specified type: " + request.getKeyType();
            log.error(errorMessage);

            throw new UnprocessableEntityExeption(errorMessage, HttpStatus.UNPROCESSABLE_ENTITY);
        }
    }

    private boolean isValidKeyAndExists(PixRequestDTO request){
        return validateKey(request) && existKey(request.getPixKey());
    }

    private String extractAccountNumberFromJwt(){
        // Lógica para extrair o número da conta do JWT
        return "2022"; // Simulação, substituir por lógica real
    }

    public void makeTransfer(String accountNumber, BigDecimal transactionAmount){
        Account accountEntity = accountRepository.findByNumber(accountNumber)
              .orElseThrow(() -> new ObjectNotFoundExeption("Account not found.", HttpStatus.FOUND));

        if (accountEntity.getBalance().compareTo(transactionAmount) > 0) {
            var originalBalance = accountEntity.getBalance();
            var newBalance = originalBalance.subtract(transactionAmount);

            accountEntity.setBalance(newBalance);
            accountRepository.save(accountEntity);

            try {
                maiaBankClient.receiversPix();
            } catch (Exception e) {
                log.error("Fail send Pix. {}", e);

                log.info("Restore the original balance in case of failure. {}", e);
                accountEntity.setBalance(originalBalance);
                accountRepository.save(accountEntity);

                throw new UnprocessableEntityExeption("Fail send Pix.", HttpStatus.UNPROCESSABLE_ENTITY);
            }
        } else {
            log.error("Operation not performed, there is not enough balance in the account.");
            throw new UnprocessableEntityExeption("Operation not performed, there is not enough balance in the account.", HttpStatus.UNPROCESSABLE_ENTITY);
        }
    }

    private boolean existKey(String key){
        log.info("validate if the pixKey exists. {}", key);
        return true;
    }

    private boolean validateKey(PixRequestDTO request){
        String pixKey = request.getPixKey();
        switch (request.getKeyType()) {
            case CPF:
                return CPFValidator.isCpf(pixKey);
            case EMAIL:
                return EmailValidator.isValidEmail(pixKey);
            case PHONE:
                return PhoneValidator.isValidPhoneNumber(pixKey);
            default:
                return false;
        }
    }

}
