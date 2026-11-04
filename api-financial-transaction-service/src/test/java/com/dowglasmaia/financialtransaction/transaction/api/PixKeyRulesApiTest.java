package com.dowglasmaia.financialtransaction.transaction.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.dowglasmaia.financialtransaction.support.AbstractIntegrationTest;
import com.dowglasmaia.financialtransaction.support.MockIssuer;
import java.math.BigDecimal;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

/** SL-06 (F-11): a bad key of any type is a 422 with the existing message, never a 500, and nothing moves. */
class PixKeyRulesApiTest extends AbstractIntegrationTest {

    @Autowired JdbcTemplate jdbc;

    @BeforeEach
    @AfterEach
    void clean() {
        jdbc.update("delete from idempotency_key");
    }

    private static String pix(String key, String type, String amount) {
        return "{\"pixKey\":\"" + key + "\",\"keyType\":\"" + type + "\",\"transactionAmount\":" + amount + "}";
    }

    @ParameterizedTest(name = "{1} key \"{0}\" is refused")
    @CsvSource(delimiter = '|', value = {
          "1|cpf", "5299822472|cpf", "529982247251|cpf", "52998224724|cpf", "00000000000|cpf", "11111111111|cpf", "abcdefghijk|cpf",
          "a..b@example.com|email", "a@b|email", "a@-example.com|email", "nope|email",
          "1199999888|phone", "11899998888|phone", "+5511999998888|phone", "(11)99999-8888|phone"})
    void aBadKeyIsA422ThatMovesNothingAndNeverCallsMaiaBank(String key, String type) throws Exception {
        post("/send-pix", pix(key, type, "10.00"))
              .andExpect(status().isUnprocessableEntity())
              .andExpect(jsonPath("$.code").value("UNPROCESSABLE_ENTITY"))
              .andExpect(jsonPath("$.message").value("Pix key does not exist or is invalid for the specified type: " + type));
        assertThat(balanceOf(SEED_ACCOUNT)).isEqualByComparingTo(SEED_BALANCE);
        assertThat(transactionRepository.count()).isZero();
        verify(maiaBankClient, never()).receiversPix();
    }

    @ParameterizedTest(name = "{1} key \"{0}\" is accepted")
    @CsvSource(delimiter = '|', value = {"52998224725|cpf", "529.982.247-25|cpf", "user+tag@mail.example.com.br|email", "21912345678|phone"})
    void aGoodKeyIsSent(String key, String type) throws Exception {
        post("/send-pix", pix(key, type, "10.00")).andExpect(status().isNoContent());
        assertThat(balanceOf(SEED_ACCOUNT)).isEqualByComparingTo(SEED_BALANCE.subtract(new BigDecimal("10.00")));
    }

    @Test
    void replayingAPixKeySendsOnePixAndDebitsOnce() throws Exception {
        for (int i = 0; i < 3; i++) {
            var request = MockMvcRequestBuilders.post("/api/v1/transactions/send-pix")
                  .contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON)
                  .content(pix("52998224725", "cpf", "40.00")).header("Authorization", "Bearer " + MockIssuer.ownerToken())
                  .header("Idempotency-Key", "pix-replay-1");
            var result = mvc.perform(request).andExpect(status().isNoContent());
            if (i > 0) result.andExpect(header().string("Idempotent-Replayed", "true"));
        }
        verify(maiaBankClient, times(1)).receiversPix();
        assertThat(balanceOf(SEED_ACCOUNT)).isEqualByComparingTo(SEED_BALANCE.subtract(new BigDecimal("40.00")));
        assertThat(transactionRepository.count()).isEqualTo(1);
    }
}
