package com.dowglasmaia.financialtransaction.integrity;

import com.dowglasmaia.financialtransaction.entity.Transaction;
import com.dowglasmaia.financialtransaction.transaction.domain.TransactionType;
import com.dowglasmaia.financialtransaction.support.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Spec BE-03: money operations must be atomic and race-safe. Many requests hit the same account at the same
 * time; whatever the interleaving, the balance never goes below zero and the ledger explains the balance exactly
 * (sum of ledger rows = change in balance, one row per successful operation).
 */
class MoneyConcurrencyTest extends AbstractIntegrationTest {

    private static final int THREADS = 24;

    @Test
    void parallelWithdrawalsNeverOverdrawAndTheLedgerMatchesTheBalance() throws Exception {
        List<Integer> statuses = inParallel(THREADS, () -> status(post("/atm-withdrawal", "{\"amount\":300.00}")));

        long accepted = statuses.stream().filter(s -> s == 204).count();
        long rejected = statuses.stream().filter(s -> s == 422).count();
        assertThat(accepted + rejected).as("only 204 or 422 are expected, got %s", statuses).isEqualTo(THREADS);

        BigDecimal balance = balanceOf(SEED_ACCOUNT);
        List<Transaction> ledger = transactionRepository.findAll();

        assertThat(balance.signum()).as("balance must never be negative").isGreaterThanOrEqualTo(0);
        assertThat(ledger).as("one ledger row per accepted withdrawal").hasSize((int) accepted);
        assertThat(sum(ledger)).as("sum of the ledger = change in balance").isEqualByComparingTo(SEED_BALANCE.subtract(balance));
        // 5000.00 / 300.00 = 16 withdrawals fit; the remaining 200.00 can never cover another one
        assertThat(accepted).isEqualTo(16);
    }

    @Test
    void parallelDepositsAndWithdrawalsKeepTheLedgerAndTheBalanceConsistent() throws Exception {
        List<Callable<Integer>> work = new ArrayList<>();
        for (int i = 0; i < 12; i++) {
            work.add(() -> status(post("/deposit", "{\"agency\":\"0211\",\"account\":\"2022\",\"amount\":100.00}")));
            work.add(() -> status(post("/atm-withdrawal", "{\"amount\":700.00}")));
        }
        List<Integer> statuses = inParallel(work);

        assertThat(statuses).allMatch(s -> s == 204 || s == 422);
        BigDecimal balance = balanceOf(SEED_ACCOUNT);
        List<Transaction> ledger = transactionRepository.findAll();

        BigDecimal deposits = sum(ledger.stream().filter(t -> t.getTransactionType() == TransactionType.DEPOSIT).toList());
        BigDecimal withdrawals = sum(ledger.stream().filter(t -> t.getTransactionType() == TransactionType.WITHDRAW).toList());

        assertThat(balance.signum()).isGreaterThanOrEqualTo(0);
        assertThat(ledger).hasSize((int) statuses.stream().filter(s -> s == 204).count());
        assertThat(balance).as("balance = seed + deposits - withdrawals").isEqualByComparingTo(SEED_BALANCE.add(deposits).subtract(withdrawals));
    }

    // ---------------------------------------------------------------- helpers

    private static BigDecimal sum(List<Transaction> rows) {
        return rows.stream().map(Transaction::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private static int status(org.springframework.test.web.servlet.ResultActions result) throws Exception {
        return result.andReturn().getResponse().getStatus();
    }

    private static List<Integer> inParallel(int count, Callable<Integer> task) throws Exception {
        List<Callable<Integer>> tasks = new ArrayList<>();
        for (int i = 0; i < count; i++) tasks.add(task);
        return inParallel(tasks);
    }

    /** Releases all tasks at the same instant to maximize contention. Unexpected failures are reported as 500. */
    private static List<Integer> inParallel(List<Callable<Integer>> tasks) throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(tasks.size());
        CountDownLatch ready = new CountDownLatch(tasks.size());
        CountDownLatch go = new CountDownLatch(1);
        List<Future<Integer>> futures = new ArrayList<>();
        for (Callable<Integer> task : tasks) {
            futures.add(pool.submit(() -> {
                ready.countDown();
                go.await();
                try {
                    return task.call();
                } catch (Throwable t) {
                    return 500;
                }
            }));
        }
        ready.await();
        go.countDown();
        List<Integer> results = new ArrayList<>();
        for (Future<Integer> future : futures) results.add(future.get(60, TimeUnit.SECONDS));
        pool.shutdown();
        return results;
    }
}
