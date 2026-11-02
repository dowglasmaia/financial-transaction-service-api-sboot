package com.dowglasmaia.financialtransaction.config.kafka;

import com.dowglasmaia.financialtransaction.entity.OutboxEvent;
import com.dowglasmaia.financialtransaction.repository.OutboxEventRepository;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.concurrent.TimeUnit;

/**
 * Publishes outbox rows to Kafka. At-least-once: a row is marked published only after the broker acknowledged it; if
 * publishing fails the row stays pending (attempts/last_error updated) and is retried on the next run.
 */
@Slf4j
@Component
public class OutboxRelay {

    private static final String TYPE_ID_HEADER = "__TypeId__"; // what Spring's JsonSerializer used to add

    private final OutboxEventRepository outboxEventRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;

    public OutboxRelay(OutboxEventRepository outboxEventRepository,
                       @Qualifier("outboxKafkaTemplate") KafkaTemplate<String, String> kafkaTemplate
    ){
        this.outboxEventRepository = outboxEventRepository;
        this.kafkaTemplate = kafkaTemplate;
    }

    @Scheduled(fixedDelayString = "${app.outbox.poll-interval-ms:1000}")
    public void publishPending(){
        for (OutboxEvent event : outboxEventRepository.findTop50ByPublishedAtIsNullOrderByCreatedAtAsc()) {
            publish(event);
        }
    }

    void publish(OutboxEvent event){
        try {
            var record = new ProducerRecord<>(event.getTopic(), event.getEventKey(), event.getPayload());
            if (event.getPayloadType() != null) {
                record.headers().add(TYPE_ID_HEADER, event.getPayloadType().getBytes(StandardCharsets.UTF_8));
            }
            kafkaTemplate.send(record).get(10, TimeUnit.SECONDS);
            outboxEventRepository.markPublished(event.getId(), OffsetDateTime.now());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            outboxEventRepository.markFailed(event.getId(), "interrupted");
        } catch (Exception e) {
            log.warn("Outbox event {} not published (will retry): {}", event.getId(), e.toString());
            outboxEventRepository.markFailed(event.getId(), truncate(e.toString()));
        }
    }

    private static String truncate(String text){
        return text.length() > 500 ? text.substring(0, 500) : text;
    }
}
