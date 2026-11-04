package com.dowglasmaia.financialtransaction.shared.idempotency;

import com.dowglasmaia.provider.model.ResponseErrorDTO;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.HexFormat;
import java.util.Set;

/**
 * {@code Idempotency-Key} for the four money POSTs (SL-03). It is generic: it imports no feature and never calls a use
 * case. With the header present it makes one user attempt run at most once:
 * <ul>
 *   <li>first request of a (customer, key): claims the key, lets the request run, stores the status if it succeeded and
 *       frees the key if it did not (a business failure can be fixed and sent again with the same key);</li>
 *   <li>same key, same body, finished: answers the stored status with {@code Idempotent-Replayed: true}, touching nothing;</li>
 *   <li>same key, same body, still running: waits for the first attempt and then replays it;</li>
 *   <li>same key, different body: 422.</li>
 * </ul>
 * It runs after Spring Security, so the key is scoped to the token {@code sub}. Without the header nothing changes.
 * Known limit: the key is stored in its own statement, so a crash between the money commit and {@code complete} leaves a
 * running row that is taken over after {@value #RUNNING_TAKEOVER_SECONDS} s; the window is a few milliseconds.
 */
@Component
@Order(Ordered.LOWEST_PRECEDENCE - 10)
public class IdempotencyFilter extends OncePerRequestFilter {

    static final String HEADER = "Idempotency-Key";
    static final String REPLAYED = "Idempotent-Replayed";
    static final long RUNNING_TAKEOVER_SECONDS = 60;
    private static final int MAX_BODY_BYTES = 16 * 1024;
    private static final Duration WAIT_LIMIT = Duration.ofSeconds(20);
    private static final Set<String> PATHS = Set.of(
          "/api/v1/transactions/atm-withdrawal", "/api/v1/transactions/deposit",
          "/api/v1/transactions/recharge-mobile", "/api/v1/transactions/send-pix");

    private final IdempotencyStore store;
    private final ObjectMapper json;

    public IdempotencyFilter(IdempotencyStore store, ObjectMapper json) {
        this.store = store;
        this.json = json;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !"POST".equals(request.getMethod()) || !PATHS.contains(request.getRequestURI()) || request.getHeader(HEADER) == null;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain) throws ServletException, IOException {
        String key = request.getHeader(HEADER);
        if (key.isBlank() || key.length() > 64) {
            fail(response, HttpStatus.UNPROCESSABLE_ENTITY, "Invalid Idempotency-Key: use 1 to 64 characters.");
            return;
        }
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication instanceof AnonymousAuthenticationToken || !authentication.isAuthenticated()) {
            chain.doFilter(request, response); // security answers 401 before this point; never reached for a real request
            return;
        }
        String subject = authentication.getName();

        byte[] body = request.getInputStream().readNBytes(MAX_BODY_BYTES + 1);
        if (body.length > MAX_BODY_BYTES) {
            fail(response, HttpStatus.UNPROCESSABLE_ENTITY, "Request body is too large.");
            return;
        }
        CachedBodyRequest cached = new CachedBodyRequest(request, body);
        String hash = sha256(body);

        long deadline = System.nanoTime() + WAIT_LIMIT.toNanos();
        while (true) {
            if (store.claim(subject, key, hash)) {
                run(subject, key, cached, response, chain);
                return;
            }
            var entry = store.find(subject, key);
            if (entry.isEmpty()) {
                continue; // freed between the claim and the lookup: try to claim again
            }
            var existing = entry.get();
            boolean expired = existing.age().compareTo(IdempotencyStore.TTL) > 0;
            boolean abandoned = existing.running() && existing.age().getSeconds() > RUNNING_TAKEOVER_SECONDS;
            if (expired || abandoned) {
                store.evict(subject, key, existing.requestHash());
                continue;
            }
            if (!existing.requestHash().equals(hash)) {
                fail(response, HttpStatus.UNPROCESSABLE_ENTITY, "Idempotency-Key reuse with a different request.");
                return;
            }
            if (!existing.running()) {
                response.setStatus(existing.status());
                response.setHeader(REPLAYED, "true");
                return;
            }
            if (System.nanoTime() > deadline) {
                fail(response, HttpStatus.SERVICE_UNAVAILABLE, "The first attempt with this key is still running. Try again.");
                return;
            }
            pause();
        }
    }

    private void run(String subject, String key, HttpServletRequest request, HttpServletResponse response, FilterChain chain) throws IOException, ServletException {
        try {
            chain.doFilter(request, response);
        } catch (IOException | ServletException | RuntimeException e) {
            store.release(subject, key);
            throw e;
        }
        int status = response.getStatus();
        if (status >= 200 && status < 300) {
            store.complete(subject, key, status);
        } else {
            store.release(subject, key);
        }
    }

    private void fail(HttpServletResponse response, HttpStatus status, String message) throws IOException {
        ResponseErrorDTO error = new ResponseErrorDTO();
        error.setCode(status.name());
        error.setMessage(message);
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        json.writeValue(response.getOutputStream(), error);
    }

    private static void pause() {
        try {
            Thread.sleep(25);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while waiting for an idempotent request", e);
        }
    }

    private static String sha256(byte[] body) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(body));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
