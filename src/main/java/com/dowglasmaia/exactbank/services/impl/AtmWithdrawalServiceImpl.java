package com.dowglasmaia.exactbank.services.impl;

import com.dowglasmaia.exactbank.services.AtmWithdrawalService;
import com.dowglasmaia.exactbank.services.TransactionService;
import com.dowglasmaia.exactbank.services.client.account.AddAmountService;
import com.dowglasmaia.exactbank.services.client.agency.AgencyService;
import com.dowglasmaia.provider.model.WithdrawRequestDTO;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class AtmWithdrawalServiceImpl implements AtmWithdrawalService {

    private final AgencyService agencyService;
    private final AddAmountService addAmountService;
    private final TransactionService transactionService;

    @Autowired
    public AtmWithdrawalServiceImpl(AgencyService agencyService,
                                    AddAmountService addAmountService,
                                    TransactionService transactionService
    ){
        this.agencyService = agencyService;
        this.addAmountService = addAmountService;
        this.transactionService = transactionService;
    }

    @Override
    public void makeAtmWithdrawal(WithdrawRequestDTO withdrawRequest){
        log.info("Start makeAtmWithdrawal");

        String agencyId = agencyService.getCurrentAgency();
        log.info("Deposit requested at agency: {}", agencyId);

        var account = extractUserAccountFromJwt();

        if (isCardBasedWithdrawal(withdrawRequest)) {
            log.info("AtmWithdrawal base card");

            addAmountService.addAmountToAccount(account.getNumber(), withdrawRequest.getAmount());
        } else {
            log.info("AtmWithdrawal not based card");

            addAmountService.addAmountToAccount(withdrawRequest.getNumberAccount(), withdrawRequest.getAmount());
        }

        transactionService.save(withdrawRequest, account.getId(), withdrawRequest.getAmount(), agencyId);
    }

    private boolean isCardBasedWithdrawal(WithdrawRequestDTO withdrawRequest){
        return withdrawRequest.getAgency() == null && withdrawRequest.getNumberAccount() == null;
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
