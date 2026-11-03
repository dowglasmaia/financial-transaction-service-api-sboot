package com.dowglasmaia.financialtransaction.services.impl;

import com.dowglasmaia.financialtransaction.account.application.usecase.GetCurrentAccountUseCase;
import com.dowglasmaia.financialtransaction.account.application.usecase.DebitAccountUseCase;
import com.dowglasmaia.financialtransaction.account.domain.Account;
import com.dowglasmaia.financialtransaction.transaction.infrastructure.messaging.KafkaAdminConfig;
import com.dowglasmaia.financialtransaction.transaction.infrastructure.messaging.OutboxEventEntity;
import com.dowglasmaia.financialtransaction.transaction.domain.TransactionType;
import com.dowglasmaia.financialtransaction.exeptions.UnprocessableEntityExeption;
import com.dowglasmaia.financialtransaction.transaction.infrastructure.messaging.OutboxEventJpaRepository;
import com.dowglasmaia.financialtransaction.services.RechargeMobileService;
import com.dowglasmaia.financialtransaction.services.TransactionService;
import com.dowglasmaia.financialtransaction.transaction.domain.PhoneValidator;
import com.dowglasmaia.provider.model.MobileRechargeRequestDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.kafka.support.serializer.JsonSerializer;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;

@Slf4j
@Service
public class RechargeMobileServiceImpl implements RechargeMobileService {

    /** Same serializer the Kafka producer used before the outbox, so the payload bytes are identical. */
    private static final JsonSerializer<MobileRechargeRequestDTO> PAYLOAD_SERIALIZER = new JsonSerializer<>();

    private final OutboxEventJpaRepository outboxEventRepository;
    private final GetCurrentAccountUseCase getCurrentAccount;
    private final DebitAccountUseCase debitAccount;
    private final TransactionService transactionService;

    @Autowired
    public RechargeMobileServiceImpl(OutboxEventJpaRepository outboxEventRepository,
                                     GetCurrentAccountUseCase getCurrentAccount,
                                     DebitAccountUseCase debitAccount,
                                     TransactionService transactionService
    ){
        this.outboxEventRepository = outboxEventRepository;
        this.getCurrentAccount = getCurrentAccount;
        this.debitAccount = debitAccount;
        this.transactionService = transactionService;
    }

    /**
     * Debit, ledger row and the Kafka event (as an outbox row) are written in ONE transaction; the outbox relay
     * publishes the event afterwards (at-least-once). A failure anywhere rolls everything back.
     */
    @Override
    @Transactional
    public void makeRecharge(MobileRechargeRequestDTO request){
        validatePhoneNumber(request);

        Account account = getCurrentAccount.execute();

        debitAccount.execute(account.getId(), request.getAmount());

        transactionService.save(TransactionType.PHONE_RECHARGE, account.getId(), request.getAmount(), null);

        outboxEventRepository.save(OutboxEventEntity.builder()
              .topic(KafkaAdminConfig.RECHARGE_MOBILE_TOPIC)
              .payload(new String(PAYLOAD_SERIALIZER.serialize(KafkaAdminConfig.RECHARGE_MOBILE_TOPIC, request), StandardCharsets.UTF_8))
              .payloadType(MobileRechargeRequestDTO.class.getName())
              .createdAt(OffsetDateTime.now())
              .build());
    }

    private void validatePhoneNumber(MobileRechargeRequestDTO request){
        String phoneNumber = String.valueOf(request.getAreaCode()) + request.getPhoneNumber();

        if (!PhoneValidator.isValidPhoneNumber(phoneNumber)) {
            throw new UnprocessableEntityExeption("Invalid cell phone numer", HttpStatus.UNPROCESSABLE_ENTITY);
        }
    }


}
