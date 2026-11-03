package com.dowglasmaia.financialtransaction.characterization;

import com.dowglasmaia.financialtransaction.transaction.infrastructure.persistence.TransactionEntity;
import com.dowglasmaia.financialtransaction.transaction.domain.TransactionStatus;
import com.dowglasmaia.financialtransaction.transaction.domain.TransactionType;
import com.dowglasmaia.financialtransaction.support.AbstractIntegrationTest;
import com.dowglasmaia.provider.model.MobileRechargeRequestDTO;
import com.fasterxml.jackson.databind.JsonNode;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.support.serializer.JsonSerializer;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.utils.KafkaTestUtils;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Characterizes CURRENT behaviour of Pix and mobile recharge (spec BE-01). MaiaBank is mocked. */
class PixRechargeCharacterizationTest extends AbstractIntegrationTest {

    private static final String RECHARGE_TOPIC = "recharge-mobile-topic";

    @Autowired EmbeddedKafkaBroker kafka;

    private static String pix(String key, String type, String amount) {
        return "{\"pixKey\":\"" + key + "\",\"keyType\":\"" + type + "\",\"transactionAmount\":" + amount + "}";
    }

    // ---------------- Pix ----------------

    @Test
    void pixDebitsSenderCallsMaiaBankOnceAndWritesLedgerWithoutAgency() throws Exception {
        post("/send-pix", pix("82215344040", "cpf", "500.00")).andExpect(status().isNoContent());

        assertThat(sameValue(new BigDecimal("4500.00"), balanceOf(SEED_ACCOUNT))).isTrue();
        verify(maiaBankClient, times(1)).receiversPix(); // empty POST, no recipient data (F-09)
        TransactionEntity row = transactionRepository.findAll().get(0);
        assertThat(row.getTransactionType()).isEqualTo(TransactionType.PIX);
        assertThat(row.getAccountId()).isEqualTo(seedAccount().getId());
        assertThat(row.getAgencyId()).isEqualTo(seedAgency().getId()); // F-09 fixed: the agency is recorded
        assertThat(row.getStatus()).isEqualTo(TransactionStatus.COMPLETED);
    }

    @Test
    void pixAcceptsValidEmailAndPhoneKeys() throws Exception {
        post("/send-pix", pix("someone@example.com", "email", "10.00")).andExpect(status().isNoContent());
        post("/send-pix", pix("11999998888", "phone", "10.00")).andExpect(status().isNoContent());
        assertThat(sameValue(new BigDecimal("4980.00"), balanceOf(SEED_ACCOUNT))).isTrue();
    }

    @Test
    void pixWithInvalidKeyForItsTypeIsRejectedBeforeAnyDebit() throws Exception {
        post("/send-pix", pix("12345678900", "cpf", "10.00"))
              .andExpect(status().isUnprocessableEntity())
              .andExpect(jsonPath("$.code").value("UNPROCESSABLE_ENTITY"))
              .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.startsWith(
                    "Pix key does not exist or is invalid for the specified type:")));
        post("/send-pix", pix("not-an-email", "email", "10.00")).andExpect(status().isUnprocessableEntity());
        post("/send-pix", pix("123", "phone", "10.00")).andExpect(status().isUnprocessableEntity());
        assertThat(sameValue(SEED_BALANCE, balanceOf(SEED_ACCOUNT))).isTrue();
        verify(maiaBankClient, never()).receiversPix();
    }

    @Test
    void pixOfTheExactBalanceIsAllowed() throws Exception { // decision D-14: balance >= amount (was strict '>', F-10)
        post("/send-pix", pix("82215344040", "cpf", "5000.00")).andExpect(status().isNoContent());
        assertThat(sameValue(BigDecimal.ZERO, balanceOf(SEED_ACCOUNT))).isTrue();
        verify(maiaBankClient, times(1)).receiversPix();
    }

    @Test
    void pixAboveTheBalanceIsRejectedBeforeCallingMaiaBank() throws Exception {
        post("/send-pix", pix("82215344040", "cpf", "5000.01"))
              .andExpect(status().isUnprocessableEntity())
              .andExpect(jsonPath("$.message").value(INSUFFICIENT));
        verify(maiaBankClient, never()).receiversPix();
        assertThat(sameValue(SEED_BALANCE, balanceOf(SEED_ACCOUNT))).isTrue();
        assertThat(transactionRepository.count()).isZero();
    }

    @Test
    void pixRestoresTheBalanceWhenMaiaBankFails() throws Exception { // BR-14
        when(maiaBankClient.receiversPix()).thenThrow(new RuntimeException("MaiaBank down"));

        post("/send-pix", pix("82215344040", "cpf", "500.00"))
              .andExpect(status().isUnprocessableEntity())
              .andExpect(jsonPath("$.message").value("Fail send Pix."));

        assertThat(sameValue(SEED_BALANCE, balanceOf(SEED_ACCOUNT))).isTrue();
        // the reservation is kept for audit as FAILED; it never appears in the statement
        List<TransactionEntity> rows = transactionRepository.findAll();
        assertThat(rows).hasSize(1);
        assertThat(rows.get(0).getStatus()).isEqualTo(TransactionStatus.FAILED);
    }

    @Test
    void pixRequestValidationMessages() throws Exception { // BR-10
        post("/send-pix", "{}").andExpect(status().isUnprocessableEntity())
              .andExpect(jsonPath("$.message").value("Pix key is required"));
        post("/send-pix", "{\"pixKey\":\"x\",\"keyType\":\"cpf\",\"transactionAmount\":0}")
              .andExpect(status().isUnprocessableEntity())
              .andExpect(jsonPath("$.message").value("Transaction amount must be positive and non-null"));
        post("/send-pix", "{\"pixKey\":\"x\",\"transactionAmount\":5}")
              .andExpect(status().isUnprocessableEntity())
              .andExpect(jsonPath("$.message").value("Key type is required"));
    }

    // ---------------- Mobile recharge ----------------

    @Test
    void rechargeDebitsPublishesRequestJsonToKafkaAndWritesLedgerWithAccountNumber() throws Exception {
        post("/recharge-mobile",
              "{\"phoneNumber\":998877665,\"areaCode\":11,\"carrier\":\"VIVO\",\"amount\":50.00}")
              .andExpect(status().isNoContent());

        assertThat(sameValue(new BigDecimal("4950.00"), balanceOf(SEED_ACCOUNT))).isTrue();

        TransactionEntity row = transactionRepository.findAll().get(0);
        assertThat(row.getTransactionType()).isEqualTo(TransactionType.PHONE_RECHARGE);
        assertThat(row.getAccountId()).isEqualTo(seedAccount().getId()); // F-08 fixed: the account ID, like every other type
        assertThat(row.getAgencyId()).isNull();

        String rawEvent = firstRechargeEventValue();
        JsonNode event = json.readTree(rawEvent);
        assertThat(event.get("phoneNumber").asLong()).isEqualTo(998877665L);
        assertThat(event.get("areaCode").asInt()).isEqualTo(11);
        assertThat(event.get("carrier").asText()).isEqualTo("VIVO");
        assertThat(event.get("amount").decimalValue().compareTo(new BigDecimal("50"))).isZero();

        // byte-identical to what the producer published before the outbox (same JsonSerializer, same request)
        MobileRechargeRequestDTO sent = new MobileRechargeRequestDTO();
        sent.setPhoneNumber(998877665);
        sent.setAreaCode(11);
        sent.setCarrier(MobileRechargeRequestDTO.CarrierEnum.VIVO);
        sent.setAmount(new BigDecimal("50.00"));
        byte[] legacyBytes = new JsonSerializer<MobileRechargeRequestDTO>().serialize(RECHARGE_TOPIC, sent);
        assertThat(rawEvent.getBytes(StandardCharsets.UTF_8)).isEqualTo(legacyBytes);

        // the outbox row is marked as published once the broker acknowledged it
        await().atMost(Duration.ofSeconds(15)).untilAsserted(() -> {
            var rows = outboxEventRepository.findAll();
            assertThat(rows).hasSize(1);
            assertThat(rows.get(0).getPublishedAt()).isNotNull();
            assertThat(rows.get(0).getTopic()).isEqualTo(RECHARGE_TOPIC);
            assertThat(rows.get(0).getPayload()).isEqualTo(rawEvent);
        });
    }

    @Test
    void rechargeWithInvalidPhoneIsRejectedBeforeDebit() throws Exception {
        post("/recharge-mobile", "{\"phoneNumber\":12345,\"areaCode\":11,\"carrier\":\"TIM\",\"amount\":20}")
              .andExpect(status().isUnprocessableEntity())
              .andExpect(jsonPath("$.message").value("Invalid cell phone numer"));
        assertThat(sameValue(SEED_BALANCE, balanceOf(SEED_ACCOUNT))).isTrue();
        assertThat(transactionRepository.count()).isZero();
    }

    @Test
    void rechargeOfTheExactBalanceIsAllowed() throws Exception { // decision D-14: balance >= amount (was strict '>', F-10)
        post("/recharge-mobile", "{\"phoneNumber\":998877665,\"areaCode\":11,\"carrier\":\"CLARO\",\"amount\":5000}")
              .andExpect(status().isNoContent());
        assertThat(sameValue(BigDecimal.ZERO, balanceOf(SEED_ACCOUNT))).isTrue();
    }

    @Test
    void rechargeAboveTheBalanceIsRejectedAndWritesNothing() throws Exception {
        post("/recharge-mobile", "{\"phoneNumber\":998877665,\"areaCode\":11,\"carrier\":\"CLARO\",\"amount\":5000.01}")
              .andExpect(status().isUnprocessableEntity())
              .andExpect(jsonPath("$.message").value(INSUFFICIENT));
        assertThat(transactionRepository.count()).isZero();
        assertThat(outboxEventRepository.count()).isZero();
    }

    @Test
    void rechargeRequiredFieldMessages() throws Exception { // BR-40
        post("/recharge-mobile", "{\"areaCode\":11,\"carrier\":\"TIM\",\"amount\":20}")
              .andExpect(status().isUnprocessableEntity())
              .andExpect(jsonPath("$.message").value("Phone number is required"));
        post("/recharge-mobile", "{\"phoneNumber\":998877665,\"carrier\":\"TIM\",\"amount\":20}")
              .andExpect(status().isUnprocessableEntity())
              .andExpect(jsonPath("$.message").value("Area code is required"));
        post("/recharge-mobile", "{\"phoneNumber\":998877665,\"areaCode\":11,\"amount\":20}")
              .andExpect(status().isUnprocessableEntity())
              .andExpect(jsonPath("$.message").value("Invalid carrier"));
    }

    private String firstRechargeEventValue() throws Exception {
        Map<String, Object> props = KafkaTestUtils.consumerProps("be01-" + System.nanoTime(), "true", kafka);
        props.put("auto.offset.reset", "earliest");
        try (Consumer<String, String> consumer = new DefaultKafkaConsumerFactory<>(
              props, new StringDeserializer(), new StringDeserializer()).createConsumer()) {
            kafka.consumeFromAnEmbeddedTopic(consumer, RECHARGE_TOPIC);
            ConsumerRecord<String, String> record = KafkaTestUtils.getSingleRecord(consumer, RECHARGE_TOPIC, Duration.ofSeconds(15));
            return record.value();
        }
    }
}
