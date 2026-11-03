package com.dowglasmaia.financialtransaction.security;

import com.dowglasmaia.financialtransaction.support.AbstractIntegrationTest;
import com.dowglasmaia.financialtransaction.support.MockIssuer;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Spec BE-04: authentication (401) and role authorization (403), against a real signature/issuer/audience/expiry
 * validation served by the mock issuer.
 */
class AuthenticationTest extends AbstractIntegrationTest {

    private static final String STATEMENT = "?initial-date=01/01/2000&final-date=31/12/2100";
    private static final String DEPOSIT = "{\"agency\":\"0211\",\"account\":\"2022\",\"amount\":10.00}";

    // ---- 401: the caller is not authenticated

    @Test
    void noTokenIs401WithTheStandardBodyAndABearerChallenge() throws Exception {
        getAs(null, STATEMENT)
              .andExpect(status().isUnauthorized())
              .andExpect(header().string("WWW-Authenticate", org.hamcrest.Matchers.startsWith("Bearer")))
              .andExpect(jsonPath("$.code").value("UNAUTHORIZED"))
              .andExpect(jsonPath("$.message").isNotEmpty());
    }

    @Test
    void aWriteWithoutTokenIs401AndChangesNothing() throws Exception {
        postAs(null, "/deposit", DEPOSIT).andExpect(status().isUnauthorized());
        assertThat(sameValue(SEED_BALANCE, balanceOf(SEED_ACCOUNT))).isTrue();
        assertThat(transactionRepository.count()).isZero();
    }

    @Test
    void aMalformedTokenIs401() throws Exception {
        getAs("not-a-jwt", STATEMENT).andExpect(status().isUnauthorized());
    }

    @Test
    void aTokenFromAnotherIssuerIs401() throws Exception {
        getAs(MockIssuer.token().issuer("https://evil.example.com/").build(), STATEMENT).andExpect(status().isUnauthorized());
    }

    @Test
    void aTokenForAnotherAudienceIs401() throws Exception {
        getAs(MockIssuer.token().audience("https://some-other-api.example.com").build(), STATEMENT).andExpect(status().isUnauthorized());
    }

    @Test
    void aTokenForTheAuth0ManagementApiIs401() throws Exception {
        getAs(MockIssuer.token().audience(MockIssuer.MANAGEMENT_API_AUDIENCE).build(), STATEMENT).andExpect(status().isUnauthorized());
    }

    @Test
    void anExpiredTokenIs401() throws Exception {
        getAs(MockIssuer.token().expiresAt(Instant.now().minusSeconds(300)).build(), STATEMENT).andExpect(status().isUnauthorized());
    }

    @Test
    void aTokenWithAForgedSignatureIs401() throws Exception {
        getAs(MockIssuer.token().signedByAnotherKey().build(), STATEMENT).andExpect(status().isUnauthorized());
    }

    @Test
    void aTokenWithoutSubjectIs401() throws Exception {
        getAs(MockIssuer.token().subject(null).build(), STATEMENT).andExpect(status().isUnauthorized());
    }

    // ---- 403: authenticated, but not allowed

    @Test
    void aValidTokenWithoutTheRolesClaimIs403() throws Exception {
        getAs(MockIssuer.token().withoutRolesClaim().build(), STATEMENT)
              .andExpect(status().isForbidden())
              .andExpect(jsonPath("$.code").value("FORBIDDEN"))
              .andExpect(jsonPath("$.message").isNotEmpty());
    }

    @Test
    void aValidTokenWithoutTheCustomerRoleIs403() throws Exception {
        getAs(MockIssuer.token().roles("admin").build(), STATEMENT).andExpect(status().isForbidden());
        postAs(MockIssuer.token().roles("admin").build(), "/deposit", DEPOSIT).andExpect(status().isForbidden());
        assertThat(transactionRepository.count()).isZero();
    }

    @Test
    void aSubjectNotLinkedToAnyUserIs403OnReadsAndWrites() throws Exception {
        String stranger = MockIssuer.tokenFor("auth0|nobody-linked-this");
        getAs(stranger, STATEMENT).andExpect(status().isForbidden()).andExpect(jsonPath("$.code").value("FORBIDDEN"));
        postAs(stranger, "/atm-withdrawal", "{\"amount\":10.00}").andExpect(status().isForbidden());
        assertThat(sameValue(SEED_BALANCE, balanceOf(SEED_ACCOUNT))).isTrue();
    }

    // ---- 200: the happy path and the public/closed endpoints

    @Test
    void aValidCustomerTokenIsAccepted() throws Exception {
        getAs(MockIssuer.ownerToken(), STATEMENT).andExpect(status().isOk());
    }

    @Test
    void healthIsPublic() throws Exception {
        int status = mvc.perform(MockMvcRequestBuilders.get("/actuator/health")).andReturn().getResponse().getStatus();
        assertThat(status).as("health must not require a token").isNotIn(401, 403);
    }

    @Test
    void otherActuatorEndpointsAreNotOpenToCustomers() throws Exception {
        mvc.perform(MockMvcRequestBuilders.get("/actuator/metrics")).andExpect(status().isUnauthorized());
        mvc.perform(MockMvcRequestBuilders.get("/actuator/metrics").header("Authorization", "Bearer " + MockIssuer.ownerToken()))
              .andExpect(status().isForbidden());
    }

    @Test
    void theApiUsesIssuerDiscoveryAgainstTheIssuerItIsConfiguredWith() throws Exception {
        getAs(MockIssuer.ownerToken(), STATEMENT).andExpect(status().isOk());
        assertThat(MockIssuer.REQUESTED_PATHS).anyMatch(p -> p.endsWith("/.well-known/openid-configuration"));
        assertThat(MockIssuer.REQUESTED_PATHS).anyMatch(p -> p.endsWith("/.well-known/jwks.json"));
    }
}
