package com.dowglasmaia.financialtransaction.transaction.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.dowglasmaia.financialtransaction.account.infrastructure.persistence.AccountEntity;
import com.dowglasmaia.financialtransaction.support.AbstractIntegrationTest;
import com.dowglasmaia.financialtransaction.transaction.domain.TransactionType;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.EntityManagerFactory;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.UUID;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/** SL-02: the statement and the summary end to end (real PostgreSQL, real security, the Sao Paulo day). */
class StatementEvolutionTest extends AbstractIntegrationTest {

    @Autowired EntityManagerFactory entityManagerFactory;

    private static OffsetDateTime utc(String instant) {
        return OffsetDateTime.parse(instant);
    }

    private String seedId() {
        return seedAccount().getId();
    }

    // ----------------------------------------------------------------------------------- the period
    @Test
    void aSingleDayReturnsThatDayInTheSaoPauloZone() throws Exception { // F-07 fixed
        ledger(TransactionType.DEPOSIT, new BigDecimal("1.00"), seedId(), utc("2026-03-11T12:00:00Z"));

        JsonNode body = bodyOf(get("?from=2026-03-11&to=2026-03-11").andExpect(status().isOk()));

        assertThat(body.get("transaction")).hasSize(1);
        assertThat(body.get("sumIncoming").decimalValue()).isEqualByComparingTo("1");
    }

    @Test
    void theDayBoundariesAreInclusiveAtTheStartAndExclusiveAtTheEnd() throws Exception {
        ledger(TransactionType.DEPOSIT, new BigDecimal("1.00"), seedId(), utc("2026-03-11T03:00:00Z"));      // 00:00 in Sao Paulo: in
        ledger(TransactionType.DEPOSIT, new BigDecimal("2.00"), seedId(), utc("2026-03-12T02:59:59Z"));      // 23:59:59: in
        ledger(TransactionType.DEPOSIT, new BigDecimal("4.00"), seedId(), utc("2026-03-11T02:59:59Z"));      // the 10th: out
        ledger(TransactionType.DEPOSIT, new BigDecimal("8.00"), seedId(), utc("2026-03-12T03:00:00Z"));      // the 12th: out

        JsonNode body = bodyOf(get("?from=2026-03-11&to=2026-03-11").andExpect(status().isOk()));

        assertThat(body.get("transaction")).hasSize(2);
        assertThat(body.get("sumIncoming").decimalValue()).isEqualByComparingTo("3");
    }

    @Test
    void theDeprecatedParametersMeanTheSamePeriod() throws Exception {
        ledger(TransactionType.DEPOSIT, new BigDecimal("1.00"), seedId(), utc("2026-03-11T12:00:00Z"));

        JsonNode body = bodyOf(get("?initial-date=11/03/2026&final-date=11/03/2026").andExpect(status().isOk()));

        assertThat(body.get("transaction")).hasSize(1);
    }

    @Test
    void missingDatesAreRejectedWith422AndTheKnownMessage() throws Exception { // F-07: was an NPE
        get("").andExpect(status().isUnprocessableEntity())
              .andExpect(jsonPath("$.code").value("UNPROCESSABLE_ENTITY"))
              .andExpect(jsonPath("$.message").value("Dates are required: send from and to."));
        get("?from=2026-03-11").andExpect(status().isUnprocessableEntity())
              .andExpect(jsonPath("$.message").value("Dates are required: send from and to."));
    }

    @Test
    void badOrReversedDatesAre422NeverA500() throws Exception {
        get("?from=2026-13-45&to=2026-03-11").andExpect(status().isUnprocessableEntity())
              .andExpect(jsonPath("$.message").value("Invalid value for 'from': expected yyyy-MM-dd."));
        get("?initial-date=31/02/2026&final-date=01/03/2026").andExpect(status().isUnprocessableEntity())
              .andExpect(jsonPath("$.message").value("Invalid date: expected dd/MM/yyyy."));
        get("?from=2026-03-12&to=2026-03-10").andExpect(status().isUnprocessableEntity())
              .andExpect(jsonPath("$.message").value("The start date must not be after the end date."));
    }

    // ----------------------------------------------------------------------------------- rows, paging, sorting, filter
    @Test
    void everyRowCarriesItsId() throws Exception {
        var saved = ledger(TransactionType.PIX, new BigDecimal("12.50"), seedId(), utc("2026-03-11T12:00:00Z"));

        JsonNode first = bodyOf(get("?from=2026-03-11&to=2026-03-11").andExpect(status().isOk())).get("transaction").get(0);

        assertThat(first.get("id").asText()).isEqualTo(saved.getId());
        get("/" + first.get("id").asText()).andExpect(status().isOk()).andExpect(jsonPath("$.id").value(saved.getId()));
    }

    @Test
    void pagingSortingAndTheTypeFilterWork() throws Exception {
        ledger(TransactionType.DEPOSIT, new BigDecimal("10.00"), seedId(), utc("2026-03-10T12:00:00Z"));
        ledger(TransactionType.PIX, new BigDecimal("30.00"), seedId(), utc("2026-03-11T12:00:00Z"));
        ledger(TransactionType.PIX, new BigDecimal("20.00"), seedId(), utc("2026-03-12T12:00:00Z"));
        String period = "from=2026-03-10&to=2026-03-12";

        JsonNode newestFirst = bodyOf(get("?" + period).andExpect(status().isOk()));
        assertThat(newestFirst.get("transaction").get(0).get("amount").decimalValue()).isEqualByComparingTo("20"); // default date desc
        assertThat(newestFirst.get("page").asInt()).isZero();
        assertThat(newestFirst.get("size").asInt()).isEqualTo(20);
        assertThat(newestFirst.get("totalElements").asLong()).isEqualTo(3);

        JsonNode secondPage = bodyOf(get("?" + period + "&sort=amount&order=desc&page=1&size=2").andExpect(status().isOk()));
        assertThat(secondPage.get("transaction")).hasSize(1);
        assertThat(secondPage.get("transaction").get(0).get("amount").decimalValue()).isEqualByComparingTo("10");
        assertThat(secondPage.get("totalElements").asLong()).isEqualTo(3);

        JsonNode onlyPix = bodyOf(get("?" + period + "&type=PIX&sort=date&order=asc").andExpect(status().isOk()));
        assertThat(onlyPix.get("transaction")).hasSize(2);
        assertThat(onlyPix.get("transaction").get(0).get("amount").decimalValue()).isEqualByComparingTo("30"); // oldest first
        assertThat(onlyPix.get("totalElements").asLong()).isEqualTo(2);
    }

    @Test
    void totalsCoverTheWholePeriodWhateverThePageOrTheTypeFilter() throws Exception {
        ledger(TransactionType.DEPOSIT, new BigDecimal("100.00"), seedId(), utc("2026-03-10T12:00:00Z"));
        ledger(TransactionType.PIX, new BigDecimal("30.00"), seedId(), utc("2026-03-11T12:00:00Z"));
        ledger(TransactionType.WITHDRAW, new BigDecimal("20.00"), seedId(), utc("2026-03-11T13:00:00Z"));
        ledger(TransactionType.PHONE_RECHARGE, new BigDecimal("5.00"), seedId(), utc("2026-03-12T12:00:00Z"));

        for (String extra : new String[] {"", "&size=1", "&page=3&size=1", "&type=PIX", "&type=DEPOSIT&size=1"}) {
            JsonNode body = bodyOf(get("?from=2026-03-10&to=2026-03-12" + extra).andExpect(status().isOk()));
            assertThat(body.get("sumIncoming").decimalValue()).as("sumIncoming" + extra).isEqualByComparingTo("100");
            assertThat(body.get("sumOutgoing").decimalValue()).as("sumOutgoing" + extra).isEqualByComparingTo("55");
            assertThat(body.get("balance").decimalValue()).as("balance" + extra).isEqualByComparingTo("5000");
        }
    }

    @Test
    void invalidPagingSortingOrTypeAre422WithTheKnownMessage() throws Exception {
        String p = "?from=2026-03-10&to=2026-03-12";
        get(p + "&page=-1").andExpect(status().isUnprocessableEntity()).andExpect(jsonPath("$.message").value("page must be 0 or greater"));
        get(p + "&size=0").andExpect(status().isUnprocessableEntity()).andExpect(jsonPath("$.message").value("size must be between 1 and 100"));
        get(p + "&size=101").andExpect(status().isUnprocessableEntity()).andExpect(jsonPath("$.message").value("size must be between 1 and 100"));
        get(p + "&sort=name").andExpect(status().isUnprocessableEntity()).andExpect(jsonPath("$.message").value("sort must be one of: date, amount"));
        get(p + "&order=up").andExpect(status().isUnprocessableEntity()).andExpect(jsonPath("$.message").value("order must be asc or desc"));
        get(p + "&type=LOAN").andExpect(status().isUnprocessableEntity())
              .andExpect(jsonPath("$.message").value("type must be one of: DEPOSIT, PIX, PHONE_RECHARGE, WITHDRAW"));
        get(p + "&page=abc").andExpect(status().isUnprocessableEntity()).andExpect(jsonPath("$.message").value("Invalid value for 'page'."));
    }

    @Test
    void theStatementHitsTheLedgerWithTwoQueriesNoMatterHowManyRows() throws Exception { // no N+1
        for (int i = 0; i < 15; i++) {
            ledger(i % 2 == 0 ? TransactionType.DEPOSIT : TransactionType.PIX, new BigDecimal("1.00"), seedId(), utc("2026-03-11T12:00:00Z").plusMinutes(i));
        }
        Statistics statistics = entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
        statistics.setStatisticsEnabled(true);
        statistics.clear();

        get("?from=2026-03-11&to=2026-03-11&size=15").andExpect(status().isOk());

        long ledgerQueries = Arrays.stream(statistics.getQueries()).filter(hql -> hql.contains("TransactionEntity"))
              .mapToLong(hql -> statistics.getQueryStatistics(hql).getExecutionCount()).sum();
        assertThat(ledgerQueries).isEqualTo(2); // the page of rows + one aggregate (sums and count)
    }

    @Test
    void datesAreIsoStringsAsTheContractSaysNotNumbersOrArrays() throws Exception {
        ledger(TransactionType.DEPOSIT, new BigDecimal("1.00"), seedId(), utc("2026-03-11T12:00:00Z"));

        JsonNode row = bodyOf(get("?from=2026-03-11&to=2026-03-11").andExpect(status().isOk())).get("transaction").get(0);
        JsonNode day = bodyOf(get("/summary?from=2026-03-11&to=2026-03-11").andExpect(status().isOk())).get("series").get(0);

        assertThat(row.get("dateHour").isTextual()).isTrue();
        assertThat(OffsetDateTime.parse(row.get("dateHour").asText())).isEqualTo(utc("2026-03-11T12:00:00Z"));
        assertThat(day.get("date").asText()).isEqualTo("2026-03-11");
    }

    // ----------------------------------------------------------------------------------- detail
    @Test
    void aForeignOrUnknownIdIsNotFoundAndANonUuidIdIsABadRequest() throws Exception {
        AccountEntity other = createOtherAccount("9999", new BigDecimal("10.00"));
        var foreign = ledger(TransactionType.DEPOSIT, new BigDecimal("9.00"), other.getId(), utc("2026-03-11T12:00:00Z"));

        get("/" + foreign.getId()).andExpect(status().isNotFound()).andExpect(jsonPath("$.message").value("Transaction not found"));
        get("/" + UUID.randomUUID()).andExpect(status().isNotFound());
        get("/not-a-uuid").andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("BAD_REQUEST"));
    }

    // ----------------------------------------------------------------------------------- summary
    @Test
    void theSummaryHasTheTotalsOfThePeriodAndOneBucketPerSaoPauloDay() throws Exception {
        ledger(TransactionType.DEPOSIT, new BigDecimal("100.00"), seedId(), utc("2026-03-11T02:30:00Z")); // the 10th (23:30)
        ledger(TransactionType.PIX, new BigDecimal("35.00"), seedId(), utc("2026-03-12T12:00:00Z"));      // the 12th

        JsonNode summary = bodyOf(get("/summary?from=2026-03-10&to=2026-03-12").andExpect(status().isOk()));
        JsonNode statement = bodyOf(get("?from=2026-03-10&to=2026-03-12").andExpect(status().isOk()));

        assertThat(summary.get("series")).hasSize(3); // the 11th has no activity and is still there
        assertThat(summary.get("series").get(0).get("date").asText()).isEqualTo("2026-03-10");
        assertThat(summary.get("series").get(0).get("incoming").decimalValue()).isEqualByComparingTo("100");
        assertThat(summary.get("series").get(1).get("incoming").decimalValue()).isEqualByComparingTo("0");
        assertThat(summary.get("series").get(2).get("outgoing").decimalValue()).isEqualByComparingTo("35");
        // Home equals Statement for the same period
        assertThat(summary.get("sumIncoming").decimalValue()).isEqualByComparingTo(statement.get("sumIncoming").decimalValue());
        assertThat(summary.get("sumOutgoing").decimalValue()).isEqualByComparingTo(statement.get("sumOutgoing").decimalValue());
        assertThat(summary.get("balance").decimalValue()).isEqualByComparingTo(statement.get("balance").decimalValue());
    }

    @Test
    void theSummaryNeedsBothDatesAndAValidRange() throws Exception {
        get("/summary").andExpect(status().isUnprocessableEntity()).andExpect(jsonPath("$.message").value("Dates are required: send from and to."));
        get("/summary?from=2026-03-12&to=2026-03-10").andExpect(status().isUnprocessableEntity())
              .andExpect(jsonPath("$.message").value("The start date must not be after the end date."));
    }

    @Test
    void theSummaryNeverShowsAnotherCustomersRows() throws Exception {
        AccountEntity other = createOtherAccount("9999", new BigDecimal("10.00"));
        ledger(TransactionType.DEPOSIT, new BigDecimal("77.00"), other.getId(), utc("2026-03-11T12:00:00Z"));

        JsonNode summary = bodyOf(get("/summary?from=2026-03-11&to=2026-03-11").andExpect(status().isOk()));

        assertThat(summary.get("sumIncoming").decimalValue()).isEqualByComparingTo("0");
    }
}
