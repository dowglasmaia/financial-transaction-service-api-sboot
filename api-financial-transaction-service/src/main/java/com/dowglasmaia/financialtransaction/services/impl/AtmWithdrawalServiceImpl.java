package com.dowglasmaia.financialtransaction.services.impl;

import com.dowglasmaia.financialtransaction.entity.TransactionTypeEnum;
import com.dowglasmaia.financialtransaction.services.AtmWithdrawalService;
import com.dowglasmaia.financialtransaction.services.TransactionService;
import com.dowglasmaia.financialtransaction.services.client.account.AddAmountService;
import com.dowglasmaia.financialtransaction.services.client.account.FindAccountByNumberAndUserIdService;
import com.dowglasmaia.financialtransaction.services.client.agency.AgencyService;
import com.dowglasmaia.provider.model.WithdrawRequestDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
public class AtmWithdrawalServiceImpl implements AtmWithdrawalService {

    private final AgencyService agencyService;
    private final AddAmountService addAmountService;
    private final TransactionService transactionService;
    private final FindAccountByNumberAndUserIdService findAccountByNumberAndUserIdService;

    @Autowired
    public AtmWithdrawalServiceImpl(AgencyService agencyService,
                                    AddAmountService addAmountService,
                                    TransactionService transactionService,
                                    FindAccountByNumberAndUserIdService findAccountByNumberAndUserIdService
    ){
        this.agencyService = agencyService;
        this.addAmountService = addAmountService;
        this.transactionService = transactionService;
        this.findAccountByNumberAndUserIdService = findAccountByNumberAndUserIdService;
    }

    /** Debit + ledger row in ONE transaction. (Which account may be debited is decided later, spec BE-04.) */
    @Override
    @Transactional
    public void makeAtmWithdrawal(WithdrawRequestDTO withdrawRequest){
        log.info("Start makeAtmWithdrawal");

        String agencyId = agencyService.getCurrentAgency();
        log.info("AtmWithdrawal at agency: {}", agencyId);

        var account = findAccountByNumberAndUserIdService.findByNumberAndUserId();

        if (isCardBasedWithdrawal(withdrawRequest)) {
            log.info("AtmWithdrawal base card");

            addAmountService.debitById(account.getId(), withdrawRequest.getAmount());
        } else {
            log.info("AtmWithdrawal not based card");

            addAmountService.debit(withdrawRequest.getNumberAccount(), withdrawRequest.getAmount());
        }

        transactionService.save(TransactionTypeEnum.WITHDRAW, account.getId(), withdrawRequest.getAmount(), agencyId);
    }

    private boolean isCardBasedWithdrawal(WithdrawRequestDTO withdrawRequest){
        return withdrawRequest.getAgency() == null && withdrawRequest.getNumberAccount() == null;
    }
}
