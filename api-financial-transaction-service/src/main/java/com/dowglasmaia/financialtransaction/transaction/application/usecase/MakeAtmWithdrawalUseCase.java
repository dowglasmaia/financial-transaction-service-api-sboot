package com.dowglasmaia.financialtransaction.transaction.application.usecase;

import com.dowglasmaia.financialtransaction.account.application.usecase.DebitAccountUseCase;
import com.dowglasmaia.financialtransaction.account.application.usecase.GetCurrentAccountUseCase;
import com.dowglasmaia.financialtransaction.account.application.usecase.GetCurrentAgencyUseCase;
import com.dowglasmaia.financialtransaction.transaction.domain.TransactionType;
import com.dowglasmaia.financialtransaction.shared.error.ObjectNotFoundException;
import java.math.BigDecimal;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
public class MakeAtmWithdrawalUseCase {

    private final GetCurrentAgencyUseCase getCurrentAgency;
    private final GetCurrentAccountUseCase getCurrentAccount;
    private final DebitAccountUseCase debitAccount;
    private final SaveTransactionUseCase saveTransaction;

    public MakeAtmWithdrawalUseCase(GetCurrentAgencyUseCase getCurrentAgency,
                                    GetCurrentAccountUseCase getCurrentAccount,
                                    DebitAccountUseCase debitAccount,
                                    SaveTransactionUseCase saveTransaction) {
        this.getCurrentAgency = getCurrentAgency;
        this.getCurrentAccount = getCurrentAccount;
        this.debitAccount = debitAccount;
        this.saveTransaction = saveTransaction;
    }

    /**
     * Debit + ledger row in ONE transaction. Only the caller's own account can be debited: a card withdrawal uses it
     * directly, a withdrawal that names an account must name the caller's (a foreign number is "not found", never debited).
     * The ledger row follows the debited account.
     */
    @Transactional
    public void execute(String agency, String numberAccount, BigDecimal amount) {
        log.info("Start makeAtmWithdrawal");

        String agencyId = getCurrentAgency.execute();

        var account = getCurrentAccount.execute();

        if (agency == null && numberAccount == null) {
            log.info("AtmWithdrawal base card");

            debitAccount.execute(account.getId(), amount);
        } else {
            log.info("AtmWithdrawal not based card");

            if (!Objects.equals(numberAccount, account.getNumber())) {
                throw new ObjectNotFoundException("Account not found.");
            }
            debitAccount.execute(account.getId(), amount);
        }

        saveTransaction.execute(TransactionType.WITHDRAW, account.getId(), amount, agencyId);
    }
}
