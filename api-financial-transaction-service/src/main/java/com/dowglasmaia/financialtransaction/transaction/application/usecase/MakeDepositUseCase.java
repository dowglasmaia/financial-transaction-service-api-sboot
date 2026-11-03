package com.dowglasmaia.financialtransaction.transaction.application.usecase;

import com.dowglasmaia.financialtransaction.account.application.usecase.CreditAccountUseCase;
import com.dowglasmaia.financialtransaction.account.application.usecase.FindAccountByAgencyAndNumberUseCase;
import com.dowglasmaia.financialtransaction.account.application.usecase.GetCurrentAgencyUseCase;
import com.dowglasmaia.financialtransaction.account.domain.Account;
import com.dowglasmaia.financialtransaction.transaction.domain.TransactionType;
import com.dowglasmaia.financialtransaction.shared.error.ObjectNotFoundExeption;
import java.math.BigDecimal;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
public class MakeDepositUseCase {

    private final GetCurrentAgencyUseCase getCurrentAgency;
    private final FindAccountByAgencyAndNumberUseCase findAccountByAgencyAndNumber;
    private final CreditAccountUseCase creditAccount;
    private final SaveTransactionUseCase saveTransaction;

    public MakeDepositUseCase(GetCurrentAgencyUseCase getCurrentAgency,
                              FindAccountByAgencyAndNumberUseCase findAccountByAgencyAndNumber,
                              CreditAccountUseCase creditAccount,
                              SaveTransactionUseCase saveTransaction) {
        this.getCurrentAgency = getCurrentAgency;
        this.findAccountByAgencyAndNumber = findAccountByAgencyAndNumber;
        this.creditAccount = creditAccount;
        this.saveTransaction = saveTransaction;
    }

    /** Credit + ledger row in ONE transaction: if the ledger cannot be written the credit is rolled back. */
    @Transactional
    public void execute(String agency, String account, BigDecimal amount) {
        log.info("Start makeDeposit at agency and at acoount: {},{}", agency, account);

        String agencyId = getCurrentAgency.execute();
        log.info("Deposit requested at agency: {}", agencyId);

        Account found = findAccountByAgencyAndNumber.execute(agencyId, account)
              .orElseThrow(() -> new ObjectNotFoundExeption("Account not found."));

        creditAccount.execute(found.getId(), amount);

        saveTransaction.execute(TransactionType.DEPOSIT, found.getId(), amount, agencyId);

        log.info("Deposit successfully");
    }
}
