package com.dowglasmaia.financialtransaction.config.kafka;

import com.dowglasmaia.financialtransaction.entity.OutboxEvent;
import com.dowglasmaia.financialtransaction.repository.OutboxEventRepository;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;

import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Spec BE-03: the relay publishes pending events and keeps them pending (retry) when Kafka is unavailable. */
@ExtendWith(MockitoExtension.class)
class OutboxRelayTest {

    @Mock OutboxEventRepository repository;
    @Mock KafkaTemplate<String, String> kafkaTemplate;

    OutboxRelay relay;
    OutboxEvent event;

    @BeforeEach
    void setUp() {
        relay = new OutboxRelay(repository, kafkaTemplate);
        event = OutboxEvent.builder().id("evt-1").topic("recharge-mobile-topic")
              .payload("{\"phoneNumber\":998877665}").payloadType("com.example.Type").createdAt(OffsetDateTime.now()).build();
        when(repository.findTop50ByPublishedAtIsNullOrderByCreatedAtAsc()).thenReturn(List.of(event));
    }

    @Test
    @SuppressWarnings("unchecked")
    void publishesThePayloadAsIsWithTheTypeHeaderAndMarksItPublished() {
        when(kafkaTemplate.send(any(ProducerRecord.class))).thenReturn(CompletableFuture.completedFuture(mock()));

        relay.publishPending();

        ArgumentCaptor<ProducerRecord<String, String>> sent = ArgumentCaptor.forClass(ProducerRecord.class);
        verify(kafkaTemplate).send(sent.capture());
        assertThat(sent.getValue().topic()).isEqualTo("recharge-mobile-topic");
        assertThat(sent.getValue().value()).isEqualTo("{\"phoneNumber\":998877665}");
        assertThat(new String(sent.getValue().headers().lastHeader("__TypeId__").value(), StandardCharsets.UTF_8)).isEqualTo("com.example.Type");
        verify(repository).markPublished(eq("evt-1"), any(OffsetDateTime.class));
        verify(repository, never()).markFailed(anyString(), anyString());
    }

    @Test
    @SuppressWarnings("unchecked")
    void keepsTheEventPendingWhenKafkaIsDown() {
        when(kafkaTemplate.send(any(ProducerRecord.class)))
              .thenReturn(CompletableFuture.failedFuture(new IllegalStateException("broker unavailable")));

        relay.publishPending();

        verify(repository).markFailed(eq("evt-1"), anyString());
        verify(repository, never()).markPublished(anyString(), any());
    }

    @SuppressWarnings("unchecked")
    private static SendResult<String, String> mock() {
        return org.mockito.Mockito.mock(SendResult.class);
    }
}
