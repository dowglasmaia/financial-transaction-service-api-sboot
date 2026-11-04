package com.dowglasmaia.financialtransaction.support;

import com.dowglasmaia.financialtransaction.account.infrastructure.persistence.AccountEntity;
import com.dowglasmaia.financialtransaction.transaction.domain.TransactionType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Spec BE-02: the schema comes from Flyway, Hibernate only validates, and the constraints exist in the database. */
class FlywaySchemaTest extends AbstractIntegrationTest {

    @Autowired JdbcTemplate jdbc;
    @Autowired Environment env;

    @Test
    void flywayAppliedTheBaselineMigration() {
        List<String> versions = jdbc.queryForList(
              "select version from flyway_schema_history where success = true order by installed_rank", String.class);
        assertThat(versions).contains("1");
    }

    @Test
    void hibernateOnlyValidatesTheSchema() {
        assertThat(env.getProperty("spring.jpa.hibernate.ddl-auto")).isEqualTo("validate");
    }

    @Test
    void theDatabaseRejectsANegativeBalance() {
        assertThatThrownBy(() -> jdbc.update("update account set balance = -0.01 where number = ?", SEED_ACCOUNT))
              .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void anAccountNumberIsUniquePerAgency() {
        AccountEntity duplicate = AccountEntity.builder().agency(seedAgency()).user(seedAccount().getUser())
              .number(SEED_ACCOUNT).balance(BigDecimal.ZERO).build();
        assertThatThrownBy(() -> accountRepository.saveAndFlush(duplicate))
              .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void ledgerRowsMustReferenceAnExistingAgencyWhenOneIsGiven() {
        assertThatThrownBy(() -> jdbc.update(
              "insert into transaction (id, date_hour, amount, account_id, agency_id, transaction_type) values (?,?,?,?,?,?)",
              "fk-test", OffsetDateTime.now(), new BigDecimal("1.00"), seedAccount().getId(), "no-such-agency",
              TransactionType.DEPOSIT.name())).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void ledgerMoneyTypeAndDateAreMandatory() {
        assertThatThrownBy(() -> jdbc.update(
              "insert into transaction (id, date_hour, amount, account_id, agency_id, transaction_type) values (?,?,?,?,?,?)",
              "null-amount", OffsetDateTime.now(), null, seedAccount().getId(), null, "DEPOSIT"))
              .isInstanceOf(DataIntegrityViolationException.class);
        assertThatThrownBy(() -> jdbc.update(
              "insert into transaction (id, date_hour, amount, account_id, agency_id, transaction_type) values (?,?,?,?,?,?)",
              "bad-type", OffsetDateTime.now(), new BigDecimal("1.00"), seedAccount().getId(), null, "TRANSFER"))
              .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void theStatementIndexExists() {
        Integer count = jdbc.queryForObject(
              "select count(*) from pg_indexes where tablename = 'transaction' and indexname = 'idx_transaction_account_date'",
              Integer.class);
        assertThat(count).isEqualTo(1);
    }

    @Test
    void migrationV2WasApplied() {
        List<String> versions = jdbc.queryForList(
              "select version from flyway_schema_history where success = true order by installed_rank", String.class);
        assertThat(versions).contains("1", "2");
    }

    @Test
    void ledgerRowsMustReferenceAnExistingAccount() { // V2 adds the foreign key (recharge stored the account number before BE-03)
        assertThatThrownBy(() -> jdbc.update(
              "insert into transaction (id, date_hour, amount, account_id, agency_id, transaction_type) values (?,?,?,?,?,?)",
              "fk-account", OffsetDateTime.now(), new BigDecimal("1.00"), "2022", null, "PIX"))
              .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void ledgerStatusIsRestrictedAndDefaultsToCompleted() {
        jdbc.update("insert into transaction (id, date_hour, amount, account_id, transaction_type) values (?,?,?,?,?)",
              "status-default", OffsetDateTime.now(), new BigDecimal("1.00"), seedAccount().getId(), "PIX");
        assertThat(jdbc.queryForObject("select status from transaction where id = 'status-default'", String.class)).isEqualTo("COMPLETED");
        assertThatThrownBy(() -> jdbc.update(
              "insert into transaction (id, date_hour, amount, account_id, transaction_type, status) values (?,?,?,?,?,?)",
              "bad-status", OffsetDateTime.now(), new BigDecimal("1.00"), seedAccount().getId(), "PIX", "DONE"))
              .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void anIdentityProviderSubjectCanLinkToOnlyOneUser() { // V3: usuario.external_subject is unique
        assertThat(jdbc.queryForObject("select count(*) from information_schema.columns where table_name = 'usuario' and column_name = 'external_subject'", Integer.class)).isEqualTo(1);
        jdbc.update("insert into usuario (id, name, \"cpf\", email, external_subject) values ('u-sub-1','A','00000000001','a1@example.test','auth0|dup')");
        try {
            assertThatThrownBy(() -> jdbc.update("insert into usuario (id, name, \"cpf\", email, external_subject) values ('u-sub-2','B','00000000002','b1@example.test','auth0|dup')"))
                  .isInstanceOf(DataIntegrityViolationException.class);
        } finally {
            jdbc.update("delete from usuario where id in ('u-sub-1','u-sub-2')");
        }
    }

    @Test
    void theOutboxTableExists() {
        assertThat(jdbc.queryForObject("select count(*) from information_schema.tables where table_name = 'outbox_event'", Integer.class)).isEqualTo(1);
    }
}
