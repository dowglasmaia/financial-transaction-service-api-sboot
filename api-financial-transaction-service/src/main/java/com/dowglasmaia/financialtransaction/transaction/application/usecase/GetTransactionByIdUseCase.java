package com.dowglasmaia.financialtransaction.transaction.application.usecase;

import com.dowglasmaia.financialtransaction.account.application.usecase.GetCurrentAccountUseCase;
import com.dowglasmaia.financialtransaction.transaction.application.port.TransactionRepository;
import com.dowglasmaia.financialtransaction.account.domain.Account;
import com.dowglasmaia.financialtransaction.transaction.domain.Transaction;
import com.dowglasmaia.financialtransaction.shared.error.ObjectNotFoundExeption;
import org.springframework.stereotype.Service;

/** One entry of the caller's account; an id that belongs to another account behaves as an unknown one. */
@Service
public class GetTransactionByIdUseCase {

    private final TransactionRepository ledger;
    private final GetCurrentAccountUseCase getCurrentAccount;

    public GetTransactionByIdUseCase(TransactionRepository ledger, GetCurrentAccountUseCase getCurrentAccount) {
        this.ledger = ledger;
        this.getCurrentAccount = getCurrentAccount;
    }

    public Transaction execute(String id) {
        Account account = getCurrentAccount.execute();

        return ledger.findByIdAndAccountId(id, account.getId())
              .orElseThrow(() -> new ObjectNotFoundExeption("Transaction not found"));
    }
}
