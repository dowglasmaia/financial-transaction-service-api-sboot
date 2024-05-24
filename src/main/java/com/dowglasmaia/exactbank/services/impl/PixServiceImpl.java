package com.dowglasmaia.exactbank.services.impl;

import com.dowglasmaia.exactbank.entity.Account;
import com.dowglasmaia.exactbank.integrations.MaiaBankClient;
import com.dowglasmaia.exactbank.repository.AccountRepository;
import com.dowglasmaia.exactbank.services.PixService;
import com.dowglasmaia.provider.model.PixRequestDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;

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
    public void sendPix(PixRequestDTO request){
        if (existKey(request.getPixKey())) {
            String accountNumber = "2022"; // extrair do JWT

            var amountTransferred = BigDecimal
                  .valueOf(request.getTransactionAmount())
                  .setScale(2, RoundingMode.HALF_UP);

            send( accountNumber,amountTransferred );

            log.info("Pix sended with successfully");
        } else {
            log.error("Pix key does not exist: KEY. {}", request.getPixKey());
            throw new RuntimeException("Pix key does not exist");
        }
    }

    public void send(String accountNumber, BigDecimal transactionAmount){
        log.info("Start Send Pix");
        Account accountEntity = accountRepository.findByNumber(accountNumber)
              .orElseThrow(() -> new RuntimeException("Account not found."));

        if (accountEntity.getBalance().compareTo(transactionAmount) > 0) {
            var originalBalance = accountEntity.getBalance();
            var newBalance = originalBalance.subtract(transactionAmount);

            accountEntity.setBalance(newBalance);
            accountRepository.save(accountEntity);

            try {
                maiaBankClient.receiversPix();
                log.info("End Send Pix");
            } catch (Exception e) {
                log.error("Fail send Pix. {}", e);

                log.info("Restore the original balance in case of failure. {}", e);
                accountEntity.setBalance(originalBalance);
                accountRepository.save(accountEntity);

                throw new RuntimeException("Fail send Pix.", e);
            }
        } else {
            log.error("Operation not performed, there is not enough balance in the account.");
            throw new RuntimeException("Operation not performed, there is not enough balance in the account.");
        }
    }

    private boolean existKey(String key){
        log.info("validate if the pixKey exists. {}", key);
        return true;
    }


}
