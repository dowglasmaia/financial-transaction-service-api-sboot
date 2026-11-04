package com.dowglasmaia.financialtransaction.transaction.infrastructure.messaging;

import com.dowglasmaia.financialtransaction.transaction.infrastructure.persistence.OutboxEventEntity;
import com.dowglasmaia.financialtransaction.transaction.infrastructure.persistence.OutboxEventJpaRepository;
import com.dowglasmaia.financialtransaction.transaction.application.port.RechargeEventPublisher;
import com.dowglasmaia.financialtransaction.transaction.domain.MobileRecharge;
import com.dowglasmaia.provider.model.MobileRechargeRequestDTO;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import org.springframework.kafka.support.serializer.JsonSerializer;
import org.springframework.stereotype.Component;

/**
 * Writes the recharge event as an outbox row (no transaction of its own: it joins the caller's one); the outbox relay
 * publishes it afterwards, at least once. The payload is the contract DTO serialized by the same serializer the Kafka
 * producer used before the outbox, so the bytes and the {@code __TypeId__} header are identical.
 */
@Component
class OutboxRechargeEventPublisher implements RechargeEventPublisher {

    private static final JsonSerializer<MobileRechargeRequestDTO> PAYLOAD_SERIALIZER = new JsonSerializer<>();

    private final OutboxEventJpaRepository outbox;

    OutboxRechargeEventPublisher(OutboxEventJpaRepository outbox) {
        this.outbox = outbox;
    }

    @Override
    public void publish(MobileRecharge recharge) {
        MobileRechargeRequestDTO payload = new MobileRechargeRequestDTO()
              .phoneNumber(recharge.phoneNumber())
              .areaCode(recharge.areaCode())
              .carrier(MobileRechargeRequestDTO.CarrierEnum.fromValue(recharge.carrier()))
              .amount(recharge.amount());

        outbox.save(OutboxEventEntity.builder()
              .topic(KafkaAdminConfig.RECHARGE_MOBILE_TOPIC)
              .payload(new String(PAYLOAD_SERIALIZER.serialize(KafkaAdminConfig.RECHARGE_MOBILE_TOPIC, payload), StandardCharsets.UTF_8))
              .payloadType(MobileRechargeRequestDTO.class.getName())
              .createdAt(OffsetDateTime.now())
              .build());
    }
}
