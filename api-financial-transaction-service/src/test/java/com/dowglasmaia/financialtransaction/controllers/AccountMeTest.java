package com.dowglasmaia.financialtransaction.controllers;

import com.dowglasmaia.financialtransaction.support.AbstractIntegrationTest;
import com.dowglasmaia.financialtransaction.support.MockIssuer;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Spec SL-01: GET /api/v1/accounts/me returns the signed-in customer's account ({agency, number, balance}) and nothing
 * else. The account comes from the token, never from a parameter, so no other customer's data can be requested.
 */
class AccountMeTest extends AbstractIntegrationTest {

    private static final String ME = "/api/v1/accounts/me";

    private ResultActions me(String token, String query) throws Exception {
        var request = MockMvcRequestBuilders.get(ME + query);
        if (token != null) request.header("Authorization", "Bearer " + token);
        return mvc.perform(request);
    }

    @Test
    void theSeededOwnerGetsTheirAccountWithExactlyThreeFields() throws Exception {
        String body = me(MockIssuer.ownerToken(), "").andExpect(status().isOk())
              .andReturn().getResponse().getContentAsString();

        JsonNode node = json.readTree(body);
        Set<String> fields = new HashSet<>();
        for (Iterator<String> names = node.fieldNames(); names.hasNext(); ) fields.add(names.next());
        assertThat(fields).containsExactlyInAnyOrder("agency", "number", "balance");
        assertThat(node.get("agency").asText()).isEqualTo(SEED_AGENCY);
        assertThat(node.get("number").asText()).isEqualTo(SEED_ACCOUNT);
        assertThat(node.get("balance").decimalValue()).isEqualByComparingTo(SEED_BALANCE);
    }

    @Test
    void aSecondCustomerGetsADifferentAccountAndBalance() throws Exception {
        createOtherAccount("9999", new BigDecimal("1000.00"), MockIssuer.OTHER_SUB);

        me(MockIssuer.tokenFor(MockIssuer.OTHER_SUB), "").andExpect(status().isOk())
              .andExpect(jsonPath("$.number").value("9999"))
              .andExpect(jsonPath("$.balance").value(1000.00));
        me(MockIssuer.ownerToken(), "").andExpect(status().isOk())
              .andExpect(jsonPath("$.number").value(SEED_ACCOUNT));
    }

    @Test
    void parametersCannotSelectAnotherCustomersAccount() throws Exception {
        createOtherAccount("9999", new BigDecimal("1000.00"), MockIssuer.OTHER_SUB);

        me(MockIssuer.ownerToken(), "?number=9999&accountId=" + accountRepository.findByNumber("9999").orElseThrow().getId())
              .andExpect(status().isOk())
              .andExpect(jsonPath("$.number").value(SEED_ACCOUNT));
    }

    @Test
    void noTokenIs401WithTheStandardErrorBody() throws Exception {
        me(null, "").andExpect(status().isUnauthorized()).andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    @Test
    void aTokenForAnotherAudienceIs401() throws Exception {
        me(MockIssuer.token().audience("https://some-other-api.example").build(), "").andExpect(status().isUnauthorized());
    }

    @Test
    void aTokenForTheManagementApiAudienceIs401() throws Exception {
        me(MockIssuer.token().audience(MockIssuer.MANAGEMENT_API_AUDIENCE).build(), "").andExpect(status().isUnauthorized());
    }

    @Test
    void aValidTokenNotLinkedToACustomerIs403() throws Exception {
        me(MockIssuer.tokenFor("auth0|000000000000000000000000"), "").andExpect(status().isForbidden())
              .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    @Test
    void aTokenWithoutTheCustomerRoleIs403() throws Exception {
        me(MockIssuer.token().roles("admin").build(), "").andExpect(status().isForbidden());
    }
}
