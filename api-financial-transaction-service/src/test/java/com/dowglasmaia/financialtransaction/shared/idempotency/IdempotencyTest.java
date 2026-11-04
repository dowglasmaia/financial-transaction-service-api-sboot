package com.dowglasmaia.financialtransaction.shared.idempotency;

import com.dowglasmaia.financialtransaction.support.AbstractIntegrationTest;
import com.dowglasmaia.financialtransaction.support.MockIssuer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * SL-03: {@code Idempotency-Key} on the four money POSTs. Replaying a key never moves money again; parallel requests with
 * the same key move it once; the key belongs to the signed-in customer; a failed attempt does not burn the key.
 */
class IdempotencyTest extends AbstractIntegrationTest {

    private static final String W100 = "{\"amount\":100.00}";

    @Autowired JdbcTemplate jdbc;

    @BeforeEach
    @AfterEach
    void clean() {
        jdbc.update("delete from idempotency_key");
    }

    private ResultActions send(String token, String path, String key, String body) throws Exception {
        var request = MockMvcRequestBuilders.post("/api/v1/transactions" + path)
              .contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON).content(body)
              .header("Authorization", "Bearer " + token);
        if (key != null) request.header("Idempotency-Key", key);
        return mvc.perform(request);
    }

    private ResultActions send(String path, String key, String body) throws Exception {
        return send(MockIssuer.ownerToken(), path, key, body);
    }

    @Test
    void replayingTheSameKeyAndBodyDebitsOnceAndAnswersTheOriginalResult() throws Exception {
        send("/atm-withdrawal", "k-1", W100).andExpect(status().isNoContent()).andExpect(header().doesNotExist("Idempotent-Replayed"));
        send("/atm-withdrawal", "k-1", W100).andExpect(status().isNoContent()).andExpect(header().string("Idempotent-Replayed", "true"));
        send("/atm-withdrawal", "k-1", W100).andExpect(status().isNoContent()).andExpect(header().string("Idempotent-Replayed", "true"));

        assertThat(balanceOf(SEED_ACCOUNT)).isEqualByComparingTo(SEED_BALANCE.subtract(new BigDecimal("100.00")));
        assertThat(transactionRepository.count()).isEqualTo(1);
    }

    @Test
    void theSameKeyWithADifferentBodyIsRejectedAndNothingMoves() throws Exception {
        send("/atm-withdrawal", "k-2", W100).andExpect(status().isNoContent());
        send("/atm-withdrawal", "k-2", "{\"amount\":200.00}")
              .andExpect(status().isUnprocessableEntity())
              .andExpect(jsonPath("$.code").value("UNPROCESSABLE_ENTITY"))
              .andExpect(jsonPath("$.message").value("Idempotency-Key reuse with a different request."));

        assertThat(balanceOf(SEED_ACCOUNT)).isEqualByComparingTo(SEED_BALANCE.subtract(new BigDecimal("100.00")));
        assertThat(transactionRepository.count()).isEqualTo(1);
    }

    @Test
    void parallelRequestsWithTheSameKeyDebitOnce() throws Exception {
        List<Callable<Integer>> tasks = new ArrayList<>();
        for (int i = 0; i < 12; i++) tasks.add(() -> send("/atm-withdrawal", "k-parallel", W100).andReturn().getResponse().getStatus());

        List<Integer> statuses = inParallel(tasks);

        assertThat(statuses).as("every caller gets the one result").allMatch(s -> s == 204);
        assertThat(balanceOf(SEED_ACCOUNT)).isEqualByComparingTo(SEED_BALANCE.subtract(new BigDecimal("100.00")));
        assertThat(transactionRepository.count()).isEqualTo(1);
    }

    @Test
    void withoutAKeyEveryRequestIsAnotherOperationAsToday() throws Exception {
        send("/atm-withdrawal", null, W100).andExpect(status().isNoContent());
        send("/atm-withdrawal", null, W100).andExpect(status().isNoContent());

        assertThat(balanceOf(SEED_ACCOUNT)).isEqualByComparingTo(SEED_BALANCE.subtract(new BigDecimal("200.00")));
        assertThat(transactionRepository.count()).isEqualTo(2);
    }

    @Test
    void theKeyBelongsToTheCustomer() throws Exception {
        createOtherAccount("3033", new BigDecimal("1000.00"), MockIssuer.OTHER_SUB);

        send(MockIssuer.ownerToken(), "/atm-withdrawal", "shared-key", W100).andExpect(status().isNoContent());
        send(MockIssuer.tokenFor(MockIssuer.OTHER_SUB), "/atm-withdrawal", "shared-key", W100)
              .andExpect(status().isNoContent()).andExpect(header().doesNotExist("Idempotent-Replayed"));

        assertThat(balanceOf(SEED_ACCOUNT)).isEqualByComparingTo(SEED_BALANCE.subtract(new BigDecimal("100.00")));
        assertThat(balanceOf("3033")).isEqualByComparingTo(new BigDecimal("900.00"));
    }

    @Test
    void aBusinessFailureDoesNotBurnTheKey() throws Exception {
        send("/atm-withdrawal", "k-retry", "{\"amount\":9000.00}").andExpect(status().isUnprocessableEntity());
        // same key, a body the user fixed: it is a new attempt, not a "different request" error
        send("/atm-withdrawal", "k-retry", W100).andExpect(status().isNoContent());
        send("/atm-withdrawal", "k-retry", W100).andExpect(status().isNoContent()).andExpect(header().string("Idempotent-Replayed", "true"));

        assertThat(balanceOf(SEED_ACCOUNT)).isEqualByComparingTo(SEED_BALANCE.subtract(new BigDecimal("100.00")));
        assertThat(transactionRepository.count()).isEqualTo(1);
    }

    @Test
    void aKeyOlderThanTwentyFourHoursIsForgotten() throws Exception {
        send("/atm-withdrawal", "k-old", W100).andExpect(status().isNoContent());
        jdbc.update("update idempotency_key set created_at = now() - interval '25 hours'");

        send("/atm-withdrawal", "k-old", W100).andExpect(status().isNoContent()).andExpect(header().doesNotExist("Idempotent-Replayed"));

        assertThat(transactionRepository.count()).isEqualTo(2);
    }

    @Test
    void aBlankOrTooLongKeyIsAn422() throws Exception {
        send("/atm-withdrawal", "   ", W100).andExpect(status().isUnprocessableEntity())
              .andExpect(jsonPath("$.message").value("Invalid Idempotency-Key: use 1 to 64 characters."));
        send("/atm-withdrawal", "x".repeat(65), W100).andExpect(status().isUnprocessableEntity())
              .andExpect(jsonPath("$.message").value("Invalid Idempotency-Key: use 1 to 64 characters."));
        send("/atm-withdrawal", "x".repeat(64), W100).andExpect(status().isNoContent());

        assertThat(transactionRepository.count()).isEqualTo(1);
    }

    @Test
    void theComponentIsSharedByDepositAndRecharge() throws Exception {
        String deposit = "{\"agency\":\"0211\",\"account\":\"2022\",\"amount\":50.00}";
        send("/deposit", "k-dep", deposit).andExpect(status().isNoContent());
        send("/deposit", "k-dep", deposit).andExpect(status().isNoContent()).andExpect(header().string("Idempotent-Replayed", "true"));
        assertThat(balanceOf(SEED_ACCOUNT)).isEqualByComparingTo(SEED_BALANCE.add(new BigDecimal("50.00")));

        String recharge = "{\"phoneNumber\":999998888,\"areaCode\":11,\"carrier\":\"VIVO\",\"amount\":20.00}";
        send("/recharge-mobile", "k-rec", recharge).andExpect(status().isNoContent());
        send("/recharge-mobile", "k-rec", recharge).andExpect(status().isNoContent()).andExpect(header().string("Idempotent-Replayed", "true"));
        assertThat(balanceOf(SEED_ACCOUNT)).isEqualByComparingTo(SEED_BALANCE.add(new BigDecimal("30.00")));
        assertThat(outboxEventRepository.count()).as("one recharge event, not two").isEqualTo(1);
    }

    @Test
    void theComponentIsSharedByPix() throws Exception {
        String pix = "{\"pixKey\":\"82215344040\",\"keyType\":\"cpf\",\"transactionAmount\":10.00}";
        send("/send-pix", "k-pix", pix).andExpect(status().isNoContent());
        send("/send-pix", "k-pix", pix).andExpect(status().isNoContent()).andExpect(header().string("Idempotent-Replayed", "true"));

        verify(maiaBankClient, times(1)).receiversPix();
        assertThat(balanceOf(SEED_ACCOUNT)).isEqualByComparingTo(SEED_BALANCE.subtract(new BigDecimal("10.00")));
    }

    @Test
    void anUnauthenticatedRequestNeverReachesTheStore() throws Exception {
        var request = MockMvcRequestBuilders.post("/api/v1/transactions/atm-withdrawal")
              .contentType(MediaType.APPLICATION_JSON).content(W100).header("Idempotency-Key", "k-anon");
        mvc.perform(request).andExpect(status().isUnauthorized());

        assertThat(jdbc.queryForObject("select count(*) from idempotency_key", Integer.class)).isZero();
    }

    private static List<Integer> inParallel(List<Callable<Integer>> tasks) throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(tasks.size());
        CountDownLatch go = new CountDownLatch(1);
        try {
            List<Future<Integer>> futures = new ArrayList<>();
            for (Callable<Integer> task : tasks) {
                futures.add(pool.submit(() -> {
                    go.await();
                    return task.call();
                }));
            }
            go.countDown();
            List<Integer> results = new ArrayList<>();
            for (Future<Integer> future : futures) results.add(future.get(60, TimeUnit.SECONDS));
            return results;
        } finally {
            pool.shutdownNow();
        }
    }
}
