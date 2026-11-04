package com.dowglasmaia.financialtransaction.transaction.api;

import com.dowglasmaia.financialtransaction.support.AbstractIntegrationTest;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * SL-03: the SHAPE of all four money POST bodies is validated once: a null, missing, blank or non-positive field answers
 * 422 with the known message, never 500, and nothing moves. Operation-specific rules (Pix key check digits, recharge
 * phone/carrier) stay in SL-04 and SL-06.
 */
class BodyShapeValidationTest extends AbstractIntegrationTest {

    private static final String AMOUNT = "Amount must be positive and non-null";
    private static final String REQUIRED = "Request body is required";

    static Stream<Arguments> badBodies() {
        return Stream.of(
              // withdrawal
              arguments("/atm-withdrawal", "null", REQUIRED),
              arguments("/atm-withdrawal", "{}", AMOUNT),
              arguments("/atm-withdrawal", "{\"amount\":null}", AMOUNT),
              arguments("/atm-withdrawal", "{\"amount\":0}", AMOUNT),
              arguments("/atm-withdrawal", "{\"amount\":-5}", AMOUNT),
              // deposit
              arguments("/deposit", "null", REQUIRED),
              arguments("/deposit", "{}", "Account is required"),
              arguments("/deposit", "{\"agency\":\"0211\",\"account\":\"   \",\"amount\":10}", "Account is required"),
              arguments("/deposit", "{\"agency\":\"0211\",\"account\":\"2022\"}", AMOUNT),
              arguments("/deposit", "{\"agency\":\"0211\",\"account\":\"2022\",\"amount\":0}", AMOUNT),
              arguments("/deposit", "{\"agency\":\"0211\",\"account\":\"2022\",\"amount\":-1}", AMOUNT),
              arguments("/deposit", "{\"agency\":\"  \",\"account\":\"2022\",\"amount\":10}", "Agency is required"),
              arguments("/deposit", "{\"account\":\"2022\",\"amount\":10}", "Agency is required"),
              // mobile recharge
              arguments("/recharge-mobile", "null", REQUIRED),
              arguments("/recharge-mobile", "{}", "Phone number is required"),
              arguments("/recharge-mobile", "{\"phoneNumber\":999998888,\"areaCode\":11,\"carrier\":\"VIVO\"}", AMOUNT),
              arguments("/recharge-mobile", "{\"phoneNumber\":999998888,\"areaCode\":11,\"carrier\":\"VIVO\",\"amount\":0}", AMOUNT),
              arguments("/recharge-mobile", "{\"phoneNumber\":999998888,\"areaCode\":11,\"carrier\":\"VIVO\",\"amount\":-3}", AMOUNT),
              arguments("/recharge-mobile", "{\"phoneNumber\":999998888,\"amount\":10,\"carrier\":\"VIVO\"}", "Area code is required"),
              arguments("/recharge-mobile", "{\"phoneNumber\":999998888,\"areaCode\":11,\"amount\":10}", "Invalid carrier"),
              // Pix
              arguments("/send-pix", "null", REQUIRED),
              arguments("/send-pix", "{}", "Pix key is required"),
              arguments("/send-pix", "{\"pixKey\":\"   \",\"keyType\":\"cpf\",\"transactionAmount\":10}", "Pix key is required"),
              arguments("/send-pix", "{\"pixKey\":\"82215344040\",\"keyType\":\"cpf\"}", "Transaction amount must be positive and non-null"),
              arguments("/send-pix", "{\"pixKey\":\"82215344040\",\"keyType\":\"cpf\",\"transactionAmount\":0}", "Transaction amount must be positive and non-null"),
              arguments("/send-pix", "{\"pixKey\":\"82215344040\",\"keyType\":\"cpf\",\"transactionAmount\":-1}", "Transaction amount must be positive and non-null"),
              arguments("/send-pix", "{\"pixKey\":\"82215344040\",\"transactionAmount\":10}", "Key type is required")
        );
    }

    @ParameterizedTest(name = "{0} <- {1}")
    @MethodSource("badBodies")
    void aBadBodyIsAn422WithTheKnownMessageAndNothingMoves(String path, String body, String message) throws Exception {
        post(path, body)
              .andExpect(status().isUnprocessableEntity())
              .andExpect(jsonPath("$.code").value("UNPROCESSABLE_ENTITY"))
              .andExpect(jsonPath("$.message").value(message));

        assertThat(transactionRepository.count()).isZero();
        assertThat(balanceOf(SEED_ACCOUNT)).isEqualByComparingTo(SEED_BALANCE);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("paths")
    void aMissingOrUnreadableBodyIsAn422NeverA500(String path) throws Exception {
        postAs(com.dowglasmaia.financialtransaction.support.MockIssuer.ownerToken(), path, "")
              .andExpect(status().isUnprocessableEntity())
              .andExpect(jsonPath("$.message").value(REQUIRED));
        post(path, "{not json")
              .andExpect(status().isUnprocessableEntity())
              .andExpect(jsonPath("$.message").value("Request body is malformed."));
        post(path, "{\"amount\":\"abc\",\"transactionAmount\":\"abc\",\"pixKey\":\"x\",\"keyType\":\"cpf\"}")
              .andExpect(status().isUnprocessableEntity());
    }

    static Stream<String> paths() {
        return Stream.of("/atm-withdrawal", "/deposit", "/recharge-mobile", "/send-pix");
    }
}
