package com.dowglasmaia.financialtransaction.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

/** Event written in the same transaction as the business change and published to Kafka by the outbox relay. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "outbox_event")
public class OutboxEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    private String topic;

    private String eventKey;

    @Column(columnDefinition = "text")
    private String payload;

    private String payloadType;

    private OffsetDateTime createdAt;

    private OffsetDateTime publishedAt;

    private int attempts;

    @Column(columnDefinition = "text")
    private String lastError;
}
