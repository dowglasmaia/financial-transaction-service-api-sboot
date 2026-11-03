package com.dowglasmaia.financialtransaction.transaction.infrastructure.messaging;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.dowglasmaia.financialtransaction.transaction.domain.MobileRecharge;
import com.dowglasmaia.provider.model.MobileRechargeRequestDTO;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** The outbox row carries exactly the bytes and the type header the Kafka producer used before the outbox. */
@ExtendWith(MockitoExtension.class)
class OutboxRechargeEventPublisherTest {

    @Mock OutboxEventJpaRepository outbox;

    @Test
    void writesAnOutboxRowWithTheContractPayloadTopicAndTypeHeader() {
        when(outbox.save(org.mockito.ArgumentMatchers.any(OutboxEventEntity.class))).thenAnswer(call -> call.getArgument(0));

        new OutboxRechargeEventPublisher(outbox).publish(new MobileRecharge(11, 998877665, new BigDecimal("50.00"), "VIVO"));

        ArgumentCaptor<OutboxEventEntity> saved = ArgumentCaptor.forClass(OutboxEventEntity.class);
        verify(outbox).save(saved.capture());
        OutboxEventEntity row = saved.getValue();
        assertThat(row.getTopic()).isEqualTo("recharge-mobile-topic");
        assertThat(row.getPayload()).isEqualTo("{\"phoneNumber\":998877665,\"areaCode\":11,\"carrier\":\"VIVO\",\"amount\":50.00}");
        assertThat(row.getPayloadType()).isEqualTo(MobileRechargeRequestDTO.class.getName());
        assertThat(row.getCreatedAt()).isNotNull();
        assertThat(row.getPublishedAt()).isNull();
    }
}
