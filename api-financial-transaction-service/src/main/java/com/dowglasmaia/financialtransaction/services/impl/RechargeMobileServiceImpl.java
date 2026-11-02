package com.dowglasmaia.financialtransaction.services.impl;

import com.dowglasmaia.financialtransaction.config.kafka.KafkaAdminConfig;
import com.dowglasmaia.financialtransaction.entity.Account;
import com.dowglasmaia.financialtransaction.entity.OutboxEvent;
import com.dowglasmaia.financialtransaction.entity.TransactionTypeEnum;
import com.dowglasmaia.financialtransaction.exeptions.UnprocessableEntityExeption;
import com.dowglasmaia.financialtransaction.repository.OutboxEventRepository;
import com.dowglasmaia.financialtransaction.services.RechargeMobileService;
import com.dowglasmaia.financialtransaction.services.TransactionService;
import com.dowglasmaia.financialtransaction.services.client.account.AddAmountService;
import com.dowglasmaia.financialtransaction.services.client.account.FindAccountByNumberAndUserIdService;
import com.dowglasmaia.financialtransaction.utils.PhoneValidator;
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

    private final OutboxEventRepository outboxEventRepository;
    private final FindAccountByNumberAndUserIdService findAccountByNumberAndUserIdService;
    private final TransactionService transactionService;
    private final AddAmountService addAmountService;

    @Autowired
    public RechargeMobileServiceImpl(OutboxEventRepository outboxEventRepository,
                                     FindAccountByNumberAndUserIdService findAccountByNumberAndUserIdService,
                                     TransactionService transactionService,
                                     AddAmountService addAmountService
    ){
        this.outboxEventRepository = outboxEventRepository;
        this.findAccountByNumberAndUserIdService = findAccountByNumberAndUserIdService;
        this.transactionService = transactionService;
        this.addAmountService = addAmountService;
    }

    /**
     * Debit, ledger row and the Kafka event (as an outbox row) are written in ONE transaction; the outbox relay
     * publishes the event afterwards (at-least-once). A failure anywhere rolls everything back.
     */
    @Override
    @Transactional
    public void makeRecharge(MobileRechargeRequestDTO request){
        validatePhoneNumber(request);

        Account account = findAccountByNumberAndUserIdService.findByNumberAndUserId();

        addAmountService.debitById(account.getId(), request.getAmount());

        transactionService.save(TransactionTypeEnum.PHONE_RECHARGE, account.getId(), request.getAmount(), null);

        outboxEventRepository.save(OutboxEvent.builder()
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
