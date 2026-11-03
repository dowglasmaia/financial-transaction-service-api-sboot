package com.dowglasmaia.financialtransaction.services.impl;

import com.dowglasmaia.financialtransaction.account.application.usecase.GetCurrentAgencyUseCase;
import com.dowglasmaia.financialtransaction.account.application.usecase.GetCurrentAccountUseCase;
import com.dowglasmaia.financialtransaction.account.application.usecase.DebitAccountUseCase;
import com.dowglasmaia.financialtransaction.transaction.domain.TransactionType;
import com.dowglasmaia.financialtransaction.exeptions.ObjectNotFoundExeption;
import com.dowglasmaia.financialtransaction.services.AtmWithdrawalService;
import com.dowglasmaia.financialtransaction.services.TransactionService;
import com.dowglasmaia.provider.model.WithdrawRequestDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;

@Slf4j
@Service
public class AtmWithdrawalServiceImpl implements AtmWithdrawalService {

    private final GetCurrentAgencyUseCase getCurrentAgency;
    private final GetCurrentAccountUseCase getCurrentAccount;
    private final DebitAccountUseCase debitAccount;
    private final TransactionService transactionService;

    @Autowired
    public AtmWithdrawalServiceImpl(GetCurrentAgencyUseCase getCurrentAgency,
                                    GetCurrentAccountUseCase getCurrentAccount,
                                    DebitAccountUseCase debitAccount,
                                    TransactionService transactionService
    ){
        this.getCurrentAgency = getCurrentAgency;
        this.getCurrentAccount = getCurrentAccount;
        this.debitAccount = debitAccount;
        this.transactionService = transactionService;
    }

    /**
     * Debit + ledger row in ONE transaction. Only the caller's own account can be debited: a card withdrawal uses it
     * directly, a withdrawal that names an account must name the caller's (a foreign number is "not found", never debited).
     * The ledger row follows the debited account.
     */
    @Override
    @Transactional
    public void makeAtmWithdrawal(WithdrawRequestDTO withdrawRequest){
        log.info("Start makeAtmWithdrawal");

        String agencyId = getCurrentAgency.execute();
        log.info("AtmWithdrawal at agency: {}", agencyId);

        var account = getCurrentAccount.execute();

        if (isCardBasedWithdrawal(withdrawRequest)) {
            log.info("AtmWithdrawal base card");

            debitAccount.execute(account.getId(), withdrawRequest.getAmount());
        } else {
            log.info("AtmWithdrawal not based card");

            if (!Objects.equals(withdrawRequest.getNumberAccount(), account.getNumber())) {
                throw new ObjectNotFoundExeption("Account not found.", HttpStatus.NOT_FOUND);
            }
            debitAccount.execute(account.getId(), withdrawRequest.getAmount());
        }

        transactionService.save(TransactionType.WITHDRAW, account.getId(), withdrawRequest.getAmount(), agencyId);
    }

    private boolean isCardBasedWithdrawal(WithdrawRequestDTO withdrawRequest){
        return withdrawRequest.getAgency() == null && withdrawRequest.getNumberAccount() == null;
    }
}
