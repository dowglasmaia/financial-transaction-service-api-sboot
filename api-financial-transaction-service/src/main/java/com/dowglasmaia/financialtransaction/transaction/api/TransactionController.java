package com.dowglasmaia.financialtransaction.transaction.api;

import com.dowglasmaia.financialtransaction.transaction.application.usecase.GetStatementUseCase;
import com.dowglasmaia.financialtransaction.shared.error.UnprocessableEntityException;
import com.dowglasmaia.financialtransaction.transaction.application.usecase.GetTransactionByIdUseCase;
import com.dowglasmaia.financialtransaction.transaction.application.usecase.GetTransactionsSummaryUseCase;
import com.dowglasmaia.financialtransaction.transaction.application.usecase.MakeAtmWithdrawalUseCase;
import com.dowglasmaia.financialtransaction.transaction.application.usecase.MakeDepositUseCase;
import com.dowglasmaia.financialtransaction.transaction.application.usecase.MakePixTransferUseCase;
import com.dowglasmaia.financialtransaction.transaction.application.usecase.RequestMobileRechargeUseCase;
import com.dowglasmaia.financialtransaction.transaction.domain.MobileRecharge;
import com.dowglasmaia.financialtransaction.transaction.domain.PixKey;
import com.dowglasmaia.financialtransaction.transaction.domain.PixKeyType;
import com.dowglasmaia.financialtransaction.transaction.domain.PixTransfer;
import com.dowglasmaia.financialtransaction.transaction.domain.Statement;
import com.dowglasmaia.financialtransaction.transaction.domain.StatementPeriod;
import com.dowglasmaia.financialtransaction.transaction.domain.StatementQuery;
import com.dowglasmaia.financialtransaction.transaction.domain.TransactionsSummary;
import com.dowglasmaia.provider.api.TransactionsApi;
import com.dowglasmaia.provider.model.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.util.UUID;

import static com.dowglasmaia.financialtransaction.transaction.api.ValidateBodyRequest.*;
import static com.dowglasmaia.financialtransaction.transaction.api.TransactionMapper.toSummaryResponse;
import static com.dowglasmaia.financialtransaction.transaction.api.TransactionMapper.toTransactionRequest;
import static com.dowglasmaia.financialtransaction.transaction.api.TransactionMapper.toTransactionsResponse;

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
    private final GetTransactionsSummaryUseCase getTransactionsSummary;

    public TransactionController(MakePixTransferUseCase makePixTransfer,
                                 MakeDepositUseCase makeDeposit,
                                 RequestMobileRechargeUseCase requestMobileRecharge,
                                 MakeAtmWithdrawalUseCase makeAtmWithdrawal,
                                 GetStatementUseCase getStatement,
                                 GetTransactionByIdUseCase getTransactionById,
                                 GetTransactionsSummaryUseCase getTransactionsSummary
    ){
        this.makePixTransfer = makePixTransfer;
        this.makeDeposit = makeDeposit;
        this.requestMobileRecharge = requestMobileRecharge;
        this.makeAtmWithdrawal = makeAtmWithdrawal;
        this.getStatement = getStatement;
        this.getTransactionById = getTransactionById;
        this.getTransactionsSummary = getTransactionsSummary;
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
    public ResponseEntity<TransactionsResponseDTO> getTransactions(LocalDate from, LocalDate to, String initialDate, String finalDate,
                                                                   String type, String sort, String order, Integer page, Integer size){
        StatementPeriod period = periodOf(from, to, initialDate, finalDate);
        Statement statement = getStatement.execute(StatementQuery.of(period, type, sort, order, page, size));
        return ResponseEntity.status(HttpStatus.OK).body(toTransactionsResponse(statement));
    }

    @Override
    public ResponseEntity<TransactionsSummaryResponseDTO> getTransactionsSummary(LocalDate from, LocalDate to){
        TransactionsSummary summary = getTransactionsSummary.execute(StatementPeriod.ofDays(from, to));
        return ResponseEntity.status(HttpStatus.OK).body(toSummaryResponse(summary));
    }

    /** from/to win; the deprecated initial-date/final-date (dd/MM/yyyy) fill the gaps and now mean the same whole days. */
    private static StatementPeriod periodOf(LocalDate from, LocalDate to, String initialDate, String finalDate){
        return StatementPeriod.ofDays(from != null ? from : legacyDate(initialDate), to != null ? to : legacyDate(finalDate));
    }

    private static LocalDate legacyDate(String value){
        if (value == null) {
            return null;
        }
        try {
            return LocalDate.parse(value, DateTimeFormatter.ofPattern("dd/MM/uuuu").withResolverStyle(ResolverStyle.STRICT));
        } catch (DateTimeParseException e) {
            throw new UnprocessableEntityException("Invalid date: expected dd/MM/yyyy.");
        }
    }

    @Override
    public ResponseEntity<TransactionResponseDTO> getTransaction(UUID id){
        var transaction = toTransactionRequest(getTransactionById.execute(String.valueOf(id)));
        return ResponseEntity.status(HttpStatus.OK).body(transaction);
    }
}
