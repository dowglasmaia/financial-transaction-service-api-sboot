package com.dowglasmaia.financialtransaction.shared.idempotency;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;
import java.util.Optional;

/**
 * Table {@code idempotency_key} (Flyway V4). Every method is one short auto-committed statement, so a claim is visible to
 * other requests at once and never sits inside the money transaction.
 */
@Component
public class IdempotencyStore {

    /** What a key currently stands for. {@code status} is null while the first attempt is still running. */
    public record Entry(String requestHash, Integer status, Duration age) {
        public boolean running() {
            return status == null;
        }
    }

    static final Duration TTL = Duration.ofHours(24);

    private final JdbcTemplate jdbc;

    public IdempotencyStore(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** True when this call created the row (it owns the attempt); false when the key already exists. */
    public boolean claim(String subject, String key, String requestHash) {
        return jdbc.update("insert into idempotency_key (subject, idem_key, request_hash) values (?, ?, ?) on conflict do nothing",
              subject, key, requestHash) == 1;
    }

    public Optional<Entry> find(String subject, String key) {
        List<Entry> found = jdbc.query(
              "select request_hash, status, extract(epoch from (now() - created_at)) as age from idempotency_key where subject = ? and idem_key = ?",
              (rs, i) -> new Entry(rs.getString("request_hash"), rs.getObject("status", Integer.class),
                    Duration.ofMillis((long) (rs.getDouble("age") * 1000))),
              subject, key);
        return found.stream().findFirst();
    }

    public void complete(String subject, String key, int status) {
        jdbc.update("update idempotency_key set status = ? where subject = ? and idem_key = ?", status, subject, key);
    }

    /** The attempt did not move money (a business failure or an error): the key is free again. */
    public void release(String subject, String key) {
        jdbc.update("delete from idempotency_key where subject = ? and idem_key = ?", subject, key);
    }

    /** Drops a row that expired or whose owner is gone, but only if it is still the row the caller looked at. */
    public boolean evict(String subject, String key, String requestHash) {
        return jdbc.update("delete from idempotency_key where subject = ? and idem_key = ? and request_hash = ?", subject, key, requestHash) == 1;
    }

    @Scheduled(fixedDelayString = "PT1H", initialDelayString = "PT1M")
    void purgeExpired() {
        jdbc.update("delete from idempotency_key where created_at < now() - make_interval(secs => ?)", TTL.toSeconds());
    }
}
