package com.dowglasmaia.financialtransaction.controllers;

import com.dowglasmaia.financialtransaction.transaction.application.usecase.GetStatementUseCase;
import com.dowglasmaia.financialtransaction.transaction.application.usecase.GetTransactionByIdUseCase;
import com.dowglasmaia.financialtransaction.transaction.application.usecase.MakeAtmWithdrawalUseCase;
import com.dowglasmaia.financialtransaction.transaction.application.usecase.MakeDepositUseCase;
import com.dowglasmaia.financialtransaction.transaction.application.usecase.MakePixTransferUseCase;
import com.dowglasmaia.financialtransaction.transaction.application.usecase.RequestMobileRechargeUseCase;
import com.dowglasmaia.financialtransaction.transaction.domain.MobileRecharge;
import com.dowglasmaia.financialtransaction.transaction.domain.PixKey;
import com.dowglasmaia.financialtransaction.transaction.domain.PixKeyType;
import com.dowglasmaia.financialtransaction.transaction.domain.PixTransfer;
import com.dowglasmaia.financialtransaction.transaction.domain.Statement;
import com.dowglasmaia.provider.api.TransactionsApi;
import com.dowglasmaia.provider.model.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

import static com.dowglasmaia.financialtransaction.controllers.validate.ValidateBodyRequest.*;
import static com.dowglasmaia.financialtransaction.services.impl.mapper.TransactionMapper.toTransactionRequest;
import static com.dowglasmaia.financialtransaction.services.impl.mapper.TransactionMapper.toTransactionsResponse;

/** Inbound adapter: validates the shape, maps the generated DTOs to domain values and calls one use case. */
@RestController
@RequestMapping(value = "/api/v1")
public class TransactionController implements TransactionsApi {

    private final MakePixTransferUseCase makePixTransfer;
    private final MakeDepositUseCase makeDeposit;
    private final RequestMobileRechargeUseCase requestMobileRecharge;
    private final MakeAtmWithdrawalUseCase makeAtmWithdrawal;
    private final GetStatementUseCase getStatement;
    private final GetTransactionByIdUseCase getTransactionById;

    public TransactionController(MakePixTransferUseCase makePixTransfer,
                                 MakeDepositUseCase makeDeposit,
                                 RequestMobileRechargeUseCase requestMobileRecharge,
                                 MakeAtmWithdrawalUseCase makeAtmWithdrawal,
                                 GetStatementUseCase getStatement,
                                 GetTransactionByIdUseCase getTransactionById
    ){
        this.makePixTransfer = makePixTransfer;
        this.makeDeposit = makeDeposit;
        this.requestMobileRecharge = requestMobileRecharge;
        this.makeAtmWithdrawal = makeAtmWithdrawal;
        this.getStatement = getStatement;
        this.getTransactionById = getTransactionById;
    }

    @Override
    public ResponseEntity<Void> sendPix(PixRequestDTO body){
        validatorPixRequest(body);
        makePixTransfer.execute(new PixTransfer(new PixKey(PixKeyType.valueOf(body.getKeyType().name()), body.getPixKey()), body.getTransactionAmount()));
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

    @Override
    public ResponseEntity<Void> deposit(DepositRequestDTO body){
        validatorDepositRequest(body);
        makeDeposit.execute(body.getAgency(), body.getAccount(), body.getAmount());
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

    @Override
    public ResponseEntity<Void> rechargeMobile(MobileRechargeRequestDTO body){
        validateMobileRechargeRequest(body);
        requestMobileRecharge.execute(new MobileRecharge(body.getAreaCode(), body.getPhoneNumber(), body.getAmount(), body.getCarrier().toString()));
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

    @Override
    public ResponseEntity<Void> withdraw(WithdrawRequestDTO body){
        validateWithdrawRequest(body);
        makeAtmWithdrawal.execute(body.getAgency(), body.getNumberAccount(), body.getAmount());
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

    @Override
    public ResponseEntity<TransactionsResponseDTO> getTransactions(String initialDate, String finalDate){
        Statement statement = getStatement.execute(initialDate, finalDate);
        var transactions = toTransactionsResponse(statement.transactions());
        transactions.setBalance(statement.balance());
        return ResponseEntity.status(HttpStatus.OK).body(transactions);
    }

    @Override
    public ResponseEntity<TransactionResponseDTO> getTransaction(UUID id){
        var transaction = toTransactionRequest(getTransactionById.execute(String.valueOf(id)));
        return ResponseEntity.status(HttpStatus.OK).body(transaction);
    }
}
