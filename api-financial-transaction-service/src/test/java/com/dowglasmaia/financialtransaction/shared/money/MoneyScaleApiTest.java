package com.dowglasmaia.financialtransaction.shared.money;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.dowglasmaia.financialtransaction.support.AbstractIntegrationTest;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/** BE-08: an amount with more than two decimal places is a 422 on every money endpoint and nothing moves. */
class MoneyScaleApiTest extends AbstractIntegrationTest {

    private static String body(String path, String amount) {
        return switch (path) {
            case "/atm-withdrawal" -> "{\"amount\":" + amount + "}";
            case "/deposit" -> "{\"agency\":\"0211\",\"account\":\"2022\",\"amount\":" + amount + "}";
            case "/recharge-mobile" -> "{\"phoneNumber\":912345678,\"areaCode\":11,\"carrier\":\"VIVO\",\"amount\":" + amount + "}";
            default -> "{\"pixKey\":\"52998224725\",\"keyType\":\"cpf\",\"transactionAmount\":" + amount + "}";
        };
    }

    @ParameterizedTest(name = "{0} with {1} is refused")
    @CsvSource({"/atm-withdrawal,10.005", "/deposit,10.005", "/recharge-mobile,0.001", "/send-pix,1.234"})
    void moreThanTwoDecimalPlacesIsA422ThatMovesNothing(String path, String amount) throws Exception {
        post(path, body(path, amount))
              .andExpect(status().isUnprocessableEntity())
              .andExpect(jsonPath("$.code").value("UNPROCESSABLE_ENTITY"))
              .andExpect(jsonPath("$.message").value("Amount must have at most 2 decimal places"));
        assertThat(balanceOf(SEED_ACCOUNT)).isEqualByComparingTo(SEED_BALANCE);
        assertThat(transactionRepository.count()).isZero();
    }

    @ParameterizedTest(name = "{0} with {1} is accepted")
    @CsvSource({"/atm-withdrawal,10.5", "/deposit,10.50", "/recharge-mobile,10", "/send-pix,10.500"})
    void twoOrFewerDecimalPlacesStillWork(String path, String amount) throws Exception {
        post(path, body(path, amount)).andExpect(status().isNoContent());
    }
}
