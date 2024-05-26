package com.dowglasmaia.exactbank.services.impl;

import com.dowglasmaia.exactbank.services.AtmWithdrawalService;
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

    @Autowired
    public AtmWithdrawalServiceImpl(AgencyService agencyService,
                                    AddAmountService addAmountService
    ){
        this.agencyService = agencyService;
        this.addAmountService = addAmountService;
    }

    @Override
    public void makeAtmWithdrawal(WithdrawRequestDTO withdrawRequest){
        log.info("Start makeAtmWithdrawal");

        String currentAgency = agencyService.getCurrentAgency();
        log.info("Deposit requested at agency: {}", currentAgency);

        if (isCardBasedWithdrawal(withdrawRequest)) {
            log.info("AtmWithdrawal base card");
            var account = extractUserAccountFromJwt();
            addAmountService.addAmountToAccount(account.getNumber(), withdrawRequest.getAmount());
        } else {
            log.info("AtmWithdrawal not based card");
            addAmountService.addAmountToAccount(withdrawRequest.getNumberAccount(), withdrawRequest.getAmount());
        }

    }

    // Simulated method to extract user account details from JWT
    private UserAccountDTO extractUserAccountFromJwt(){
        return new UserAccountDTO(); // Simulação, substituir por lógica real
    }

    private boolean isCardBasedWithdrawal(WithdrawRequestDTO withdrawRequest){
        return withdrawRequest.getAgency() == null && withdrawRequest.getNumberAccount() == null;
    }

    @Getter
    class UserAccountDTO {
        private String userId = "user_id";
        private String agencyNumber = "1122";
        private String number = "2022";
    }
}
