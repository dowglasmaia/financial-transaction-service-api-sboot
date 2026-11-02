package com.dowglasmaia.financialtransaction.support;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import com.sun.net.httpserver.HttpServer;
import org.springframework.test.context.DynamicPropertyRegistry;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Spec BE-04: a local OIDC issuer for the whole test run. It serves the discovery document and the JWKS over HTTP,
 * so the API validates tokens exactly as it does against a real tenant (signature, issuer, audience, expiry) with no
 * network access, no account and no secret. The issuer ends with a slash on purpose (Auth0 style).
 */
public final class MockIssuer {

    public static final String AUDIENCE = "https://api.financial-transaction.test";
    public static final String MANAGEMENT_API_AUDIENCE = "https://tenant.example.auth0.com/api/v2/";
    public static final String ROLES_CLAIM = "https://financial-transaction/roles";
    public static final String OWNER_SUB = "auth0|test-owner";
    public static final String OTHER_SUB = "auth0|test-other";

    private static final RSAKey KEY = generate("test-key");
    private static final RSAKey FOREIGN_KEY = generate("test-key"); // same kid, different key: a forged signature
    private static final HttpServer SERVER = start();
    private static final String ISSUER = "http://localhost:" + SERVER.getAddress().getPort() + "/";
    /** Paths the API asked the issuer for (lets a test assert discovery was really used). */
    public static final List<String> REQUESTED_PATHS = new CopyOnWriteArrayList<>();

    private MockIssuer() {
    }

    public static String issuer() {
        return ISSUER;
    }

    public static void register(DynamicPropertyRegistry registry) {
        registry.add("spring.security.oauth2.resourceserver.jwt.issuer-uri", MockIssuer::issuer);
        registry.add("app.security.audience", () -> AUDIENCE);
        registry.add("app.security.roles-claim", () -> ROLES_CLAIM);
        registry.add("app.seed.owner-subject", () -> OWNER_SUB);
    }

    /** A valid token for the seeded owner with role customer. */
    public static String ownerToken() {
        return token().build();
    }

    public static String tokenFor(String sub) {
        return token().subject(sub).build();
    }

    public static Builder token() {
        return new Builder();
    }

    public static final class Builder {
        private String subject = OWNER_SUB;
        private String issuer = ISSUER;
        private List<String> audience = List.of(AUDIENCE);
        private List<String> roles = List.of("customer");
        private boolean includeRoles = true;
        private Instant expiresAt = Instant.now().plusSeconds(600);
        private RSAKey signWith = KEY;

        public Builder subject(String value) { this.subject = value; return this; }
        public Builder issuer(String value) { this.issuer = value; return this; }
        public Builder audience(String... values) { this.audience = List.of(values); return this; }
        public Builder roles(String... values) { this.roles = List.of(values); return this; }
        public Builder withoutRolesClaim() { this.includeRoles = false; return this; }
        public Builder expiresAt(Instant value) { this.expiresAt = value; return this; }
        public Builder signedByAnotherKey() { this.signWith = FOREIGN_KEY; return this; }

        public String build() {
            JWTClaimsSet.Builder claims = new JWTClaimsSet.Builder()
                  .issuer(issuer)
                  .audience(new ArrayList<>(audience))
                  .issueTime(Date.from(Instant.now().minusSeconds(5)))
                  .expirationTime(Date.from(expiresAt))
                  .jwtID(UUID.randomUUID().toString());
            if (subject != null) claims.subject(subject);
            if (includeRoles) claims.claim(ROLES_CLAIM, roles);
            try {
                SignedJWT jwt = new SignedJWT(new JWSHeader.Builder(JWSAlgorithm.RS256).keyID(signWith.getKeyID()).build(), claims.build());
                jwt.sign(new RSASSASigner(signWith));
                return jwt.serialize();
            } catch (JOSEException e) {
                throw new IllegalStateException(e);
            }
        }
    }

    private static RSAKey generate(String kid) {
        try {
            return new RSAKeyGenerator(2048).keyID(kid).generate();
        } catch (JOSEException e) {
            throw new IllegalStateException(e);
        }
    }

    private static HttpServer start() {
        try {
            HttpServer server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
            server.createContext("/", exchange -> {
                String path = exchange.getRequestURI().getPath();
                REQUESTED_PATHS.add(path);
                String issuer = "http://localhost:" + server.getAddress().getPort() + "/";
                String body;
                int status = 200;
                if (path.endsWith("/.well-known/openid-configuration")) {
                    body = "{\"issuer\":\"" + issuer + "\",\"jwks_uri\":\"" + issuer + ".well-known/jwks.json\"}";
                } else if (path.endsWith("/.well-known/jwks.json")) {
                    body = new JWKSet(KEY.toPublicJWK()).toString();
                } else {
                    body = "{}";
                    status = 404;
                }
                byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
                exchange.getResponseHeaders().add("Content-Type", "application/json");
                exchange.sendResponseHeaders(status, bytes.length);
                exchange.getResponseBody().write(bytes);
                exchange.close();
            });
            server.setExecutor(java.util.concurrent.Executors.newCachedThreadPool());
            server.start();
            return server;
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }
}
