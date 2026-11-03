package com.dowglasmaia.financialtransaction.security;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtDecoders;
import org.springframework.security.oauth2.jwt.BadJwtException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Spec BE-04, opt-in (never runs in CI): proves the decoder can bootstrap from the REAL tenant's public discovery
 * document with the trailing-slash issuer. Only public endpoints are called, no credentials.
 * Run: mvn test -Dtest=LiveTenantDiscoveryTest -Dauth.live=true -Dauth.issuer=https://dev-xxxx.eu.auth0.com/
 */
@EnabledIfSystemProperty(named = "auth.live", matches = "true")
class LiveTenantDiscoveryTest {

    @Test
    void theDecoderBootstrapsFromTheTenantAndRejectsAnUnsignedToken() {
        String issuer = System.getProperty("auth.issuer");
        assertThat(issuer).isNotBlank().endsWith("/");

        JwtDecoder decoder = JwtDecoders.fromIssuerLocation(issuer); // discovery + JWKS location resolved here

        assertThatThrownBy(() -> decoder.decode("eyJhbGciOiJub25lIn0.eyJzdWIiOiJ4In0."))
              .isInstanceOf(BadJwtException.class);
    }
}
