package com.dowglasmaia.financialtransaction.support;

import com.dowglasmaia.financialtransaction.entity.Account;
import com.dowglasmaia.financialtransaction.entity.Agency;
import com.dowglasmaia.financialtransaction.entity.Transaction;
import com.dowglasmaia.financialtransaction.entity.TransactionTypeEnum;
import com.dowglasmaia.financialtransaction.entity.User;
import com.dowglasmaia.financialtransaction.repository.AccountRepository;
import com.dowglasmaia.financialtransaction.repository.AgencyRepository;
import com.dowglasmaia.financialtransaction.repository.OutboxEventRepository;
import com.dowglasmaia.financialtransaction.repository.TransactionRepository;
import com.dowglasmaia.financialtransaction.repository.UserRepository;
import com.dowglasmaia.financialtransaction.services.client.maiaBank.MaiaBankClient;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Base for characterization tests (spec BE-01): real PostgreSQL (Testcontainers), embedded Kafka,
 * MaiaBank client mocked. The context is shared by all subclasses; each test starts from the seeded
 * state (account "2022", agency "0211", balance 5000.00) created by the "test" profile.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@EmbeddedKafka(partitions = 1, topics = "recharge-mobile-topic")
@TestPropertySource(properties = "spring.kafka.bootstrap-servers=${spring.embedded.kafka.brokers}")
public abstract class AbstractIntegrationTest {

    protected static final String SEED_ACCOUNT = "2022";
    protected static final String SEED_AGENCY = "0211";
    protected static final BigDecimal SEED_BALANCE = new BigDecimal("5000.00");
    protected static final String INSUFFICIENT = "Operation not performed, there is not enough balance in the account.";

    @DynamicPropertySource
    static void database(DynamicPropertyRegistry registry) {
        SharedPostgres.register(registry);
        MockIssuer.register(registry);
    }

    @Autowired protected MockMvc mvc;
    @Autowired protected ObjectMapper json;
    @Autowired protected AccountRepository accountRepository;
    @Autowired protected AgencyRepository agencyRepository;
    @Autowired protected UserRepository userRepository;
    @Autowired protected TransactionRepository transactionRepository;
    @Autowired protected OutboxEventRepository outboxEventRepository;

    @MockBean protected MaiaBankClient maiaBankClient;

    private final List<Account> extraAccounts = new ArrayList<>();
    private final List<User> extraUsers = new ArrayList<>();

    @BeforeEach
    void resetToSeed() {
        transactionRepository.deleteAll();
        outboxEventRepository.deleteAll();
        Account seed = seedAccount();
        seed.setBalance(SEED_BALANCE);
        accountRepository.save(seed);
    }

    @AfterEach
    void removeExtras() {
        transactionRepository.deleteAll(); // the ledger references the accounts (foreign key)
        accountRepository.deleteAll(extraAccounts);
        userRepository.deleteAll(extraUsers);
        extraAccounts.clear();
        extraUsers.clear();
    }

    protected Account seedAccount() {
        return accountRepository.findByNumber(SEED_ACCOUNT).orElseThrow();
    }

    protected Agency seedAgency() {
        return agencyRepository.findByNumber(SEED_AGENCY).orElseThrow();
    }

    /** A second account in the same agency, owned by another user whose token subject is NOT linked. */
    protected Account createOtherAccount(String number, BigDecimal balance) {
        return createOtherAccount(number, balance, null);
    }

    /** A second account in the same agency, owned by the user linked to {@code subject} (see {@link MockIssuer#OTHER_SUB}). */
    protected Account createOtherAccount(String number, BigDecimal balance, String subject) {
        User user = userRepository.save(User.builder()
              .name("Other Test User").CPF(number + "000000000".substring(0, 11 - number.length()))
              .email("other-" + number + "@example.test").areaCode(11).phoneNumber(988887777)
              .externalSubject(subject).build());
        extraUsers.add(user);
        Account account = accountRepository.save(Account.builder()
              .agency(seedAgency()).user(user).number(number).balance(balance).build());
        extraAccounts.add(account);
        return account;
    }

    protected BigDecimal balanceOf(String accountNumber) {
        return accountRepository.findByNumber(accountNumber).orElseThrow().getBalance();
    }

    protected Transaction ledger(TransactionTypeEnum type, BigDecimal amount, String accountId, OffsetDateTime when) {
        return transactionRepository.save(Transaction.builder()
              .transactionType(type).amount(amount).accountId(accountId).agencyId(null).dateHour(when).build());
    }

    /** Requests are made as the seeded owner (a valid token from the mock issuer) unless a test says otherwise. */
    protected ResultActions post(String path, String body) throws Exception {
        return postAs(MockIssuer.ownerToken(), path, body);
    }

    protected ResultActions get(String pathAndQuery) throws Exception {
        return getAs(MockIssuer.ownerToken(), pathAndQuery);
    }

    protected ResultActions postAs(String token, String path, String body) throws Exception {
        var request = MockMvcRequestBuilders.post("/api/v1/transactions" + path)
              .contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON).content(body);
        if (token != null) request.header("Authorization", "Bearer " + token);
        return mvc.perform(request);
    }

    protected ResultActions getAs(String token, String pathAndQuery) throws Exception {
        var request = MockMvcRequestBuilders.get("/api/v1/transactions" + pathAndQuery).accept(MediaType.APPLICATION_JSON);
        if (token != null) request.header("Authorization", "Bearer " + token);
        return mvc.perform(request);
    }

    protected JsonNode bodyOf(ResultActions result) throws Exception {
        return json.readTree(result.andReturn().getResponse().getContentAsString());
    }

    protected static boolean sameValue(BigDecimal expected, BigDecimal actual) {
        return expected.compareTo(actual) == 0;
    }
}
