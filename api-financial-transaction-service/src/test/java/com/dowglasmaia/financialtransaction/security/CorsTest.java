package com.dowglasmaia.financialtransaction.security;

import com.dowglasmaia.financialtransaction.support.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Spec BE-04: CORS preflight is answered without a token, and only for the configured SPA origin. */
class CorsTest extends AbstractIntegrationTest {

    private static final String ALLOWED = "http://localhost:4200";

    private org.springframework.test.web.servlet.ResultActions preflight(String origin) throws Exception {
        return mvc.perform(MockMvcRequestBuilders.options("/api/v1/transactions/deposit")
              .header("Origin", origin)
              .header("Access-Control-Request-Method", "POST")
              .header("Access-Control-Request-Headers", "authorization,content-type"));
    }

    @Test
    void preflightFromTheConfiguredOriginIsAllowedWithoutAToken() throws Exception {
        preflight(ALLOWED).andExpect(status().isOk())
              .andExpect(header().string("Access-Control-Allow-Origin", ALLOWED));
    }

    @Test
    void preflightFromAnotherOriginIsRejected() throws Exception {
        preflight("https://evil.example.com").andExpect(status().isForbidden());
    }
}
